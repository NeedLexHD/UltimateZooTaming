package com.lex3d.ultimatezootaming.ambiance;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sons d'ambiance du parc : le brouhaha de foule + cris d'animaux occasionnels.
 * L'intensite depend du nombre de visiteurs presents. Aucun son ambiant si zoo ferme.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class AmbianceSoundHandler {

    /** Toutes les 10s (200 ticks), tirage aleatoire d'un son de foule si visiteurs presents. */
    private static final int CHECK = 200;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % CHECK != 0) return;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            playCrowdSounds(level);
            playAnimalCalls(level);
        }
    }

    /** Sons de foule : plus il y a de visiteurs, plus la chance et le volume montent. */
    private static void playCrowdSounds(ServerLevel level) {
        var visitors = level.getEntitiesOfClass(VisitorEntity.class,
                new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000));
        if (visitors.isEmpty()) return;
        // Chance : 0.1 pour 1 visiteur, 0.5 pour 5+, plafond 0.7
        double chance = Math.min(0.7, 0.1 + visitors.size() * 0.08);
        if (level.random.nextDouble() > chance) return;
        // Volume proportionnel a la foule
        float volume = Math.min(0.8f, 0.2f + visitors.size() * 0.05f);
        // Jouer pres d'un visiteur aleatoire (pas d'un seul point, ca fait "vrai")
        VisitorEntity v = visitors.get(level.random.nextInt(visitors.size()));
        SoundEvent[] crowdSounds = {
                SoundEvents.VILLAGER_AMBIENT, SoundEvents.PLAYER_LEVELUP,
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundEvents.VILLAGER_YES,
        };
        SoundEvent snd = crowdSounds[level.random.nextInt(crowdSounds.length)];
        level.playSound(null, v.getX(), v.getY(), v.getZ(), snd, SoundSource.NEUTRAL,
                volume * 0.5f, 0.8f + level.random.nextFloat() * 0.4f);
    }

    /** Cris d'animaux : les animaux du zoo emettent des sons occasionnels supplementaires. */
    private static void playAnimalCalls(ServerLevel level) {
        var animals = level.getEntitiesOfClass(Animal.class,
                new AABB(-30000000, -64, -30000000, 30000000, 320, 30000000));
        if (animals.isEmpty()) return;
        // Une chance sur 3 qu'un animal aleatoire "parle" en plus a chaque cycle
        if (level.random.nextInt(3) != 0) return;
        Animal a = animals.get(level.random.nextInt(animals.size()));
        // On utilise le son ambient natif de l'animal (playAmbientSound n'est pas expose,
        // on force via playSound avec un son generique adapte au type)
        var ambientSound = getAnimalAmbientSound(a);
        if (ambientSound != null) {
            level.playSound(null, a.getX(), a.getY(), a.getZ(), ambientSound,
                    SoundSource.NEUTRAL, 0.7f, 0.9f + level.random.nextFloat() * 0.2f);
        }
    }

    /** Devine un son "cri" pour un animal (vanille + mods communs). Retourne null si inconnu. */
    private static SoundEvent getAnimalAmbientSound(Animal a) {
        String type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType()) != null
                ? net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType()).getPath() : "";
        return switch (type) {
            case "cow" -> SoundEvents.COW_AMBIENT;
            case "pig" -> SoundEvents.PIG_AMBIENT;
            case "sheep" -> SoundEvents.SHEEP_AMBIENT;
            case "chicken" -> SoundEvents.CHICKEN_AMBIENT;
            case "wolf" -> SoundEvents.WOLF_AMBIENT;
            case "cat" -> SoundEvents.CAT_AMBIENT;
            case "fox" -> SoundEvents.FOX_AMBIENT;
            case "horse" -> SoundEvents.HORSE_AMBIENT;
            case "goat" -> SoundEvents.GOAT_AMBIENT;
            case "frog" -> SoundEvents.FROG_AMBIENT;
            case "panda" -> SoundEvents.PANDA_AMBIENT;
            case "polar_bear" -> SoundEvents.POLAR_BEAR_AMBIENT;
            case "parrot" -> SoundEvents.PARROT_AMBIENT;
            case "camel" -> SoundEvents.CAMEL_AMBIENT;
            default -> null;
        };
    }
}
