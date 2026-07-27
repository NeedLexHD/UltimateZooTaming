package com.lex3d.ultimatezootaming.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Config TOML serveur : ultimatezootame-server.toml
 * Regroupe toutes les valeurs d'equilibrage du gameplay, modifiables sans
 * recompiler le mod. Chargee cote serveur (s'applique au monde/serveur).
 */
public class ZooServerConfig {

    public static final ForgeConfigSpec SPEC;

    // --- Bien-etre ---
    public static final ForgeConfigSpec.IntValue WELFARE_RECALC_INTERVAL;
    public static final ForgeConfigSpec.IntValue MISERY_TO_SICK_TICKS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SICKNESS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_WELFARE_EFFECTS;

    // --- Mangeoire / reproduction ---
    public static final ForgeConfigSpec.IntValue FEEDER_RADIUS;
    public static final ForgeConfigSpec.IntValue BABY_FEED_EVERY;
    public static final ForgeConfigSpec.IntValue BABY_GROWTH_PER_FEED;

    // --- Soigneur ---
    public static final ForgeConfigSpec.IntValue KEEPER_CARE_INTERVAL;
    public static final ForgeConfigSpec.IntValue KEEPER_CARE_AMOUNT;
    public static final ForgeConfigSpec.IntValue KEEPER_REFILL_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue KEEPER_NEEDS_REMEDY;
    public static final ForgeConfigSpec.IntValue KEEPER_TP_TIMEOUT;
    public static final ForgeConfigSpec.IntValue SALARY_PERIOD;
    public static final ForgeConfigSpec.IntValue SALARY_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue VISITORS_ENABLED;
    public static final ForgeConfigSpec.IntValue MAX_VISITORS;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> VISITOR_SKINS;
    public static final ForgeConfigSpec.IntValue PLUSH_PRICE;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> STAR_SPECIES;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> PATH_BLOCKS;
    /** Especes que le mod laisse entierement au comportement vanilla. */
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> PET_SPECIES;

    // --- Capture ---
    public static final ForgeConfigSpec.DoubleValue GLOBAL_CAPTURE_MULTIPLIER;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Ultimate Zoo Taming - Equilibrage du gameplay (serveur)").push("welfare");

        WELFARE_RECALC_INTERVAL = b
                .comment("Intervalle (en ticks) de recalcul du bien-etre. 20 ticks = 1 seconde. Defaut 600 (30s).")
                .defineInRange("welfareRecalcInterval", 600, 20, 12000);
        MISERY_TO_SICK_TICKS = b
                .comment("Duree (ticks) de malheur continu avant qu'un animal tombe malade. Defaut 6000 (~5min).")
                .defineInRange("miseryToSickTicks", 6000, 200, 72000);
        ENABLE_SICKNESS = b
                .comment("Active la maladie quand un animal est malheureux trop longtemps.")
                .define("enableSickness", true);
        ENABLE_WELFARE_EFFECTS = b
                .comment("Active les effets doux (vitesse si heureux, lenteur si malheureux, particules).")
                .define("enableWelfareEffects", true);

        b.pop().push("feeder");

        FEEDER_RADIUS = b
                .comment("Rayon d'action de la Mangeoire (en blocs) pour nourrir/reproduire. Defaut 8.")
                .defineInRange("feederRadius", 8, 2, 32);
        BABY_FEED_EVERY = b
                .comment("La Mangeoire ne nourrit un bebe qu'un cycle sur N (evite de la vider). Defaut 3.")
                .defineInRange("babyFeedEvery", 3, 1, 20);
        BABY_GROWTH_PER_FEED = b
                .comment("Croissance (ticks) donnee a un bebe par croquette. Defaut 1200 (~1min).")
                .defineInRange("babyGrowthPerFeed", 1200, 100, 24000);

        b.pop().push("keeper");

        KEEPER_CARE_INTERVAL = b
                .comment("Intervalle (ticks) entre deux soins du Soigneur. Defaut 40 (2s).")
                .defineInRange("keeperCareInterval", 40, 10, 600);
        KEEPER_CARE_AMOUNT = b
                .comment("Points de satisfaction rendus par soin du Soigneur. Defaut 8.")
                .defineInRange("keeperCareAmount", 8, 1, 100);
        KEEPER_REFILL_COOLDOWN = b
                .comment("Pause (ticks) apres qu'un Soigneur remplit une mangeoire. Defaut 100 (5s).")
                .defineInRange("keeperRefillCooldown", 100, 0, 2400);
        KEEPER_NEEDS_REMEDY = b
                .comment("Le Soigneur doit consommer un Remede animal (dans un coffre de l'enclos) pour guerir une maladie.")
                .define("keeperNeedsRemedy", true);
        KEEPER_TP_TIMEOUT = b
                .comment("Si un trajet du Soigneur depasse ce temps (ticks), il se teleporte a sa cible. 0 = jamais. Defaut 600 (30s).")
                .defineInRange("keeperTpTimeout", 600, 0, 12000);
        SALARY_PERIOD = b
                .comment("Periode de paie des employes en ticks (24000 = 1 jour MC). 0 = salaires desactives.")
                .defineInRange("salaryPeriod", 24000, 0, 240000);
        SALARY_AMOUNT = b
                .comment("Billets payes a chaque employe par periode (preleves dans la Tresorerie du Zoo).")
                .defineInRange("salaryAmount", 4, 0, 64);
        VISITORS_ENABLED = b
                .comment("Les visiteurs viennent-ils dans le zoo ? (necessite une Entree du Zoo)")
                .define("visitorsEnabled", true);
        MAX_VISITORS = b
                .comment("Plafond de visiteurs simultanes par monde (anti-lag).")
                .defineInRange("maxVisitors", 12, 1, 60);
        VISITOR_SKINS = b
                .comment("Pseudos Minecraft dont les visiteurs empruntent le skin (telecharges par le client).",
                        "Vide = skins Minecraft par defaut (Steve/Alex/Ari/Efe/Kai/Makena/Noor/Sunny/Zuri).",
                        "Exemple : [\"Notch\", \"jeb_\", \"Dinnerbone\"]")
                .defineList("visitorSkins", java.util.List.of(),
                        o -> o instanceof String s && !s.isBlank() && s.length() <= 16);
        PLUSH_PRICE = b
                .comment("Prix de vente par defaut des peluches (Plushie Mod & co), en billets de parc.")
                .defineInRange("plushPrice", 4, 1, 64);
        STAR_SPECIES = b
                .comment("Especes VEDETTES : chacune presente dans le zoo augmente l'affluence de 20%.",
                        "Mets ici les especes rares de ton modpack (id complet).")
                .defineList("starSpecies", java.util.List.of(
                        "minecraft:panda", "minecraft:axolotl", "minecraft:sniffer", "minecraft:allay"),
                        o -> o instanceof String s && s.contains(":"));
        PET_SPECIES = b
                .comment("Animaux de compagnie : especes que le mod ignore completement.",
                         "Elles gardent leur comportement vanilla : pas de fiche, pas",
                         "d'enclos, pas de soigneur, pas de bien-etre.")
                .defineList("petSpecies", java.util.List.of(
                        "minecraft:wolf", "minecraft:cat", "minecraft:parrot"),
                        o -> o instanceof String);
        PATH_BLOCKS = b
                .comment("Blocs qui comptent comme ALLEE (en plus de toutes les dalles/slabs et de l'Allee de zoo).",
                        "Mets ici tes blocs pleins d'allee : planches, chemins, etc. (id complet).")
                .defineList("pathBlocks", java.util.List.of(
                        "minecraft:birch_planks", "minecraft:oak_planks", "minecraft:gravel"),
                        o -> o instanceof String s && s.contains(":"));

        b.pop().push("capture");

        GLOBAL_CAPTURE_MULTIPLIER = b
                .comment("Multiplicateur global des chances de capture/taming. >1 = plus facile.")
                .defineInRange("globalCaptureMultiplier", 1.0, 0.1, 10.0);

        b.pop();
        SPEC = b.build();
    }
}
