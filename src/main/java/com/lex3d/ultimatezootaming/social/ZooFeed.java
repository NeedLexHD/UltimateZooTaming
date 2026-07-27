package com.lex3d.ultimatezootaming.social;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * LE FLUX SOCIAL DU ZOO.
 *
 * Conserve les derniers posts et calcule la "temperature" du parc : un flux
 * majoritairement positif attire du monde, un bad buzz le fait fuir.
 *
 * Plafond strict de 50 posts : au-dela, les plus anciens tombent. La sauvegarde
 * reste donc bornee quoi qu'il arrive.
 */
public class ZooFeed {

    /** Nombre maximum de posts conserves. */
    public static final int MAX_POSTS = 50;

    private final Deque<ZooPost> posts = new ArrayDeque<>();
    private final Random rng = new Random();

    public List<ZooPost> getPosts() { return new ArrayList<>(posts); }
    public int size() { return posts.size(); }

    /** Publie un post et fait tomber le plus ancien si le flux deborde. */
    public void publish(ZooPost post) {
        posts.addFirst(post);
        while (posts.size() > MAX_POSTS) posts.removeLast();
    }

    /**
     * Fait vivre le flux : les posts recents gagnent des likes au fil du temps.
     * Appele une fois par jour, pas a chaque tick.
     */
    public void tickLikes(int currentDay) {
        int i = 0;
        for (ZooPost p : posts) {
            if (i++ >= 12) break;                 // seuls les posts recents bougent
            int age = currentDay - p.day;
            if (age > 3) continue;                // un vieux post ne decolle plus
            // Un post positif accroche mieux qu'une plainte
            int gain = p.kind.isPositive() ? rng.nextInt(14) : rng.nextInt(6);
            p.likes += gain;
        }
    }

    /**
     * Multiplicateur d'affluence issu de la viralite, borne entre 0.7 et 1.4.
     *
     * On pondere chaque post recent par son impact ET par ses likes : un post
     * positif tres like pese plus lourd qu'un post ignore.
     */
    public double buzzFactor(int currentDay) {
        double score = 0;
        int counted = 0;
        for (ZooPost p : posts) {
            int age = currentDay - p.day;
            if (age > 3) continue;                 // seuls les 3 derniers jours comptent
            double weight = 1.0 + Math.min(2.0, p.likes / 40.0);
            score += p.kind.impact * weight;
            if (++counted >= 20) break;
        }
        if (counted == 0) return 1.0;
        // score typique entre -60 et +90 : on ramene ca dans une plage raisonnable
        double factor = 1.0 + (score / 220.0);
        return Math.max(0.7, Math.min(1.4, factor));
    }

    /** Part de posts positifs parmi les recents, pour l'indicateur d'humeur. */
    public int moodPercent(int currentDay) {
        int pos = 0, total = 0;
        for (ZooPost p : posts) {
            if (currentDay - p.day > 3) continue;
            total++;
            if (p.kind.isPositive()) pos++;
        }
        return total == 0 ? 50 : (pos * 100) / total;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        ListTag list = new ListTag();
        for (ZooPost p : posts) list.add(p.save());
        t.put("Posts", list);
        return t;
    }

    public static ZooFeed load(CompoundTag t) {
        ZooFeed f = new ZooFeed();
        ListTag list = t.getList("Posts", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < MAX_POSTS; i++) {
            f.posts.addLast(ZooPost.load(list.getCompound(i)));
        }
        return f;
    }
}
