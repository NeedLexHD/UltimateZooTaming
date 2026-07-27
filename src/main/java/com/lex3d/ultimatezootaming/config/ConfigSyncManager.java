package com.lex3d.ultimatezootaming.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Traduit la config Client -> Serveur.
 *
 * Le client lit son ultimatezootame-client.toml et envoie sa Forced List au serveur
 * via ConfigSyncC2SPacket a la connexion (voir ClientSetup / evenement PlayerLoggedIn).
 * Le serveur stocke ca en RAM (par joueur, car en multijoueur chaque joueur peut avoir
 * une config differente, mais on applique la reunion de toutes les listes recues pour
 * la logique de taming globale -> voir isForced()).
 *
 * C'est volontairement une simple Map en memoire (pas de SavedData) : la donnee est
 * re-envoyee a chaque connexion donc pas besoin de la persister sur disque serveur.
 */
public class ConfigSyncManager {

    private static final Map<UUID, Set<String>> PLAYER_FORCED_MODS = new HashMap<>();
    private static final Set<String> UNION_CACHE = new HashSet<>();

    public static void handleSync(UUID player, java.util.List<String> forcedModIds) {
        Set<String> set = new HashSet<>(forcedModIds);
        PLAYER_FORCED_MODS.put(player, set);
        rebuildUnion();
    }

    public static void onPlayerDisconnect(UUID player) {
        PLAYER_FORCED_MODS.remove(player);
        rebuildUnion();
    }

    private static void rebuildUnion() {
        UNION_CACHE.clear();
        for (Set<String> set : PLAYER_FORCED_MODS.values()) {
            UNION_CACHE.addAll(set);
        }
    }

    /** True si ce modId doit passer par le systeme Croquettes+RNG meme s'il a un taming natif. */
    public static boolean isModForced(String modId) {
        return UNION_CACHE.contains(modId);
    }

    public static Set<String> getUnionForced() {
        return Collections.unmodifiableSet(UNION_CACHE);
    }
}
