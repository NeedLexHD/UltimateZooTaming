package com.lex3d.ultimatezootaming.social;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.Random;

/**
 * Fabrique les posts a partir de l'ETAT MENTAL du visiteur et de ce qu'il a
 * sous les yeux. Un visiteur content devant un bel enclos publie un eloge ;
 * un visiteur agace dans un parc sale publie une plainte.
 *
 * Appele uniquement quand le visiteur est assis sur un banc (il sort son
 * telephone), donc rarement : aucun impact mesurable sur les TPS.
 */
public final class PostGenerator {

    private PostGenerator() {}

    /**
     * Compose et publie un post pour ce visiteur.
     * @return true si un post a ete publie
     */
    public static boolean postFrom(ServerLevel level, VisitorEntity visitor) {
        ZooLedger ledger = ZooLedger.get(level);
        Random rng = new Random(visitor.getId() * 31L + level.getGameTime());

        // Ce que le visiteur a autour de lui decide du contenu
        var zone = zoneUnder(level, visitor);
        String subject = zone != null ? zone.getName() : "";
        String species = nearestSpecies(level, visitor);

        ZooPost.Kind kind = pickKind(level, visitor, ledger, rng);
        ZooPost post = new ZooPost(kind, ZooPost.makeHandle(rng), subject, species, ledger.getDay());
        // Un premier elan de likes selon l'humeur du visiteur
        post.likes = rng.nextInt(1 + Math.max(1, visitor.getJoy() / 12));
        ledger.publishPost(post);
        return true;
    }

    /** Choisit le type de post selon l'humeur et l'environnement. */
    private static ZooPost.Kind pickKind(ServerLevel level, VisitorEntity visitor,
                                         ZooLedger ledger, Random rng) {
        int joy = visitor.getJoy();

        // Visiteur mecontent : il se plaint de ce qui l'a gene
        if (joy < 35) {
            var complaints = new java.util.ArrayList<ZooPost.Kind>();
            if (litterNearby(level, visitor)) complaints.add(ZooPost.Kind.DIRTY_PARK);
            if (crowdedAround(level, visitor)) complaints.add(ZooPost.Kind.TOO_CROWDED);
            if (ledger.getTicketPolicy() == 2) complaints.add(ZooPost.Kind.OVERPRICED);
            complaints.add(ZooPost.Kind.SAD_ANIMAL);
            return complaints.get(rng.nextInt(complaints.size()));
        }

        // Visiteur ravi : il met en avant ce qui l'a marque
        if (joy > 70) {
            var praises = new java.util.ArrayList<ZooPost.Kind>();
            praises.add(ZooPost.Kind.BEAUTIFUL_ENCLOSURE);
            praises.add(ZooPost.Kind.CUTE_MOMENT);
            if (babyNearby(level, visitor)) praises.add(ZooPost.Kind.BABY_BORN);
            if (rareNearby(level, visitor)) praises.add(ZooPost.Kind.RARE_SPOTTED);
            return praises.get(rng.nextInt(praises.size()));
        }

        // Entre les deux : un post neutre
        return ZooPost.Kind.GREAT_DAY;
    }

    // ---- Lectures d'environnement, toutes en scan plat ou par registre ----

    private static com.lex3d.ultimatezootaming.zones.ZooZone zoneUnder(
            ServerLevel level, VisitorEntity v) {
        for (var z : ZooSavedData.get(level).getAllZones()) {
            if (z.contains(v.blockPosition())) return z;
        }
        return null;
    }

    /** L'espece la plus proche, pour la vignette du post. */
    private static String nearestSpecies(ServerLevel level, VisitorEntity v) {
        var animals = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                new AABB(v.blockPosition()).inflate(12));
        if (animals.isEmpty()) return "";
        var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getKey(animals.get(0).getType());
        return id == null ? "" : id.toString();
    }

    private static boolean babyNearby(ServerLevel level, VisitorEntity v) {
        return !level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                new AABB(v.blockPosition()).inflate(12),
                net.minecraft.world.entity.animal.Animal::isBaby).isEmpty();
    }

    private static boolean rareNearby(ServerLevel level, VisitorEntity v) {
        for (var a : level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                new AABB(v.blockPosition()).inflate(12))) {
            var d = a.getCapability(
                    com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                    .resolve().orElse(null);
            if (d != null && d.getRarity() > 0) return true;
        }
        return false;
    }

    private static boolean crowdedAround(ServerLevel level, VisitorEntity v) {
        return level.getEntitiesOfClass(VisitorEntity.class,
                new AABB(v.blockPosition()).inflate(6)).size() >= 7;
    }

    /** Scan plat autour du visiteur, jamais un volume. */
    private static boolean litterNearby(ServerLevel level, VisitorEntity v) {
        var litter = com.lex3d.ultimatezootaming.core.init.ModBlocks.LITTER.get();
        var base = v.blockPosition();
        for (int dx = -8; dx <= 8; dx += 2) {
            for (int dz = -8; dz <= 8; dz += 2) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (level.getBlockState(base.offset(dx, dy, dz)).is(litter)) return true;
                }
            }
        }
        return false;
    }
}
