package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Les avis des visiteurs : un message rare et lisible dans le chat, pour que le
 * joueur sache ce qui cloche. Anti-spam : un avis toutes les 45s maximum.
 */
public final class VisitorOpinion {

    private static final long COOLDOWN_MS = 45_000;
    private static long lastSaid = 0;
    /** Un avis archive : la cle de traduction + une note sur 5 (selon la joie). */
    public record RatedOpinion(String key, int stars) {}
    /** Les derniers avis (cles), pour l'apercu rapide de l'onglet Direction. */
    private static final java.util.Deque<String> RECENT = new java.util.concurrent.ConcurrentLinkedDeque<>();
    /** Historique NOTE des 30 derniers avis, pour l'onglet Avis dedie. */
    private static final java.util.Deque<RatedOpinion> HISTORY = new java.util.concurrent.ConcurrentLinkedDeque<>();

    public static java.util.List<String> recent() {
        return new java.util.ArrayList<>(RECENT);
    }

    public static java.util.List<RatedOpinion> history() {
        return new java.util.ArrayList<>(HISTORY);
    }

    /** Note sur 5 selon le TYPE d'avis (la cle sans le suffixe de variante). */
    /** Nombre de formulations differentes par type d'avis. */
    public static final int VARIANTS = 6;

    private static int ratingFor(String keyWithVariant) {
        // retire le ".0/.1/.2" final
        String k = keyWithVariant;
        int dot = k.lastIndexOf('.');
        if (dot > 0) k = k.substring(0, dot);
        return switch (k) {
            // tres positif
            case "star_animal", "vip_thrilled" -> 5;
            // positif
            case "happy_animals", "learned", "photo_fun", "feed_fun", "water_fun" -> 4;
            // positif (nouveaux)
            case "beautiful", "clean", "staff_nice", "rare_species" -> 4;
            // plaintes de confort (manque un service)
            case "no_drink", "no_food", "no_vendor", "no_bench", "no_bin" -> 2;
            // plaintes (nouveaux)
            case "too_expensive", "crowded", "dirty", "lost", "boring" -> 2;
            // tres negatif
            case "sad_animals", "vip_sad" -> 1;
            default -> 3; // neutre
        };
    }

    private VisitorOpinion() {}

    public static void say(ServerLevel level, VisitorEntity visitor, String key) {
        long now = System.currentTimeMillis();
        if (now - lastSaid < COOLDOWN_MS) return;
        lastSaid = now;
        // 6 variantes par avis : les visiteurs ne repetent pas tous la meme phrase
        key = key + "." + level.random.nextInt(VARIANTS);
        RECENT.addFirst(key);
        while (RECENT.size() > 8) RECENT.removeLast();
        // Note sur 5 deduite du TYPE d'avis (pas de la joie instantanee qui est
        // souvent basse en debut de visite). Positif = 4-5*, neutre = 3*, plainte = 1-2*.
        int stars = ratingFor(key);
        HISTORY.addFirst(new RatedOpinion(key, stars));
        while (HISTORY.size() > 30) HISTORY.removeLast();
        Component msg = Component.literal("\uD83D\uDCAC ").withStyle(ChatFormatting.AQUA)
                .append(Component.translatable("opinion.ultimatezootaming." + key)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.level() == level && p.distanceToSqr(visitor) < 120 * 120) {
                p.sendSystemMessage(msg);
            }
        }
    }
}
