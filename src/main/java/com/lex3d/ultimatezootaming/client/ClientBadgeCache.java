package com.lex3d.ultimatezootaming.client;

import com.lex3d.ultimatezootaming.core.network.FamiliarBadgeS2CPacket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache client de l'etat "badge" des familiers (trait + malade), alimente par le
 * serveur toutes les ~1s. Le renderer (FamiliarBadgeRenderer) le lit pour dessiner
 * l'icone au-dessus de chaque animal. Les entrees expirent si plus rafraichies.
 */
public class ClientBadgeCache {

    private record Entry(int traitOrdinal, boolean sick, long updatedAt) {}
    private static final Map<Integer, Entry> CACHE = new HashMap<>();
    private static final long TTL = 3000; // 3s sans refresh -> oublie

    public static void update(List<FamiliarBadgeS2CPacket.Badge> badges) {
        long now = System.currentTimeMillis();
        for (FamiliarBadgeS2CPacket.Badge b : badges) {
            CACHE.put(b.entityId(), new Entry(b.traitOrdinal(), b.sick(), now));
        }
    }

    /** Index de trait de l'entite (0 = aucun), ou -1 si pas de donnee fraiche. */
    public static int traitOf(int entityId) {
        Entry e = CACHE.get(entityId);
        if (e == null || System.currentTimeMillis() - e.updatedAt() > TTL) return -1;
        return e.traitOrdinal();
    }

    public static boolean isSick(int entityId) {
        Entry e = CACHE.get(entityId);
        return e != null && System.currentTimeMillis() - e.updatedAt() <= TTL && e.sick();
    }

    public static boolean hasData(int entityId) {
        Entry e = CACHE.get(entityId);
        return e != null && System.currentTimeMillis() - e.updatedAt() <= TTL;
    }
}
