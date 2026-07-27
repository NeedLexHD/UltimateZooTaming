package com.lex3d.ultimatezootaming.social;

import net.minecraft.nbt.CompoundTag;

/**
 * UN POST DU FLUX SOCIAL.
 *
 * Choix d'architecture : on ne stocke QUE des donnees brutes (type, auteur,
 * cible, likes, jour). Le texte affiche est compose cote client a partir de la
 * cle de traduction du type.
 *
 * Pourquoi : une chaine de caracteres formatee alourdirait la sauvegarde et
 * empecherait de traduire le flux. Ici un post pese une poignee d'octets et
 * s'affiche dans la langue du joueur.
 */
public class ZooPost {

    /** Ce qui a declenche le post. Determine le ton et l'impact. */
    public enum Kind {
        // --- Positifs ---
        BEAUTIFUL_ENCLOSURE("beautiful", 3),
        BABY_BORN("baby", 5),
        RARE_SPOTTED("rare", 4),
        GREAT_DAY("great_day", 2),
        CUTE_MOMENT("cute", 3),
        // --- Negatifs ---
        DIRTY_PARK("dirty", -3),
        SAD_ANIMAL("sad_animal", -5),
        TOO_CROWDED("crowded", -2),
        OVERPRICED("pricey", -2),
        LONG_QUEUE("queue", -2);

        public final String key;
        /** Influence sur l'affluence : positif attire, negatif repousse. */
        public final int impact;

        Kind(String key, int impact) { this.key = key; this.impact = impact; }
        public boolean isPositive() { return impact > 0; }
    }

    public Kind kind = Kind.GREAT_DAY;
    /** Pseudo de l'auteur, genere a partir du visiteur. */
    public String author = "";
    /** Sujet du post : nom d'enclos ou d'espece, selon le type. Peut etre vide. */
    public String subject = "";
    /** Identifiant d'espece pour la vignette, vide si le post n'en a pas. */
    public String speciesId = "";
    public int likes = 0;
    public int day = 0;

    public ZooPost() {}

    public ZooPost(Kind kind, String author, String subject, String speciesId, int day) {
        this.kind = kind;
        this.author = author;
        this.subject = subject;
        this.speciesId = speciesId;
        this.day = day;
    }

    /** Cle de traduction du corps du post. */
    public String textKey() { return "post.ultimatezootaming." + kind.key; }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("Kind", kind.name());
        t.putString("Author", author);
        t.putString("Subject", subject);
        t.putString("Species", speciesId);
        t.putInt("Likes", likes);
        t.putInt("Day", day);
        return t;
    }

    public static ZooPost load(CompoundTag t) {
        ZooPost p = new ZooPost();
        try {
            p.kind = Kind.valueOf(t.getString("Kind"));
        } catch (IllegalArgumentException e) {
            p.kind = Kind.GREAT_DAY;
        }
        p.author = t.getString("Author");
        p.subject = t.getString("Subject");
        p.speciesId = t.getString("Species");
        p.likes = t.getInt("Likes");
        p.day = t.getInt("Day");
        return p;
    }

    // --- Generation de pseudos, pour que le flux ait l'air vivant ---

    private static final String[] FIRST = {
        "lea", "tom", "nina", "hugo", "zoe", "max", "ines", "noah", "jade", "eli",
        "sam", "kim", "alex", "remy", "lou", "ari", "milo", "nael", "sofia", "yann"
    };
    private static final String[] SUFFIX = {
        "_zoo", "2000", "_off", "xx", "_pix", "_travel", "_wild", "07", "_snap", ""
    };

    /** Fabrique un pseudo plausible a partir d'une graine stable. */
    public static String makeHandle(java.util.Random rng) {
        return "@" + FIRST[rng.nextInt(FIRST.length)] + SUFFIX[rng.nextInt(SUFFIX.length)];
    }
}
