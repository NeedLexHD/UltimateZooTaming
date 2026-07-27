package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.items.GeneticSampleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * INCUBATEUR : machine de reproduction assistee, rendue par GeckoLib.
 *
 * Clic droit avec un ECHANTILLON GENETIQUE pour l'inserer (deux echantillons de
 * la meme espece sont necessaires). Clic droit a main vide pour connaitre l'etat
 * du cycle, ou sneak + clic droit pour recuperer les echantillons.
 */
public class IncubatorBlock extends HorizontalDirectionalBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.9, 1.0);

    public IncubatorBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** Le rendu est entierement pris en charge par GeckoLib. */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof IncubatorBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack held = player.getItemInHand(hand);

        // Sneak : recuperer les echantillons et annuler le cycle
        if (player.isShiftKeyDown()) {
            var out = be.ejectSamples();
            if (out.isEmpty()) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.incubator_empty"), true);
            } else {
                for (ItemStack s : out) player.getInventory().placeItemBackInInventory(s);
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.incubator_ejected"), true);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_EMPTY,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.0f);
            }
            return InteractionResult.CONSUME;
        }

        // Inserer un echantillon
        if (GeneticSampleItem.isValid(held)) {
            if (be.insertSample(held)) {
                if (!player.isCreative()) held.shrink(1);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.2f);
                player.displayClientMessage(Component.translatable(
                        be.canStart() ? "message.ultimatezootaming.incubator_started"
                                      : "message.ultimatezootaming.incubator_need_second"), true);
            } else {
                // Refus : machine pleine ou especes differentes
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.incubator_mismatch")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            return InteractionResult.CONSUME;
        }

        // Main vide : etat du cycle
        if (be.isWorking()) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.incubator_progress",
                    (int) (be.getProgressRatio() * 100)), true);
        } else if (be.getSampleA().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.incubator_hint"), true);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.incubator_need_second"), true);
        }
        return InteractionResult.CONSUME;
    }

    /** Les echantillons tombent si on casse la machine. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())
                && level.getBlockEntity(pos) instanceof IncubatorBlockEntity be) {
            for (ItemStack s : be.ejectSamples()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, s);
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IncubatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof IncubatorBlockEntity inc) {
                IncubatorBlockEntity.serverTick(lvl, pos, st, inc);
            }
        };
    }
}
