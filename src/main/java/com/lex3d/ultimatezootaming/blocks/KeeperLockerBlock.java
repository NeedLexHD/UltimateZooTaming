package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vestiaire de soigneur : VRAI casier metal, 2 blocs de haut, orientable,
 * rendu GeckoLib (moitie basse). Les soigneurs sans enclos y retournent.
 */
public class KeeperLockerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape SHAPE = Block.box(1, 0, 3, 15, 16, 13);

    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOCKERS = new ConcurrentHashMap<>();

    private final String variant; // "wood" (defaut), "green", "blue", "red"

    public KeeperLockerBlock(Properties properties) {
        this(properties, "wood");
    }

    public KeeperLockerBlock(Properties properties, String variant) {
        super(properties);
        this.variant = variant;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public String getVariant() { return variant; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        if (pos.getY() >= ctx.getLevel().getMaxBuildHeight() - 1
                || !ctx.getLevel().getBlockState(pos.above()).canBeReplaced(ctx)) {
            return null; // pas la place pour la moitie haute
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return true;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Casser l'autre moitie proprement (comme une porte)
        if (!level.isClientSide()) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos other = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                level.setBlock(other, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;
        BlockPos bePos = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        if (!(level.getBlockEntity(bePos) instanceof KeeperLockerBlockEntity locker)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        var held = player.getItemInHand(hand);
        boolean stockable = held.getItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem
                || held.getItem() instanceof com.lex3d.ultimatezootaming.items.AnimalRemedyItem;
        if (stockable) {
            var rest = locker.insert(held.copy());
            int moved = held.getCount() - rest.getCount();
            if (moved > 0) {
                held.shrink(moved);
                level.playSound(null, bePos, net.minecraft.sounds.SoundEvents.IRON_DOOR_CLOSE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.4f);
            }
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        // Main vide (ou autre item) : le stock s'ouvre comme un coffre
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new net.minecraft.world.inventory.ChestMenu(
                            net.minecraft.world.inventory.MenuType.GENERIC_9x1, id, inv, locker, 1),
                    // Utilise le NOM REEL du bloc (chaque couleur a sa traduction :
                    // Vestiaire - Veterinaire, - Garde, etc.) au lieu du generique.
                    level.getBlockState(bePos).getBlock().getName()));
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // La moitie basse est rendue par GeckoLib, la haute est invisible
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER
                ? new KeeperLockerBlockEntity(pos, state) : null;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            LOCKERS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide() && !state.is(newState.getBlock())
                && state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            Set<BlockPos> set = LOCKERS.get(level.dimension());
            if (set != null) set.remove(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    /** Vestiaire le plus proche dans un rayon donne, ou null. */
    /** Enregistre un vestiaire (appele par le BlockEntity a onLoad). */
    public static void register(Level level, BlockPos pos) {
        if (!level.isClientSide()) {
            LOCKERS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet())
                    .add(pos.immutable());
        }
    }

    public static BlockPos nearestLocker(Level level, BlockPos from, double maxDist) {
        Set<BlockPos> set = LOCKERS.get(level.dimension());
        if (set == null || set.isEmpty()) return null;
        BlockPos best = null;
        double bestD = maxDist * maxDist;
        for (BlockPos p : set) {
            if (!(level.getBlockState(p).getBlock() instanceof KeeperLockerBlock)) {
                set.remove(p);
                continue;
            }
            double d = from.distSqr(p);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }

    /**
     * Cherche le casier le plus proche QUI CONTIENT un item donne. Avant, le
     * nourrisseur prenait le casier le plus proche meme vide, et abandonnait :
     * c'est pour ca qu'il ne ravitaillait jamais les mangeoires.
     */
    @Nullable
    public static BlockPos nearestLockerWith(Level level, BlockPos from, double maxDist,
                                             java.util.function.Predicate<net.minecraft.world.Container> test) {
        Set<BlockPos> set = LOCKERS.get(level.dimension());
        if (set == null || set.isEmpty()) return null;
        BlockPos best = null;
        double bestD = maxDist * maxDist;
        for (BlockPos p : set) {
            if (!(level.getBlockState(p).getBlock() instanceof KeeperLockerBlock)) {
                set.remove(p);
                continue;
            }
            if (!(level.getBlockEntity(p) instanceof net.minecraft.world.Container c)) continue;
            if (!test.test(c)) continue; // ce casier n'a pas ce qu'il faut
            double d = from.distSqr(p);
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }
}
