package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Allee de zoo : simple bloc decoratif de chemin (15/16 de haut, comme un
 *  chemin de terre). Les allees se tracent avec N'IMPORTE QUEL bloc via la
 *  Carte du Zoo — celui-ci est juste une option esthetique. */
public class ZooPathBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);

    public ZooPathBlock(Properties properties) { super(properties); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

}
