package com.lex3d.ultimatezootaming.client;

/**
 * Cache client du bien-etre du familier vise, + suivi de la cible ACTUELLE.
 * L'overlay HUD (WelfareOverlay) n'affiche la barre que si :
 *   - on vise un familier EN CE MOMENT (currentTarget mis a jour chaque tick client)
 *   - ET on a des donnees fraiches pour lui.
 * Des qu'on detourne le regard, currentTarget repasse a -1 et la barre disparait
 * instantanement (plus de remanence facon actionbar).
 */
public class ClientWelfareCache {

    // Donnees recues du serveur pour la derniere entite interrogee
    private static int entityId = -1;
    private static int satisfaction;
    private static boolean sick;
    private static boolean inZone;
    private static String trait = "NONE";
    private static long updatedAt;

    // Entite visee a l'instant present (mise a jour par le tick client)
    private static int currentTarget = -1;
    private static String currentName = "";

    public static void put(int id, int sat, boolean isSick, boolean isInZone, String tr) {
        entityId = id;
        satisfaction = sat;
        sick = isSick;
        inZone = isInZone;
        trait = tr;
        updatedAt = System.currentTimeMillis();
    }

    /** Appele chaque tick : quelle entite vise-t-on (ou -1 si aucune) + son nom. */
    public static void setCurrentTarget(int id, String name) {
        currentTarget = id;
        currentName = name;
    }

    public static void clearCurrentTarget() {
        currentTarget = -1;
    }

    /** L'overlay doit-il s'afficher ? (on vise un familier ET on a ses donnees fraiches) */
    public static boolean shouldRender() {
        return currentTarget != -1
                && currentTarget == entityId
                && System.currentTimeMillis() - updatedAt < 1500;
    }

    public static boolean has(int id) {
        return id == entityId && System.currentTimeMillis() - updatedAt < 2000;
    }

    public static int satisfaction() { return satisfaction; }
    public static boolean sick() { return sick; }
    public static boolean inZone() { return inZone; }
    public static String trait() { return trait; }
    public static String currentName() { return currentName; }
}
