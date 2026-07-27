package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Filet posable dans l'eau (waterlogged, comme une cloture) ou sur le sol pres
 * de l'eau. Meme principe que TrappingCageBlock mais en un seul bloc (pas de
 * multiblock) : plaque fine traversable, appat visible, capture par collision
 * reelle + filet de securite en tick, retour visuel/sonore succes-echec.
 */
public class NetTrapBlock extends BaseEntityBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty BAITED = TrappingCageBlock.BAITED;

    private static final VoxelShape PLATE_SHAPE = box(0, 0, 0, 16, 4, 16);

    public enum NetTier {
        SMALL(0.05f, 1, 0.9f),       // petits poissons
        REINFORCED(0.15f, 2, 1.8f),  // calamars, moyens
        POOL(0.28f, 3, 99.0f);       // tout (orques des mods, etc.)

        private final float bonus;
        private final int radius;
        private final float maxMobSize;

        NetTier(float bonus, int radius, float maxMobSize) {
            this.bonus = bonus;
            this.radius = radius;
            this.maxMobSize = maxMobSize;
        }

        /** Taille max (largeur/hauteur de hitbox) d'un mob capturable par ce filet. */
        public float getMaxMobSize() {
            return maxMobSize;
        }

        public float getBonus() {
            return bonus;
        }

        /** Cote du footprint NxN, comme les cages : Petit=1x1, Renforce=2x2, Bassin=3x3. */
        public int getRadius() {
            return radius;
        }
    }

    private final NetTier tier;

    public NetTrapBlock(NetTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(BAITED, false));
    }

    public NetTier getTier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(WATERLOGGED, BAITED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        boolean waterlogged = fluidState.getType() == Fluids.WATER;
        return defaultBlockState().setValue(WATERLOGGED, waterlogged).setValue(BAITED, false);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Rendu entierement pris en charge par le GeoBlockRenderer (GeckoLib).
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PLATE_SHAPE;
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PLATE_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetTrapBlockEntity(pos, state, tier);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof NetTrapBlockEntity trap) {
                trap.serverTick();
            }
        };
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof NetTrapBlockEntity cell)) return;
        NetTrapBlockEntity master = cell.resolveMaster();
        if (master != null) {
            master.onEntityCollide(entity);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof NetTrapBlockEntity cell) {
            BlockPos masterPos = cell.getMasterPos();
            int n = tier.getRadius();
            for (int dx = 0; dx < n; dx++) {
                for (int dz = 0; dz < n; dz++) {
                    BlockPos cellPos = masterPos.offset(dx, 0, dz);
                    if (!cellPos.equals(pos) && level.getBlockState(cellPos).getBlock() == this) {
                        level.setBlock(cellPos, level.getFluidState(cellPos).getType() == Fluids.WATER
                                ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof NetTrapBlockEntity cell) {
            NetTrapBlockEntity trap = cell.resolveMaster();
            if (trap == null) return InteractionResult.PASS;
            ItemStack held = player.getItemInHand(hand);
            // APPAT : uniquement des CROQUETTES. Sans ce filtre, n'importe quel
            // objet faisait office d'appat, ce qui vidait de son sens le systeme
            // de regimes alimentaires.
            if (!held.isEmpty() && !(held.getItem() instanceof com.lex3d.ultimatezootaming.items.KibbleItem)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.bait_kibble_only")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
            if (!held.isEmpty() && !trap.hasBait()) {
                trap.setBait(held.copy(), player.getUUID());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else if (trap.hasBait()) {
                player.displayClientMessage(
                        Component.translatable("message.ultimatezootaming.cage_already_baited"), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }
}
