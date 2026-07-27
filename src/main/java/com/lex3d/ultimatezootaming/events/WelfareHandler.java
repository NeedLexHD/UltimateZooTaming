package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.welfare.WelfareCalculator;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Applique le bien-etre (Phase C) : toutes les 30s, recalcule la satisfaction de
 * chaque familier assigne a un enclos, et applique des effets DOUX :
 *   - Heureux (>75) : particules coeur, petit boost de vitesse.
 *   - Malheureux (<25) : particules grises, lenteur ; longtemps malheureux -> maladie.
 *   - Malade : particules maladives, satisfaction plafonnee (soigne par Remede/Soigneur).
 * Aucun effet punitif fort (pas de mort, pas de fuite).
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class WelfareHandler {

    // intervalles lus depuis la config serveur (ZooServerConfig)
    
    private static final int EFFECT_INTERVAL = 100;   // effets visuels toutes les 5s

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide()) return;
        if (!(mob.level() instanceof ServerLevel level)) return;

        // Les animaux de compagnie vanilla sont hors du systeme du zoo
        if (com.lex3d.ultimatezootaming.capability.PetSpecies.isPet(mob)) return;
        mob.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            // PROTEGE : tout animal apprivoise, ET tout animal assigne a un enclos.
            // Le second cas couvre un pensionnaire dont le lien de propriete aurait
            // ete perdu (transfert de monde, mod tiers, ancien save) : tant qu'il
            // appartient a un enclos, il fait partie du zoo et ne doit pas despawn.
            boolean inZoo = data.isTamed() || data.getZoneId() != null;
            if (!inZoo) return;
            // Securite : un animal du zoo ne doit JAMAIS despawn (bug des loutres qui disparaissent)
            if (mob instanceof net.minecraft.world.entity.Mob m && !m.isPersistenceRequired()) {
                m.setPersistenceRequired();
            }
            // Anti-disparition renforcee : empeche la mort par environnement (noyade,
            // suffocation, chute, cactus...) pour les animaux du zoo. Les mods tiers
            // (loutres Critters and Companions) peuvent noyer un animal hors de son
            // element ; on garde toujours au moins 1 PV et on coupe l'air/le feu.
            if (mob.getAirSupply() < mob.getMaxAirSupply()) {
                mob.setAirSupply(mob.getMaxAirSupply());
            }
            if (mob.isOnFire()) mob.clearFire();
            if (mob.getHealth() < 2.0f && mob.getHealth() > 0.0f) {
                mob.setHealth(Math.max(mob.getHealth(), 2.0f));
            }
            if (data.getZoneId() == null) return;

            int RECALC_INTERVAL = com.lex3d.ultimatezootaming.config.ZooServerConfig.WELFARE_RECALC_INTERVAL.get();
            int MISERY_TO_SICK = com.lex3d.ultimatezootaming.config.ZooServerConfig.MISERY_TO_SICK_TICKS.get();
            long time = level.getGameTime() + mob.getId(); // decale par mob (pas de pic de charge)

            // --- Recalcul periodique ---
            if (time % RECALC_INTERVAL == 0) {
                ZooZone zone = ZooSavedData.get(level).getZone(data.getZoneId());
                if (zone == null) {
                    data.setZoneId(null); // enclos supprime -> familier libere
                    return;
                }
                // Evasion : l'animal est sorti de son enclos. On attend qu'il soit
                // charge depuis >=100 ticks (evite le faux positif au rechargement de
                // chunk, ou la position est transitoire), et on teste sa colonne reelle
                // avec une bonne marge horizontale.
                if (mob.tickCount >= 100) {
                    net.minecraft.core.BlockPos mp = mob.blockPosition();
                    boolean inside = zone.contains(mp)
                            || zone.containsNear(mp)
                            || zone.boundingBox().inflate(3, 4, 3).contains(mob.position());
                    if (!inside && !data.isEscaped()) {
                        data.setEscaped(true);
                        com.lex3d.ultimatezootaming.events.EscapeHandler.onEscape(level, mob, zone);
                    } else if (inside && data.isEscaped()) {
                        data.setEscaped(false);
                        com.lex3d.ultimatezootaming.events.EscapeHandler.onRecapture(level, mob);
                    }
                }
                WelfareCalculator.Breakdown bd = WelfareCalculator.computeBreakdown(level, mob, zone);
                int raw = bd.total();
                data.setWelfareBreakdown(bd.space(), bd.habitat(), bd.food(), bd.company(), bd.health());
                if (data.isSick()) raw = Math.min(raw, 40); // plafond tant que malade
                data.setSatisfaction(raw);

                // Malheur prolonge -> tombe malade (HARDY : bien plus resistant)
                if (raw < 25) {
                    com.lex3d.ultimatezootaming.capability.TamingData.Trait trait = data.getTrait();
                    int miseryGain = trait == com.lex3d.ultimatezootaming.capability.TamingData.Trait.HARDY
                            ? RECALC_INTERVAL / 3 : RECALC_INTERVAL;
                    data.setMiseryTimer(data.getMiseryTimer() + miseryGain);
                    if (com.lex3d.ultimatezootaming.config.ZooServerConfig.ENABLE_SICKNESS.get() && data.getMiseryTimer() >= MISERY_TO_SICK && !data.isSick()) {
                        data.setSick(true);
                        // 1 fois sur 4 : maladie GRAVE, hors de portee du veterinaire
                        if (level.random.nextInt(4) == 0) {
                            data.setSevereSick(true);
                            for (net.minecraft.server.level.ServerPlayer p :
                                    level.getServer().getPlayerList().getPlayers()) {
                                p.sendSystemMessage(net.minecraft.network.chat.Component
                                        .literal("\u2695 ").withStyle(net.minecraft.ChatFormatting.DARK_RED)
                                        .append(net.minecraft.network.chat.Component.translatable(
                                                "message.ultimatezootaming.severe_sick",
                                                mob.getName())
                                                .withStyle(net.minecraft.ChatFormatting.RED)));
                            }
                        }
                    }
                } else {
                    data.setMiseryTimer(Math.max(0, data.getMiseryTimer() - RECALC_INTERVAL));
                }
            }

            // --- Effets doux (desactivables via config) ---
            if (com.lex3d.ultimatezootaming.config.ZooServerConfig.ENABLE_WELFARE_EFFECTS.get()) {
                applyEffects(level, mob, data, time);
            }
        });
    }

    private static void applyEffects(ServerLevel level, LivingEntity mob, TamingData data, long time) {
        double x = mob.getX(), y = mob.getY() + mob.getBbHeight() + 0.2, z = mob.getZ();

        // Effets de deplacement selon l'humeur (inchange)
        switch (data.getMood()) {
            case HAPPY -> mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
            case MISERABLE -> mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, true, false));
            default -> { }
        }
        if (data.getTrait() == com.lex3d.ultimatezootaming.capability.TamingData.Trait.ENERGETIC) {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, true, false));
        }

        // PARTICULE D'ETAT au-dessus de la tete (toutes les ~3s), pour reperer les
        // problemes d'un coup d'oeil sans ouvrir le panneau. Priorite au plus grave.
        if (time % 60 != 0) return;
        if (data.isSevereSick()) {
            // maladie GRAVE : nuage de sorcier violet (bien visible = urgence)
            level.sendParticles(ParticleTypes.WITCH, x, y, z, 4, 0.25, 0.2, 0.25, 0.02);
        } else if (data.isSick()) {
            // maladie : bulles/effet vert maladif
            level.sendParticles(ParticleTypes.ENTITY_EFFECT, x, y, z, 3, 0.2, 0.15, 0.2, 0.0);
            level.sendParticles(ParticleTypes.SNEEZE, x, y, z, 2, 0.15, 0.1, 0.15, 0.01);
        } else if (needsFood(level, mob, data)) {
            // a faim : particules de nourriture (l'animal reclame a manger)
            level.sendParticles(ParticleTypes.ITEM_SLIME, x, y, z, 2, 0.15, 0.1, 0.15, 0.0);
        } else if (data.getSatisfaction() < 25) {
            // malheureux : petit nuage gris
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 2, 0.2, 0.1, 0.2, 0.0);
        } else if (data.getSatisfaction() >= 75) {
            // content : coeur
            level.sendParticles(ParticleTypes.HEART, x, y, z, 1, 0.2, 0.1, 0.2, 0.0);
        }
    }

    /** L'animal manque-t-il de nourriture ? (composante Nourriture du bien-etre a 0
     *  = pas de mangeoire remplie du bon regime accessible). */
    private static boolean needsFood(ServerLevel level, LivingEntity mob, TamingData data) {
        int[] bd = data.getWelfareBreakdown();
        return bd.length >= 3 && bd[2] == 0;
    }
}
