package com.lex3d.ultimatezootaming.entities;

/**
 * Personnalites des visiteurs : chaque visiteur a un type qui influence son
 * comportement et ses reactions. Tire au spawn.
 */
public enum VisitorPersonality {
    /** Visiteur standard : comportement classique. */
    STANDARD    ("standard",    50),
    /** Photographe : reste 3x plus longtemps devant les vedettes, prend beaucoup
     *  de photos aux bornes. */
    PHOTOGRAPHER("photographer",15),
    /** Fan d'oiseaux : trouve les enclos aviaires 2x plus interessants,
     *  rale beaucoup si pas d'oiseau au zoo. */
    BIRD_FAN    ("bird_fan",    10),
    /** Famille : arrive en groupe (deja gere via groupLeader), budget bas
     *  (achete moins de souvenirs mais reste longtemps). */
    FAMILY      ("family",      12),
    /** Enfant seul : plus petit, tient un ballon obligatoirement, plus fatigable. */
    LONE_CHILD  ("lone_child",   8),
    /** VIP celebre : rare, +50% affluence si content, laisse une prime enorme. */
    CELEBRITY   ("celebrity",    5);

    public final String key;
    public final int weight;

    VisitorPersonality(String key, int weight) {
        this.key = key;
        this.weight = weight;
    }

    /** Tire une personnalite ponderee. Accepte le RandomSource des entites. */
    public static VisitorPersonality roll(net.minecraft.util.RandomSource rng) {
        int total = 0;
        for (VisitorPersonality p : values()) total += p.weight;
        int pick = rng.nextInt(total);
        int acc = 0;
        for (VisitorPersonality p : values()) {
            acc += p.weight;
            if (pick < acc) return p;
        }
        return STANDARD;
    }
}
