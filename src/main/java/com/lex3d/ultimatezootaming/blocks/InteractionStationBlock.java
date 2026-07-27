package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/**
 * Borne d'interaction visiteur : Photo, Nourrissage, ou Jet d'eau. Le visiteur
 * s'y arrete, paie, une animation joue, et ca profite au visiteur ET (pour
 * nourrissage/eau) a l'animal le plus proche.
 */
public class InteractionStationBlock extends HorizontalDirectionalBlock
        implements net.minecraft.world.level.block.EntityBlock {

    public enum Kind { PHOTO, FEED, WATER }

    /**
     * REGISTRE DES BORNES, par monde.
     *
     * Avant, chercher une borne balayait un volume de 61x13x61, soit environ
     * 48 000 blocs, a chaque fois qu'un visiteur planifiait son etape suivante.
     * Avec vingt visiteurs, ca devenait la premiere source de lag du mod.
     *
     * On tient desormais la liste a jour a la pose et au retrait, comme pour les
     * vestiaires, les tresoreries et les jouets : la recherche devient un simple
     * parcours de quelques positions.
     */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,
            java.util.Set<BlockPos>> STATIONS = new java.util.concurrent.ConcurrentHashMap<>();

    public static java.util.Set<BlockPos> getStationsIn(net.minecraft.world.level.Level level) {
        var s = STATIONS.get(level.dimension());
        return s == null ? java.util.Set.of() : s;
    }

    /** Enregistre une borne (pose, ou rechargement de chunk). */
    public static void register(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        STATIONS.computeIfAbsent(level.dimension(),
                k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    /**
     * La borne la plus proche, eventuellement filtree.
     * @param exclude positions a ignorer (bornes deja utilisees par ce visiteur)
     */
    @javax.annotation.Nullable
    public static BlockPos nearestStation(net.minecraft.world.level.Level level, BlockPos from,
                                          double maxDist,
                                          java.util.function.Predicate<BlockPos> accept) {
        var set = STATIONS.get(level.dimension());
        if (set == null || set.isEmpty()) return null;
        BlockPos best = null;
        double bestD = maxDist * maxDist;
        for (BlockPos p : set) {
            // Nettoyage paresseux : une borne disparue sort du registre
            if (!(level.getBlockState(p).getBlock() instanceof InteractionStationBlock)) {
                set.remove(p);
                continue;
            }
            if (accept != null && !accept.test(p)) continue;
            double d = from.distSqr(p);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    /** Y a-t-il une borne dans ce rayon ? Reponse immediate. */
    public static boolean anyStationWithin(net.minecraft.world.level.Level level, BlockPos from, double dist) {
        return nearestStation(level, from, dist, null) != null;
    }

    private final Kind kind;

    public InteractionStationBlock(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public Kind getKind() { return kind; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    /** CLIC DROIT sur la borne : declenche l'effet (flash photo / nourrissage /
     *  jet d'eau) pour tester, et le montre a tout le monde. */
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, net.minecraft.world.level.Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            StationEffect.play(sl, pos);
            // Declenche l'animation GeckoLib (flash qui s'allume / buse qui tire)
            if (sl.getBlockEntity(pos) instanceof StationBlockEntity be) {
                be.triggerUse();
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                        BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        register(level, pos);
    }

    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                         BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())) {
            var s = STATIONS.get(level.dimension());
            if (s != null) s.remove(pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
            BlockPos pos, BlockState state) {
        return new StationBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }
}
