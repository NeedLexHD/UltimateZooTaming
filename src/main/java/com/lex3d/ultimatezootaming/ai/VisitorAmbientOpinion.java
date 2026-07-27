package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

/**
 * Avis CONTEXTUELS : evalue l'environnement du visiteur et declenche les
 * remarques qui n'etaient jamais dites (trop cher, bonde, sale, joli, perdu...).
 * Appele periodiquement depuis le tick du visiteur.
 */
public final class VisitorAmbientOpinion {

    private VisitorAmbientOpinion() {}

    /** Evalue le contexte et dit AU PLUS un avis. Appeler ~toutes les 30s. */
    public static void evaluate(ServerLevel level, VisitorEntity visitor) {
        var ledger = ZooLedger.get(level);
        java.util.List<String> candidates = new java.util.ArrayList<>();

        // --- PRIX : billet cher ou peu de valeur percue ---
        if (ledger.getTicketPolicy() == 2 && visitor.getRandom().nextInt(3) == 0) {
            candidates.add("too_expensive");
        }

        // --- AFFLUENCE : beaucoup de visiteurs autour de lui ---
        int neighbours = level.getEntitiesOfClass(VisitorEntity.class,
                new AABB(visitor.blockPosition()).inflate(6)).size();
        if (neighbours >= 7) candidates.add("crowded");

        // --- PROPRETE ---
        // On ne se plaint de la salete QUE s'il y a vraiment des detritus au sol.
        // Avant, l'absence de poubelle suffisait : un parc impeccable sans
        // corbeille recevait des reproches de salete, ce qui n'a aucun sens.
        int litter = countBlocksAround(level, visitor, 10,
                com.lex3d.ultimatezootaming.core.init.ModBlocks.LITTER.get());
        int bins = countBlocksAround(level, visitor, 12,
                com.lex3d.ultimatezootaming.core.init.ModBlocks.ZOO_BIN.get());
        if (litter >= 2) candidates.add("dirty");
        else if (litter == 0 && bins >= 2) candidates.add("clean");

        // --- BEAUTE : ambiance moyenne du parc ---
        double amb = com.lex3d.ultimatezootaming.welfare.AmbianceScore.zooAverage(level);
        if (amb >= 6.0) candidates.add("beautiful");

        // --- ENNUI : tres peu d'enclos dans le zoo ---
        int animalZones = 0;
        for (var z : ZooSavedData.get(level).getAllZones()) if (z.isAnimalZone()) animalZones++;
        if (animalZones <= 2) candidates.add("boring");

        // --- PERDU ---
        // On utilise la MEME definition d'allee que la navigation : dalles,
        // escaliers, Allee de zoo et blocs declares dans la config. Avant, seule
        // l'Allee de zoo comptait, donc un parc entierement dalle passait pour
        // depourvu de signalisation.
        if (!pathNearby(level, visitor, 8) && visitor.getRandom().nextInt(4) == 0) {
            candidates.add("lost");
        }

        // --- PERSONNEL : un employe non gréviste a proximite ---
        boolean staffNear = !level.getEntitiesOfClass(
                com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                new AABB(visitor.blockPosition()).inflate(5),
                k -> !k.isOnStrike()).isEmpty();
        if (staffNear && visitor.getRandom().nextInt(4) == 0) candidates.add("staff_nice");

        if (candidates.isEmpty()) return;
        String pick = candidates.get(visitor.getRandom().nextInt(candidates.size()));
        VisitorOpinion.say(level, visitor, pick);
    }

    /** Compte les blocs d'un type donne autour du visiteur (scan plat, peu couteux). */
    private static int countBlocksAround(ServerLevel level, VisitorEntity visitor, int radius,
                                         net.minecraft.world.level.block.Block block) {
        int found = 0;
        var base = visitor.blockPosition();
        // Scan en colonnes plates (dy = -1..+2) pour ne jamais scanner un volume
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (level.getBlockState(base.offset(dx, dy, dz)).is(block)) {
                        found++;
                        if (found >= 2) return found; // on n'a pas besoin de plus
                    }
                }
            }
        }
        return found;
    }

    /**
     * Une allee praticable a proximite ? On s'appuie sur la definition de la
     * navigation pour que l'avis colle a ce que l'IA considere reellement
     * comme un chemin.
     */
    private static boolean pathNearby(ServerLevel level, VisitorEntity visitor, int radius) {
        var base = visitor.blockPosition();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = -1; dy <= 1; dy++) {
                    var p = base.offset(dx, dy, dz);
                    if (com.lex3d.ultimatezootaming.ai.ZooPathNavigation
                            .isPath(level.getBlockState(p))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
