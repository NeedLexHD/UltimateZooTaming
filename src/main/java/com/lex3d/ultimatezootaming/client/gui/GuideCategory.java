package com.lex3d.ultimatezootaming.client.gui;

/**
 * Catégories du manuel du zoo. Chaque catégorie contient un nombre de pages
 * défini ; les textes sont dans les fichiers de langue sous la forme
 * guide.<cle>.<numero>.title / .body
 */
public enum GuideCategory {

    /** Premiers pas : apprivoiser, premier enclos, ouvrir le parc. */
    START("start", 3),

    /** Les animaux : bien-etre, habitats, nourriture, sante, fiches. */
    ANIMALS("animals", 5),

    /** Les enclos : outils de delimitation, types de zones, amenagement. */
    ZONES("zones", 4),

    /** Le personnel : metiers, affectation, competences, rythme de travail. */
    STAFF("staff", 6),

    /** Les visiteurs : parcours, besoins, avis, personnalites, groupes. */
    VISITORS("visitors", 5),

    /** L'argent : billets, tresorerie, boutiques, prix, recyclage, echange. */
    MONEY("money", 4),

    /** Progression : rangs, missions, marketing, evenements. */
    PROGRESS("progress", 4),

    /** Elevage : reproduction, genetique, incubateur, lignees. */
    BREEDING("breeding", 4),

    /** Proprete et logistique : dechets, billetterie, carte, chemins. */
    UPKEEP("upkeep", 3),

    /** Reputation : contrats internationaux et flux social. */
    RENOWN("renown", 3);

    public final String key;
    public final int pageCount;

    GuideCategory(String key, int pageCount) {
        this.key = key;
        this.pageCount = pageCount;
    }

    /** Clef de traduction du nom de la categorie (affiche au sommaire). */
    public String titleKey() { return "guide.ultimatezootaming.cat." + key; }

    /** Clef de la ligne de resume affichee sous le nom au sommaire. */
    public String summaryKey() { return "guide.ultimatezootaming.cat." + key + ".sum"; }

    public String pageTitleKey(int page) {
        return "guide.ultimatezootaming." + key + "." + (page + 1) + ".title";
    }

    public String pageBodyKey(int page) {
        return "guide.ultimatezootaming." + key + "." + (page + 1) + ".body";
    }
}
