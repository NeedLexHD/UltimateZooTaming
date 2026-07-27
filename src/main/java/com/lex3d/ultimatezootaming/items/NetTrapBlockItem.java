package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.blocks.NetTrapBlock;
import com.lex3d.ultimatezootaming.blocks.NetTrapBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.HitResult;

/**
 * Place le footprint complet N x N du filet (Petit=1x1, Renforce=2x2, Bassin=3x3),
 * exactement comme TrappingCageBlockItem pour les cages -- ET gere le placement
 * en cliquant directement SUR l'eau (raytrace des fluides, comme le nenuphar) :
 * chaque cellule posee dans un bloc d'eau devient waterlogged automatiquement.
 */
public class NetTrapBlockItem extends BlockItem {

    private final NetTrapBlock netBlock;

    public NetTrapBlockItem(NetTrapBlock block, Properties properties) {
        super(block, properties);
        this.netBlock = block;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // Priorite au clic sur l'eau (le raytrace vanilla la traverse sinon)
        BlockHitResult fluidHit = getPlayerPOVHitResult(context.getLevel(), player, ClipContext.Fluid.SOURCE_ONLY);
        BlockPos origin;
        if (fluidHit.getType() == HitResult.Type.BLOCK
                && context.getLevel().getFluidState(fluidHit.getBlockPos()).getType() == Fluids.WATER) {
            origin = fluidHit.getBlockPos();
        } else {
            origin = context.getClickedPos().relative(context.getClickedFace());
        }

        return placeFootprint(context.getLevel(), player, context.getHand(), origin);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Cas "eau profonde / plein ocean" : aucun bloc solide a portee, seul use() est appele
        BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        ItemStack stack = player.getItemInHand(hand);
        if (fluidHit.getType() == HitResult.Type.BLOCK
                && level.getFluidState(fluidHit.getBlockPos()).getType() == Fluids.WATER) {
            InteractionResult result = placeFootprint(level, player, hand, fluidHit.getBlockPos());
            if (result.consumesAction()) {
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    private InteractionResult placeFootprint(Level level, Player player, InteractionHand hand, BlockPos origin) {
        int n = netBlock.getTier().getRadius();

        // Tout l'espace N x N doit etre libre (air ou eau)
        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                if (!level.getBlockState(origin.offset(dx, 0, dz)).canBeReplaced()) {
                    return InteractionResult.FAIL;
                }
            }
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                boolean water = level.getFluidState(pos).getType() == Fluids.WATER;
                BlockState state = netBlock.defaultBlockState()
                        .setValue(NetTrapBlock.WATERLOGGED, water)
                        .setValue(NetTrapBlock.BAITED, false);
                level.setBlockAndUpdate(pos, state);
                if (level.getBlockEntity(pos) instanceof NetTrapBlockEntity cell) {
                    cell.setMasterPos(origin);
                    if (pos.equals(origin)) {
                        cell.setSourceDamage(player.getItemInHand(hand).getDamageValue());
                    }
                }
            }
        }

        level.playSound(null, origin, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 0.9f);

        if (!player.getAbilities().instabuild) {
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level,
                                java.util.List<Component> tooltip, TooltipFlag flag) {
        String key = switch (netBlock.getTier()) {
            case SMALL -> "small";
            case REINFORCED -> "medium";
            case POOL -> "all";
        };
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.trap_capacity",
                Component.translatable("capacity.ultimatezootaming." + key)));
    }
}
