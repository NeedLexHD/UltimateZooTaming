package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.welfare.WelfareDiagnostic;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Panneau d'enclos : pose-le dans (ou au bord d') un enclos, clic droit = le
 * diagnostic complet de l'enclos (le meme rapport que le soigneur), meme quand
 * le soigneur est en pause au vestiaire.
 */
public class ZooSignBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock {

    /** Panneaux poses, par monde (bonus educatif pour les visiteurs). */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,
            java.util.Set<net.minecraft.core.BlockPos>> SIGNS = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean anySignNear(net.minecraft.world.level.Level level,
                                      net.minecraft.core.BlockPos pos, double range) {
        var set = SIGNS.get(level.dimension());
        if (set == null) return false;
        double r2 = range * range;
        for (net.minecraft.core.BlockPos p : set) if (p.distSqr(pos) <= r2) return true;
        return false;
    }

    @Override
    public void onPlace(net.minecraft.world.level.block.state.BlockState state,
                        net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                        net.minecraft.world.level.block.state.BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) {
            SIGNS.computeIfAbsent(level.dimension(),
                    k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }
    }

    @Override
    public void onRemove(net.minecraft.world.level.block.state.BlockState state,
                         net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                         net.minecraft.world.level.block.state.BlockState newState, boolean moving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            var set = SIGNS.get(level.dimension());
            if (set != null) set.remove(pos);
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
            net.minecraft.core.BlockPos bePos, net.minecraft.world.level.block.state.BlockState beState) {
        return new DecorBlockEntity(bePos, beState);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(
            net.minecraft.world.level.block.state.BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }


    private static final VoxelShape SHAPE = Block.box(2, 0, 6, 14, 16, 10);

    public ZooSignBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        // Trouve l'enclos qui contient (ou borde) le panneau
        for (ZooZone zone : ZooSavedData.get(serverLevel).getAllZones()) {
            if (zone.containsNear(pos)) {
                WelfareDiagnostic.report(serverLevel, zone, player);
                int amb = com.lex3d.ultimatezootaming.welfare.AmbianceScore.of(serverLevel, zone);
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.sign_ambiance", amb), false);
                return InteractionResult.CONSUME;
            }
        }
        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.sign_no_zone"), true);
        return InteractionResult.CONSUME;
    }
}
