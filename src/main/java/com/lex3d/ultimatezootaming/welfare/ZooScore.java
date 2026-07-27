package com.lex3d.ultimatezootaming.welfare;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;

import java.util.HashSet;
import java.util.Set;

/** Note globale du zoo (0-100) : bien-etre moyen, especes, malades. */
public final class ZooScore {

    private ZooScore() {}

    /** Detail de la note : [moyenne bien-etre, bonus especes, malus malades, note, animaux, especes, malades]. */
    public record Detail(int welfareAvg, int speciesBonus, int sickMalus, int score,
                         int animals, int species, int sick) {}

    public static Detail detailed(ServerLevel level) {
        int totalAnimals = 0, weighted = 0, totalSick = 0;
        Set<EntityType<?>> species = new HashSet<>();
        for (ZooZone zone : ZooSavedData.get(level).getAllZones()) {
            if (!zone.isAnimalZone()) continue; // zones employes : pas d'animaux
            for (Animal a : level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                    an -> an.isAlive() && zone.contains(an.blockPosition())
                            && an.getCapability(CapabilityHandler.TAMING_DATA)
                                .resolve().map(TamingData::isTamed).orElse(false))) {
                species.add(a.getType());
                totalAnimals++;
                TamingData d = a.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
                if (d != null) {
                    weighted += d.getSatisfaction();
                    if (d.isSick()) totalSick++;
                }
            }
        }
        if (totalAnimals == 0) return new Detail(0, 0, 0, 0, 0, 0, 0);
        int avg = weighted / totalAnimals;
        int speciesBonus = Math.min(20, species.size());
        int sickMalus = Math.min(20, totalSick * 4);
        int score = Math.max(0, Math.min(100, (int) (avg * 0.8) + speciesBonus - sickMalus));
        return new Detail(avg, speciesBonus, sickMalus, score, totalAnimals, species.size(), totalSick);
    }

    public static int compute(ServerLevel level) {
        return detailed(level).score();
    }

    /** Nombre d'especes VEDETTES (config starSpecies) presentes dans le zoo. */
    public static int starCount(ServerLevel level) {
        java.util.List<? extends String> stars =
                com.lex3d.ultimatezootaming.config.ZooServerConfig.STAR_SPECIES.get();
        if (stars == null || stars.isEmpty()) return 0;
        Set<String> present = new HashSet<>();
        for (ZooZone zone : ZooSavedData.get(level).getAllZones()) {
            if (!zone.isAnimalZone()) continue;
            for (Animal a : level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                    an -> an.isAlive() && zone.contains(an.blockPosition()))) {
                var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
                if (id != null) present.add(id.toString());
            }
        }
        int n = 0;
        for (String s : stars) if (present.contains(s)) n++;
        return n;
    }

    /** Prix du billet : plus le zoo est repute, plus les gens paient. */
    public static int ticketPrice(int score) {
        // Prix d'entree en billets : plus modere (un zoo moyen ~2, un zoo parfait ~3).
        // Les gros revenus doivent venir des BOUTIQUES, pas de l'entree passive.
        return Math.max(1, score / 30);
    }
}
