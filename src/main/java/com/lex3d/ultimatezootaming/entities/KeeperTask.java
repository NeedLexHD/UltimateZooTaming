package com.lex3d.ultimatezootaming.entities;

/**
 * CE QUE FAIT UN EMPLOYE EN CE MOMENT.
 *
 * Sert uniquement a l'information : l'activite est affichee au-dessus de sa tete
 * et dans le registre du personnel. Elle n'influence aucune decision de l'IA,
 * qui reste pilotee par les Goals.
 *
 * Chaque Goal declare sa tache au demarrage et la relache a l'arret.
 */
public enum KeeperTask {

    IDLE     ("idle",     "\u00B7"),   // rien de particulier
    PATROL   ("patrol",   "\u21BB"),   // ronde d'inspection
    HEALING  ("healing",  "\u2695"),   // soigne un animal
    FEEDING  ("feeding",  "\u2617"),   // remplit une mangeoire
    FETCHING ("fetching", "\u2192"),   // va chercher du fourrage
    SELLING  ("selling",  "\u01B5"),   // tient la caisse
    RESTOCK  ("restock",  "\u2913"),   // regarnit la boutique
    CLEANING ("cleaning", "\u2724"),   // ramasse un detritus
    GUARDING ("guarding", "\u26E8"),   // contient une evasion
    COMMUTE  ("commute",  "\u279C"),   // rejoint son poste
    BREAK    ("break",    "\u2615"),   // pause dejeuner
    SLEEPING ("sleeping", "\u263D"),   // dort
    STRIKE   ("strike",   "\u26A0");   // en greve

    public final String key;
    /** Petit symbole affiche au-dessus de la tete. */
    public final String icon;

    KeeperTask(String key, String icon) {
        this.key = key;
        this.icon = icon;
    }

    public String translationKey() {
        return "task.ultimatezootaming." + key;
    }

    public static KeeperTask byOrdinal(int i) {
        var v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
