package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Vrai multiblock : une cage 2x2/3x3/5x5 occupe reellement N x N BlockPos physiques
 * dans le monde. Une seule cellule est "master" (celle cliquee pour placer) et
 * stocke l'appat ; les autres relaient vers elle.
 *
 * Detection PRINCIPALE = collision reelle (entityInside, "plaque de pression").
 * Detection DE SECOURS = un tick leger toutes les 5 ticks (voir getTicker), au cas
 * ou entityInside serait rate pour une raison quelconque (mob qui saute par-dessus,
 * edge case de moteur physique...). Ce n'est plus un "scan actif" au sens ou
 * l'entite doit deja physiquement toucher la cellule -- ca reste local a chaque
 * cellule, juste avec un filet de securite en plus de l'event pur.
 */
public class TrappingCageBlock extends BaseEntityBlock {

    /** Propriete d'etat : true = appat charge, visible sur la texture (voir blockstate JSON). */
    public static final BooleanProperty BAITED = BooleanProperty.create("baited");

    /** Orientation de la cage (la porte fait face au joueur a la pose). Le
     *  GeoBlockRenderer de GeckoLib lit cette propriete et tourne le modele seul. */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Plaque fine (4/16 de bloc de haut), assez basse pour lire "plaque", assez haute pour cliquer facilement. */
    private static final VoxelShape PLATE_SHAPE = box(0, 0, 0, 16, 4, 16);

    public enum CageSize {
        SMALL(1, false, 0.9f),        // poule, lapin, chat, loup...
        MEDIUM(2, false, 1.5f),       // cochon, mouton, vache, ours...
        LARGE(3, false, 2.5f),        // cheval, gros mobs moddes...
        UNBREAKABLE(5, true, 99.0f);  // tout

        private final int radius;
        private final boolean unbreakable;
        private final float maxMobSize;

        CageSize(int radius, boolean unbreakable, float maxMobSize) {
            this.radius = radius;
            this.unbreakable = unbreakable;
            this.maxMobSize = maxMobSize;
        }

        /** Taille max (largeur/hauteur de hitbox) d'un mob capturable par cette cage. */
        public float getMaxMobSize() {
            return maxMobSize;
        }

        public int getRadius() {
            return radius;
        }

        public boolean isUnbreakable() {
            return unbreakable;
        }
    }

    private final CageSize size;

    public TrappingCageBlock(CageSize size, Properties properties) {
        super(properties);
        this.size = size;
        this.registerDefaultState(this.stateDefinition.any().setValue(BAITED, false)
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(BAITED, FACING);
    }

    public CageSize getSize() {
        return size;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // ENTITYBLOCK_ANIMATED : le rendu est entierement pris en charge par le
        // GeoBlockRenderer (GeckoLib), le modele JSON statique n'est plus utilise
        // pour le bloc pose (il sert encore a l'item en inventaire).
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

    /** Explicite plutot que de compter uniquement sur .noCollission() des Properties : zero ambiguite. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrappingCageBlockEntity(pos, state, size);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof TrappingCageBlockEntity cage) {
                cage.serverTick();
            }
        };
    }

    /**
     * Detection PRINCIPALE : appelee des que la hitbox d'une entite chevauche cette
     * cellule (comportement "plaque de pression" reel, pas de minuteur pour ca).
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide()) return;
        if (!(level.getBlockEntity(pos) instanceof TrappingCageBlockEntity cell)) return;

        TrappingCageBlockEntity master = cell.resolveMaster();
        if (master != null) {
            master.onEntityCollide(entity);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof TrappingCageBlockEntity cell) {
            BlockPos masterPos = cell.getMasterPos();
            int n = size.getRadius();
            for (int dx = 0; dx < n; dx++) {
                for (int dz = 0; dz < n; dz++) {
                    BlockPos cellPos = masterPos.offset(dx, 0, dz);
                    if (!cellPos.equals(pos) && level.getBlockState(cellPos).getBlock() == this) {
                        level.setBlock(cellPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TrappingCageBlockEntity cell) {
            TrappingCageBlockEntity master = cell.resolveMaster();
            if (master == null) return InteractionResult.PASS;

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
            if (!held.isEmpty() && !master.hasBait()) {
                master.setBait(held.copy(), player.getUUID());
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                return InteractionResult.sidedSuccess(level.isClientSide());
            } else if (master.hasBait()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.ultimatezootaming.cage_already_baited"), true);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return InteractionResult.PASS;
    }
}
