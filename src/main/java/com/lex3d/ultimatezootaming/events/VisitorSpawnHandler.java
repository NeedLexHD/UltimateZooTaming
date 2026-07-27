package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.ZooEntranceBlock;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity;
import com.lex3d.ultimatezootaming.config.ZooServerConfig;
import com.lex3d.ultimatezootaming.core.init.ModEntities;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.welfare.ZooScore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fait venir les visiteurs : plus la note du zoo est haute, plus ils arrivent
 * souvent et paient cher. Plafond configurable (anti-lag).
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class VisitorSpawnHandler {

    private static final int CHECK = 200; // 10s

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!ZooServerConfig.VISITORS_ENABLED.get()) return;
        if (event.getServer().getTickCount() % CHECK != 0) return;

        int configMax = ZooServerConfig.MAX_VISITORS.get();
        for (ServerLevel level : event.getServer().getAllLevels()) {
            // Le plafond de visiteurs augmente avec le RANG du zoo
            var ledger0 = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level);
            var rank0 = com.lex3d.ultimatezootaming.progression.ZooRank.values()[
                    Math.max(0, Math.min(com.lex3d.ultimatezootaming.progression.ZooRank.values().length - 1,
                            ledger0.getHighestRank()))];
            int max = Math.min(configMax, rank0.maxVisitors);
            var entrances = ZooEntranceBlock.entrancesIn(level);
            if (entrances.isEmpty()) continue;

            int current = level.getEntitiesOfClass(VisitorEntity.class,
                    new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000)).size();
            if (current >= max) continue;

            // Le zoo n'accueille que de jour
            if (!com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).isOpen()) continue;
            int score = ZooScore.compute(level);
            if (score < 10) continue; // zoo trop pauvre : personne ne vient

            // Chance de spawn proportionnelle a la note et a la place restante
            // Une evasion en cours ? Personne n'entre.
            if (com.lex3d.ultimatezootaming.events.EscapeHandler.anyEscapeActive(level)) continue;
            var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level);
            double chance = (score / 100.0) * (1.0 - (double) current / max) * ledger.crowdFactor();
            // METEO : pluie/orage decouragent les visiteurs
            if (level.isThundering()) chance *= 0.2;      // orage : -80% de visiteurs
            else if (level.isRaining()) chance *= 0.6;    // pluie : -40% de visiteurs
            // Especes vedettes : +20% d'affluence chacune (max x2)
            chance *= Math.min(2.0, 1.0 + 0.2 * ZooScore.starCount(level));
            // Deco : l'ambiance moyenne booste jusqu'a +40%
            chance *= 1.0 + com.lex3d.ultimatezootaming.welfare.AmbianceScore.zooAverage(level) / 25.0;
            // Naissance recente : le zoo fait le buzz (+50%)
            if (ledger.isHyped()) chance *= 1.5;
            // COURBE D'AFFLUENCE : le parc se remplit et se vide au fil de la journee
            chance *= hourlyFactor(level);
            if (level.random.nextDouble() > chance) continue;

            var territory = com.lex3d.ultimatezootaming.saveddata.ZooTerritory.get(level);
            for (BlockPos entrance : entrances) {
                if (!level.isLoaded(entrance)) continue;
                // Le zoo a un territoire ? L'entree doit en faire partie.
                if (!territory.isEmpty() && !territory.isClaimed(entrance.getX(), entrance.getZ())) continue;
                // BILLETTERIE PHYSIQUE : si des guichets sont poses, spawn a cote du
                // guichet le plus proche de l'entree. Sinon, spawn direct sur l'entree.
                BlockPos spawnPos;
                var booths = com.lex3d.ultimatezootaming.blocks.TicketBoothBlock.getBoothsIn(level);
                if (!booths.isEmpty()) {
                    BlockPos nearestBooth = null;
                    double bd = Double.MAX_VALUE;
                    for (BlockPos b : booths) {
                        double d = b.distSqr(entrance);
                        if (d < bd) { bd = d; nearestBooth = b; }
                    }
                    if (nearestBooth != null) {
                        int dx = level.random.nextInt(3) - 1;
                        int dz = level.random.nextInt(3) - 1;
                        spawnPos = nearestBooth.offset(dx, 1, dz);
                        if (!level.getBlockState(spawnPos).isAir()) spawnPos = nearestBooth.above();
                    } else {
                        spawnPos = entrance.above();
                    }
                } else {
                    spawnPos = entrance.above();
                }
                if (!level.getBlockState(spawnPos).isAir()) continue;

                VisitorEntity visitor = ModEntities.VISITOR.get().create(level);
                if (visitor == null) continue;
                visitor.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                        level.random.nextFloat() * 360f, 0);
                visitor.setEntrance(entrance);
                if (level.getBlockEntity(entrance) instanceof
                        com.lex3d.ultimatezootaming.blocks.ZooEntranceBlockEntity ebe) {
                    ebe.triggerAnim("main", "turn"); // le tourniquet tourne !
                }
                // ~1 sur 8 est un VIP avec un souhait d'espece (vedette du config)
                if (level.random.nextInt(8) == 0) {
                    var stars = com.lex3d.ultimatezootaming.config.ZooServerConfig.STAR_SPECIES.get();
                    if (stars != null && !stars.isEmpty()) {
                        String wish = stars.get(level.random.nextInt(stars.size()));
                        visitor.makeVip(wish);
                        for (var p : level.players()) {
                            var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                                    .getValue(net.minecraft.resources.ResourceLocation.tryParse(wish));
                            p.displayClientMessage(net.minecraft.network.chat.Component.literal("\u2605 ")
                                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                                    .append(net.minecraft.network.chat.Component.translatable(
                                            "message.ultimatezootaming.vip_arrived",
                                            type != null ? net.minecraft.network.chat.Component.translatable(
                                                    type.getDescriptionId())
                                                    : net.minecraft.network.chat.Component.literal(wish))
                                            .withStyle(net.minecraft.ChatFormatting.GOLD)), false);
                        }
                    }
                }
                visitor.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                        MobSpawnType.EVENT, null, null);
                // Si aucun guichet dans le monde, le visiteur a deja son ticket
                // (retro-compat : sans guichet, le zoo fonctionne comme avant).
                if (com.lex3d.ultimatezootaming.blocks.TicketBoothBlock.getBoothsIn(level).isEmpty()) {
                    visitor.setTicket(true);
                }
                level.addFreshEntity(visitor);

                // GROUPES/FAMILLES : ~1 visiteur sur 3 vient accompagne de 1-2 autres
                // (ils arrivent ensemble a l'entree et demarrent leur visite groupes).
                if (level.random.nextInt(3) == 0) {
                    int companions = 1 + level.random.nextInt(2); // 1 ou 2
                    for (int cN = 0; cN < companions; cN++) {
                        VisitorEntity mate = ModEntities.VISITOR.get().create(level);
                        if (mate == null) continue;
                        mate.moveTo(spawnPos.getX() + 0.5 + (level.random.nextDouble() - 0.5),
                                spawnPos.getY(),
                                spawnPos.getZ() + 0.5 + (level.random.nextDouble() - 0.5),
                                level.random.nextFloat() * 360f, 0);
                        mate.setEntrance(entrance);
                        // ~40% des accompagnants sont des ENFANTS (rendu plus petit)
                        if (level.random.nextInt(10) < 4) mate.setChild(true);
                        // Rattache l'accompagnant au CHEF de groupe (le 1er visiteur)
                        mate.setGroupLeader(visitor.getUUID());
                        mate.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
                                MobSpawnType.EVENT, null, null);
                        // Compagnons : suivent le chef qui, lui, aura le ticket via le
                        // guichet. Le compagnon a aussi son ticket auto pour ne pas
                        // bloquer le groupe (le chef pait pour la famille).
                        mate.setTicket(true);
                        level.addFreshEntity(mate);
                    }
                }

                // Le billet d'entree va dans la Caisse du Zoo
                ZooVaultBlockEntity vault = ZooVaultBlock.nearestVault(level, entrance, 128);
                if (vault == null) vault = ZooVaultBlock.scanForVault(level, entrance, 24);
                if (vault != null) {
                    // Multiplicateur de RANG du zoo : plus le rang est haut, plus le
                    // billet se vend cher (ton zoo mondial vaut le double d'un zoo local).
                    int species = (int) Math.min(Integer.MAX_VALUE, com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).getTotalVisitors());
                    var rank = com.lex3d.ultimatezootaming.progression.ZooRank.values()[
                            Math.max(0, Math.min(com.lex3d.ultimatezootaming.progression.ZooRank.values().length - 1,
                                    ledger.getHighestRank()))];
                    int price = Math.max(1, (int) Math.round(
                            ZooScore.ticketPrice(score) * ledger.priceFactor() * rank.ticketMult));
                    vault.deposit(price);
                    ledger.addTickets(price);
                    // Missions journalieres
                    ledger.addMissionProgress(
                            com.lex3d.ultimatezootaming.progression.DailyMission.WELCOME_VISITORS, 1);
                    ledger.addMissionProgress(
                            com.lex3d.ultimatezootaming.progression.DailyMission.EARN_TICKETS, price);
                }
                // Billet cher : le visiteur arrive un peu grognon
                if (ledger.getTicketPolicy() == 2) visitor.spendJoy(15);
                break; // un seul visiteur par cycle
            }
        }
    }

    /**
     * Courbe d'affluence sur la journee, comme dans un vrai parc :
     * calme a l'ouverture, montee en fin de matinee, PIC en milieu de journee,
     * puis decrue jusqu'a la fermeture.
     *
     * Le jour Minecraft va de 0 (aube) a 12000 (crepuscule).
     *   0-2000   : ouverture, les premiers arrivent           x0.35
     *   2000-4000: la matinee se remplit                       x0.70
     *   4000-8000: PIC de frequentation (midi)                 x1.35
     *   8000-10000: debut d'apres-midi, ca se calme            x0.85
     *   10000+   : fin de journee, plus grand monde            x0.40
     */
    private static double hourlyFactor(ServerLevel level) {
        long t = level.getDayTime() % 24000L;
        if (t >= 12000L) return 0.0;            // nuit : le zoo est ferme de toute facon
        if (t < 2000L)  return 0.35;
        if (t < 4000L)  return 0.70;
        if (t < 8000L)  return 1.35;
        if (t < 10000L) return 0.85;
        return 0.40;
    }
}
