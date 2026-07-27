package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Guichet de billetterie : les visiteurs doivent y passer avant de rentrer
 * dans le zoo. Chaque guichet peut vendre 1 billet toutes les 5s environ, donc
 * plus tu en poses, plus le debit est eleve.
 * Fonctionnel automatiquement : pas besoin de vendeur, c'est le "sesame" du zoo.
 * Retro-compatible : si tu n'en poses pas, le zoo tourne comme avant (spawn direct).
 */
public class TicketBoothBlock extends HorizontalDirectionalBlock {

    /** Registre des guichets par monde (pour permettre au VisitorSpawnHandler de
     *  choisir un point d'arrivee proche d'un guichet). */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<Level>,
            java.util.Set<BlockPos>> BOOTHS = new java.util.concurrent.ConcurrentHashMap<>();

    public static java.util.Set<BlockPos> getBoothsIn(Level level) {
        var s = BOOTHS.get(level.dimension());
        return s == null ? java.util.Collections.emptySet() : s;
    }

    /** Y a-t-il au moins un guichet dans ce monde ? Utilise pour savoir si on
     *  active le mode "billetterie physique" (spawn eloigne). */
    public static boolean hasAnyBooth(Level level) {
        var s = BOOTHS.get(level.dimension());
        return s != null && !s.isEmpty();
    }

    public TicketBoothBlock(Properties p) {
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

    /** Guichet plat (comme un comptoir), 1 bloc de haut visuellement mais
     *  seulement 14/16 de collision (les visiteurs et le joueur peuvent passer
     *  au-dessus / a cote). */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.875, 1.0);
    }

    /** Collision : seulement le bas (comptoir), 14/16. Au-dessus, on peut passer. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.875, 1.0);
    }

    /** Selection : la meme shape que visuelle. */
    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.box(0.0, 0.0, 0.0, 1.0, 0.875, 1.0);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) {
            BOOTHS.computeIfAbsent(level.dimension(),
                    k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())) {
            var s = BOOTHS.get(level.dimension());
            if (s != null) s.remove(pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }
}
