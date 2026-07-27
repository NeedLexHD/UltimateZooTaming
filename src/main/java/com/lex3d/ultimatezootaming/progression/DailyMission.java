package com.lex3d.ultimatezootaming.progression;

/**
 * Missions journalieres : 3 defis tires chaque jour, chacun avec une prime.
 * Elles rythment les journees et poussent le joueur a diversifier son gameplay.
 */
public enum DailyMission {
    /** Accueillir X visiteurs aujourd'hui. */
    WELCOME_VISITORS ("welcome_visitors",  15, 30),
    /** Gagner X billets aujourd'hui. */
    EARN_TICKETS     ("earn_tickets",      40, 40),
    /** Soigner X animaux malades. */
    HEAL_ANIMALS     ("heal_animals",       3, 45),
    /** Vendre X articles en boutique. */
    SELL_ITEMS       ("sell_items",         8, 35),
    /** Utiliser les bornes X fois (photo/nourrissage/eau). */
    STATION_USES     ("station_uses",       6, 30),
    /** Prendre X photos souvenirs (borne photo). */
    TAKE_PHOTOS      ("take_photos",        4, 35),
    /** Nourrir X animaux (borne feed). */
    FEED_ANIMALS     ("feed_animals",       5, 30),
    /** Arroser X animaux (borne eau). */
    WATER_ANIMALS    ("water_animals",      5, 30),
    /** Ambiance moyenne des enclos >= 5. */
    AMBIANCE_5       ("ambiance_5",         5, 40),
    /** Note du zoo >= 60 ce jour. */
    SCORE_60         ("score_60",          60, 50),
    /** Recruter 1 nouvel employe. */
    HIRE_STAFF       ("hire_staff",         1, 40),
    /** Faire un cablin (tablet) a 3 animaux differents. */
    CUDDLE_3         ("cuddle_3",           3, 35);

    public final String key;
    public final int target;
    public final int reward;

    DailyMission(String key, int target, int reward) {
        this.key = key;
        this.target = target;
        this.reward = reward;
    }
}
