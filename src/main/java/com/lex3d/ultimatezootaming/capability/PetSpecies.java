package com.lex3d.ultimatezootaming.capability;

import net.minecraft.world.entity.Entity;

/**
 * ANIMAUX DE COMPAGNIE : especes que le mod laisse entierement tranquilles.
 *
 * Un loup ou un chat apprivoise reste un compagnon vanilla : il ne devient pas
 * pensionnaire du zoo, n'apparait dans aucun enclos, n'a ni fiche ni bien-etre,
 * et aucun soigneur ne s'en occupe.
 *
 * La liste est modifiable dans la config (petSpecies).
 */
public final class PetSpecies {

    private PetSpecies() {}

    /** Cette entite doit-elle etre ignoree par tout le systeme du zoo ? */
    public static boolean isPet(Entity entity) {
        if (entity == null) return false;
        var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getKey(entity.getType());
        if (id == null) return false;
        String key = id.toString();
        for (String s : com.lex3d.ultimatezootaming.config.ZooServerConfig.PET_SPECIES.get()) {
            if (key.equals(s)) return true;
        }
        return false;
    }
}
