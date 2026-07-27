package com.lex3d.ultimatezootaming.events.dynamic;

/**
 * Evenements dynamiques du zoo : casse la routine, chaque journee peut avoir
 * son propre event. Chacun a un effet REEL sur le gameplay du jour.
 */
public enum DynamicEvent {
    /** Jour normal : aucun evenement. */
    NONE           ("none",           0),
    /** Journaliste : vient tester le zoo. Bonus/malus d'affluence de 3 jours. */
    JOURNALIST     ("journalist",    15),
    /** Inspection sanitaire : amende si trop de malades, sinon prime de bon eleve. */
    HEALTH_CHECK   ("health_check",  10),
    /** Groupe scolaire : 6-10 enfants d'un coup, tous gratuits. */
    SCHOOL_TRIP    ("school_trip",   12),
    /** VIP celebre : reclame une espece rare, prime enorme si content. */
    CELEBRITY      ("celebrity",      6),
    /** Panne d'electricite : bornes desactivees quelques minutes. */
    POWER_OUTAGE   ("power_outage",   8),
    /** Manifestation : quelques manifestants devant l'entree, -30% de visiteurs. */
    PROTEST        ("protest",        4);

    public final String key;
    /** Poids de tirage (proba relative). */
    public final int weight;

    DynamicEvent(String key, int weight) {
        this.key = key;
        this.weight = weight;
    }

    /** Tire un evenement aleatoire, pondere. NONE reste le plus probable. */
    public static DynamicEvent roll(java.util.Random rng) {
        int total = 0;
        for (DynamicEvent e : values()) total += e.weight;
        // 40% de chance d'avoir NONE pour eviter que chaque jour soit un event
        if (rng.nextInt(100) < 40) return NONE;
        int pick = rng.nextInt(total);
        int acc = 0;
        for (DynamicEvent e : values()) {
            acc += e.weight;
            if (pick < acc) return e;
        }
        return NONE;
    }
}
