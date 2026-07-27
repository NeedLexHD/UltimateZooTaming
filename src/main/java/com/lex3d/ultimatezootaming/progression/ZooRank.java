package com.lex3d.ultimatezootaming.progression;

/**
 * Rangs du zoo : progression a long terme avec vrais paliers et bonus.
 * Le rang se debloque en cumulant : visiteurs a vie + especes distinctes + note.
 * Chaque rang debloque des BONUS PERMANENTS.
 */
public enum ZooRank {
    /** Zoo debutant : rien de special, tu commences ici. */
    DEBUTANT   ("debutant",    0,      0,   0, /*multBillet*/ 1.00, /*maxVisit*/ 12, /*prime*/  0),
    /** Zoo local : 50 visiteurs, 3 especes. Petits bonus. */
    LOCAL      ("local",      50,      3,  30, 1.05, 14,   50),
    /** Zoo regional : 200 visiteurs, 6 especes, note 40+. */
    REGIONAL   ("regional",  200,      6,  40, 1.10, 18,  150),
    /** Zoo national : 500 visiteurs, 10 especes, note 55+. */
    NATIONAL   ("national",  500,     10,  55, 1.20, 24,  300),
    /** Zoo prestigieux : 1000 visiteurs, 15 especes, note 70+. */
    PRESTIGIEUX("prestigieux",1000,   15,  70, 1.30, 32,  600),
    /** Zoo mondial : 2500 visiteurs, 20 especes, note 85+. */
    MONDIAL    ("mondial",   2500,    20,  85, 1.50, 40, 1200);

    public final String key;
    public final int visitorsRequired;
    public final int speciesRequired;
    public final int scoreRequired;
    /** Multiplicateur applique au prix du billet a ce rang. */
    public final double ticketMult;
    /** Plafond de visiteurs simultanes (le rang debloque plus d'affluence). */
    public final int maxVisitors;
    /** Prime en billets versee la 1re fois qu'on atteint ce rang. */
    public final int promotionBonus;

    ZooRank(String key, int visitors, int species, int score,
            double ticketMult, int maxVisitors, int promotionBonus) {
        this.key = key;
        this.visitorsRequired = visitors;
        this.speciesRequired = species;
        this.scoreRequired = score;
        this.ticketMult = ticketMult;
        this.maxVisitors = maxVisitors;
        this.promotionBonus = promotionBonus;
    }

    /** Le rang courant en fonction des stats du zoo (le plus haut atteint). */
    public static ZooRank compute(int totalVisitors, int distinctSpecies, int zooScore) {
        ZooRank best = DEBUTANT;
        for (ZooRank r : values()) {
            if (totalVisitors >= r.visitorsRequired
                    && distinctSpecies >= r.speciesRequired
                    && zooScore >= r.scoreRequired) {
                if (r.ordinal() > best.ordinal()) best = r;
            }
        }
        return best;
    }

    /** Le rang suivant (null si deja au max), pour afficher la progression. */
    public ZooRank next() {
        int i = ordinal();
        return i + 1 < values().length ? values()[i + 1] : null;
    }
}
