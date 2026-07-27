package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.blocks.TrappingCageBlock;
import com.lex3d.ultimatezootaming.blocks.TrappingCageBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Place reellement les N x N cellules physiques de la cage (pas un seul bloc avec
 * une "zone d'effet" invisible). Le footprint s'etend vers +X/+Z depuis la cellule
 * cliquee. Une seule cellule ("master", celle cliquee) stocke l'appat ; voir
 * TrappingCageBlockEntity#resolveMaster.
 */
public class TrappingCageBlockItem extends BlockItem {

    private final TrappingCageBlock cageBlock;

    public TrappingCageBlockItem(TrappingCageBlock block, Properties properties) {
        super(block, properties);
        this.cageBlock = block;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos().relative(context.getClickedFace());
        int n = cageBlock.getSize().getRadius();

        // Verifie que TOUT l'espace necessaire est libre avant de placer quoi que ce soit
        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                BlockState existing = level.getBlockState(pos);
                if (!existing.canBeReplaced()) {
                    return InteractionResult.FAIL;
                }
            }
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState cageState = cageBlock.defaultBlockState()
                .setValue(TrappingCageBlock.FACING, context.getHorizontalDirection().getOpposite());
        for (int dx = 0; dx < n; dx++) {
            for (int dz = 0; dz < n; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                level.setBlockAndUpdate(pos, cageState);
                if (level.getBlockEntity(pos) instanceof TrappingCageBlockEntity cell) {
                    cell.setMasterPos(origin);
                    if (pos.equals(origin)) {
                        cell.setSourceDamage(context.getItemInHand().getDamageValue());
                    }
                }
            }
        }

        level.playSound(null, origin, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                                java.util.List<Component> tooltip, TooltipFlag flag) {
        String key = switch (cageBlock.getSize()) {
            case SMALL -> "small";
            case MEDIUM -> "medium";
            case LARGE -> "large";
            case UNBREAKABLE -> "all";
        };
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.trap_capacity",
                Component.translatable("capacity.ultimatezootaming." + key)));
    }
}
