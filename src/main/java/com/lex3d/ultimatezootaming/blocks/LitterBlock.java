package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * DECHET AU SOL : laisse par un visiteur qui finit sa consommation sans poubelle
 * a proximite. Il abaisse l'ambiance de la zone et fait rale les autres visiteurs.
 *
 * Ramassable au clic droit (par toi) ou par un Agent d'entretien pendant sa
 * ronde. Le ramassage rend un DECHET RECYCLABLE, matiere premiere utile.
 *
 * La variete (0-2) change juste l'aspect : gobelet, papier, emballage.
 */
public class LitterBlock extends Block {

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    private static final VoxelShape SHAPE = Shapes.box(0.15, 0.0, 0.15, 0.85, 0.15, 0.85);

    public LitterBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(VARIANT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** On marche a travers : un dechet ne bloque pas le passage. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** Clic droit = ramassage manuel par le joueur. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        collect(level, pos, player);
        return InteractionResult.CONSUME;
    }

    /**
     * Ramasse le dechet : il disparait et rend un Dechet recyclable.
     * @param toPlayer si non nul, l'item va dans son inventaire ; sinon il tombe au sol
     */
    public static void collect(Level level, BlockPos pos, Player toPlayer) {
        if (!level.getBlockState(pos).is(
                com.lex3d.ultimatezootaming.core.init.ModBlocks.LITTER.get())) return;
        level.removeBlock(pos, false);
        var drop = new net.minecraft.world.item.ItemStack(
                com.lex3d.ultimatezootaming.core.init.ModItems.RECYCLABLE_WASTE.get());
        if (toPlayer != null) {
            toPlayer.getInventory().placeItemBackInInventory(drop);
        } else {
            Block.popResource(level, pos, drop);
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WOOL_BREAK,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.3f);
    }

    /**
     * Depose un dechet a cet endroit si la case est libre et posee sur du solide.
     * @return true si le dechet a bien ete cree
     */
    public static boolean tryLitter(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.below()).isSolidRender(level, pos.below())) return false;
        int variant = level.getRandom().nextInt(3);
        level.setBlock(pos, com.lex3d.ultimatezootaming.core.init.ModBlocks.LITTER.get()
                .defaultBlockState().setValue(VARIANT, variant), 3);
        return true;
    }
}
