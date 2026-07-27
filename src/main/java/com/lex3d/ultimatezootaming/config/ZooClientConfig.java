package com.lex3d.ultimatezootaming.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Config TOML client : ultimatezootame-client.toml
 * Contient la liste des modIds coches dans le ConfigModScreen (la "Forced List").
 * Un mod dans cette liste est traite par le systeme de Croquettes meme s'il a
 * deja son propre systeme de taming natif (ex : Polly's Pets).
 */
public class ZooClientConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORCED_MOD_IDS;
    public static final ForgeConfigSpec.DoubleValue GLOBAL_RNG_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue SHOW_HOLO_BADGE;
    /** Afficher la tache en cours au-dessus des employes. */
    public static final ForgeConfigSpec.BooleanValue SHOW_KEEPER_TASK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Ultimate Zoo Taming - Configuration Client")
                .push("general");

        FORCED_MOD_IDS = builder
                .comment(
                        "Liste des modIds dont TOUS les mobs doivent passer par le systeme de",
                        "Croquettes + RNG, meme si ce mod a deja son propre systeme de taming.",
                        "Modifiable via le bouton 'Configurer' dans la liste des mods (ConfigModScreen)."
                )
                .defineList("forcedModIds", new ArrayList<String>(), obj -> obj instanceof String);

        GLOBAL_RNG_MULTIPLIER = builder
                .comment("Multiplicateur global applique a toutes les chances de capture/taming.")
                .defineInRange("globalRngMultiplier", 1.0, 0.1, 5.0);

        SHOW_HOLO_BADGE = builder
                .comment("Afficher le Holo-Badge au-dessus des familiers quand le Sifflet est en main.")
                .define("showHoloBadge", true);

        SHOW_KEEPER_TASK = builder
                .comment("Afficher au-dessus de chaque employe la tache qu'il execute.")
                .define("showKeeperTask", true);

        builder.pop();
        SPEC = builder.build();
    }
}
