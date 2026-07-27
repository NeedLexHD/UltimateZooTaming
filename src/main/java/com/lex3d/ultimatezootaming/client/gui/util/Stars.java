package com.lex3d.ultimatezootaming.client.gui.util;

/**
 * Rendu des notes en ETOILES avec quarts (precision 0.25).
 * Utilise partout : note du zoo, bien-etre animal, avis visiteurs.
 */
public final class Stars {

    private Stars() {}

    /** Etoile pleine, trois-quarts, demie, quart, vide. */
    private static final char FULL = '\u2605';   // ★
    private static final char EMPTY = '\u2606';  // ☆
    private static final char HALF = '\u25D0';   // ◐ (demi rempli)
    private static final char QUARTER = '\u25CB';// ○ (quart -> cercle vide)
    private static final char THREEQ = '\u25D1'; // ◑ (trois quarts)

    /**
     * Convertit une note 0-100 en chaine de 5 etoiles, arrondie au quart.
     * Ex : 82/100 -> 4.1 -> arrondi 4.0 -> "★★★★☆"
     *      90/100 -> 4.5 -> "★★★★◐"
     */
    public static String fromPercent(int percent) {
        double raw = Math.max(0, Math.min(100, percent)) / 20.0; // 0-5
        return fromRating(raw);
    }

    /** Convertit une note deja sur 5 (ex 3.7) en etoiles au quart pres. */
    public static String fromRating(double rating) {
        double r = Math.max(0, Math.min(5, rating));
        // Arrondi au quart le plus proche
        double q = Math.round(r * 4.0) / 4.0;
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            double remaining = q - i;
            if (remaining >= 1.0) sb.append(FULL);
            else if (remaining >= 0.75) sb.append(THREEQ);
            else if (remaining >= 0.5) sb.append(HALF);
            else if (remaining >= 0.25) sb.append(QUARTER);
            else sb.append(EMPTY);
        }
        return sb.toString();
    }

    /** Note sur 5 formatee : "4.25" (pour afficher a cote des etoiles). */
    public static String ratingText(double rating) {
        double q = Math.round(Math.max(0, Math.min(5, rating)) * 4.0) / 4.0;
        return String.format(java.util.Locale.ROOT, "%.2f", q);
    }

    /** Note sur 5 depuis un pourcentage 0-100. */
    public static double percentToRating(int percent) {
        return Math.max(0, Math.min(100, percent)) / 20.0;
    }

    // --- Emojis de categories de bien-etre (colonnes du GUI enclos) ---
    /** Espace : fleche d'expansion. */
    public static final String ICON_SPACE = "\u2921";     // ⤡
    /** Habitat : arbre / feuille. */
    public static final String ICON_HABITAT = "\u2618";   // ☘
    /** Nourriture : couverts. */
    public static final String ICON_FOOD = "\u2615";      // ☕ (approx bol)
    /** Compagnie : deux personnes. */
    public static final String ICON_COMPANY = "\u265B";   // ♛ (approx groupe)
    /** Sante : croix medicale. */
    public static final String ICON_HEALTH = "\u2695";    // ⚕
}
