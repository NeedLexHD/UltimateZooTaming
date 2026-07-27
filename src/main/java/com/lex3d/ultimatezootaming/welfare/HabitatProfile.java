package com.lex3d.ultimatezootaming.welfare;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Profils d'habitat definis par TYPES DE BLOCS (c'est un zoo : pas besoin du vrai
 * biome, il suffit que les blocs de l'enclos correspondent). AUTO = heuristique.
 */
public enum HabitatProfile {
    AUTO,       // heuristique par defaut (vegetation/eau/lave selon le mob)
    FOREST,     // herbe + feuilles/fleurs
    BEACH,      // sable + eau
    DESERT,     // sable/gres
    TUNDRA,     // neige/glace
    SWAMP,      // eau + herbe/argile
    ROCKY,      // pierre/graviers
    JUNGLE,     // feuilles + herbe dense
    AQUATIC,    // eau majoritaire
    NETHER,     // netherrack/lave
    SAVANNA,    // acacia + herbe
    BADLANDS,   // terracotta / sable rouge
    LUSH,       // mousse / azalea / grotte luxuriante
    MUSHROOM;   // mycelium / champignons

    /** Ce bloc satisfait-il ce profil ? */
    public boolean matches(BlockState s) {
        return switch (this) {
            case FOREST -> s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.LEAVES) || s.is(BlockTags.FLOWERS)
                    || s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN);
            case BEACH -> s.is(Blocks.SAND) || s.getFluidState().is(FluidTags.WATER);
            case DESERT -> s.is(Blocks.SAND) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SAND)
                    || s.is(Blocks.DEAD_BUSH) || s.is(Blocks.CACTUS);
            case TUNDRA -> s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.SNOW) || s.is(BlockTags.ICE)
                    || s.is(Blocks.POWDER_SNOW);
            case SWAMP -> s.getFluidState().is(FluidTags.WATER) || s.is(Blocks.CLAY)
                    || s.is(Blocks.LILY_PAD) || s.is(Blocks.MUD) || s.is(Blocks.GRASS_BLOCK);
            case ROCKY -> s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL)
                    || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE);
            case JUNGLE -> s.is(BlockTags.LEAVES) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.VINE)
                    || s.is(Blocks.BAMBOO) || s.is(Blocks.MELON) || s.is(Blocks.COCOA);
            case AQUATIC -> s.getFluidState().is(FluidTags.WATER);
            case NETHER -> s.is(BlockTags.NYLIUM) || s.is(Blocks.NETHERRACK)
                    || s.getFluidState().is(FluidTags.LAVA) || s.is(Blocks.SOUL_SAND);
            case SAVANNA -> s.is(Blocks.ACACIA_LEAVES) || s.is(Blocks.ACACIA_LOG)
                    || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.GRASS);
            case BADLANDS -> s.is(Blocks.RED_SAND) || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.DEAD_BUSH);
            case LUSH -> s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.AZALEA)
                    || s.is(Blocks.FLOWERING_AZALEA) || s.is(Blocks.BIG_DRIPLEAF) || s.is(Blocks.CLAY);
            case MUSHROOM -> s.is(Blocks.MYCELIUM) || s.is(Blocks.RED_MUSHROOM_BLOCK)
                    || s.is(Blocks.BROWN_MUSHROOM_BLOCK) || s.is(Blocks.MUSHROOM_STEM)
                    || s.is(Blocks.RED_MUSHROOM) || s.is(Blocks.BROWN_MUSHROOM);
            default -> false;
        };
    }
}
