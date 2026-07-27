package com.lex3d.ultimatezootaming.marketing;

import com.lex3d.ultimatezootaming.progression.ZooRank;

/**
 * Campagnes publicitaires : achete un boost d'affluence pour N jours.
 * Investissement strategique : plus tu paies, plus le boost et la duree sont grands,
 * mais les grosses campagnes exigent un rang minimum.
 */
public enum AdCampaign {
    /** Pas de campagne active. */
    NONE     ("none",       0,  0, 0.0,  null),
    /** Pub locale : 50 Ƶ, +30% de visiteurs pendant 3 jours. */
    LOCAL    ("local",     50,  3, 0.30, ZooRank.DEBUTANT),
    /** Pub regionale : 150 Ƶ, +50% pendant 4 jours. Debloquee au rang Regional. */
    REGIONAL ("regional", 150,  4, 0.50, ZooRank.REGIONAL),
    /** Pub nationale : 350 Ƶ, +80% pendant 5 jours. Debloquee au rang National. */
    NATIONAL ("national", 350,  5, 0.80, ZooRank.NATIONAL),
    /** Pub mondiale : 800 Ƶ, +150% pendant 7 jours. Debloquee au rang Mondial. */
    WORLDWIDE("worldwide",800,  7, 1.50, ZooRank.MONDIAL);

    public final String key;
    public final int cost;
    public final int durationDays;
    public final double crowdBonus; // multiplicateur d'affluence
    public final ZooRank minRank;

    AdCampaign(String key, int cost, int durationDays, double crowdBonus, ZooRank minRank) {
        this.key = key;
        this.cost = cost;
        this.durationDays = durationDays;
        this.crowdBonus = crowdBonus;
        this.minRank = minRank;
    }

    /** True si la campagne est achetable au rang actuel du zoo. */
    public boolean isUnlocked(int highestRank) {
        return minRank == null || highestRank >= minRank.ordinal();
    }
}
