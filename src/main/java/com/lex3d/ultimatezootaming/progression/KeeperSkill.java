package com.lex3d.ultimatezootaming.progression;

/**
 * COMPETENCES DES EMPLOYES.
 *
 * Chaque employe gagne 1 point de competence tous les 2 niveaux (donc 10 points
 * au niveau 20). Chaque competence monte jusqu'au rang 3.
 *
 * Les competences sont universelles, mais la derniere (MAITRISE) produit un
 * effet different selon le metier : c'est elle qui fait la vraie difference
 * entre un veterinaire chevronne et un debutant.
 */
public enum KeeperSkill {

    /** Celerite : se deplace plus vite. +8% de vitesse par rang. */
    SPEED("speed", 3),

    /** Portee : rayon d'action elargi (soins, ravitaillement, nettoyage). +2 blocs par rang. */
    REACH("reach", 3),

    /** Efficacite : agit plus souvent. -20% sur les temps d'attente par rang. */
    EFFICIENCY("efficiency", 3),

    /** Devouement : tolere l'impaye. 1 jour de salaire manque par rang avant la greve. */
    DEDICATION("dedication", 3),

    /** Endurance : moins de pauses, journee de travail plus longue. */
    STAMINA("stamina", 3),

    /** Maitrise : bonus specifique au metier (voir masteryDescriptionKey). */
    MASTERY("mastery", 3);

    public final String key;
    public final int maxRank;

    KeeperSkill(String key, int maxRank) {
        this.key = key;
        this.maxRank = maxRank;
    }

    /** Un point de competence tous les 2 niveaux. */
    public static int pointsForLevel(int level) {
        return Math.max(0, level / 2);
    }

    /**
     * Clef de description de la MAITRISE selon le metier, pour que le GUI
     * explique ce que la competence apporte concretement a cet employe.
     */
    public static String masteryDescriptionKey(int job) {
        return switch (job) {
            case 1 -> "skill.ultimatezootaming.mastery.vet";       // soins plus efficaces
            case 2 -> "skill.ultimatezootaming.mastery.feeder";    // porte plus de fourrage
            case 3 -> "skill.ultimatezootaming.mastery.guard";     // contient plus vite
            case 4 -> "skill.ultimatezootaming.mastery.vendor";    // meilleures recettes
            case 5 -> "skill.ultimatezootaming.mastery.janitor";   // double recyclable
            default -> "skill.ultimatezootaming.mastery.generalist";
        };
    }
}
