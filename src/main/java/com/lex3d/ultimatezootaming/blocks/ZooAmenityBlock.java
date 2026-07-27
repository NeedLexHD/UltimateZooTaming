package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Mobilier de zoo : banc (repos des visiteurs) ou poubelle (proprete). */
public class ZooAmenityBlock extends HorizontalDirectionalBlock implements net.minecraft.world.level.block.EntityBlock {

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


    public enum Kind { BENCH, BIN }

    private static final Map<Kind, Map<ResourceKey<Level>, Set<BlockPos>>> REG = new HashMap<>();

    private final Kind kind;
    private final VoxelShape shape;

    public ZooAmenityBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.shape = kind == Kind.BENCH ? Block.box(0, 0, 4, 16, 10, 12) : Block.box(4, 0, 4, 12, 14, 12);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind getKind() { return kind; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return shape;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) {
            REG.computeIfAbsent(kind, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet())
                    .add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())) {
            var m = REG.get(kind);
            if (m != null) {
                var s = m.get(level.dimension());
                if (s != null) s.remove(pos);
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }

    /** Le mobilier le plus proche d'un type donne, ou null. */
    @Nullable
    public static BlockPos nearest(Kind kind, Level level, BlockPos from, double maxDist) {
        var m = REG.get(kind);
        if (m == null) return null;
        Set<BlockPos> set = m.get(level.dimension());
        if (set == null || set.isEmpty()) return null;
        BlockPos best = null;
        double bd = maxDist * maxDist;
        for (BlockPos p : set) {
            if (!(level.getBlockState(p).getBlock() instanceof ZooAmenityBlock b) || b.getKind() != kind) {
                set.remove(p);
                continue;
            }
            double d = from.distSqr(p);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }
}
