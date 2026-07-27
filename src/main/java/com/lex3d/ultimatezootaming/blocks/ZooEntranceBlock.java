package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.welfare.ZooScore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entree du Zoo : point d'arrivee et de depart des visiteurs. Pose-la au bout de
 * ton allee principale. Clic droit = infos (note du zoo, prix du billet, affluence).
 */
public class ZooEntranceBlock extends Block implements net.minecraft.world.level.block.EntityBlock {

    /** PORTIQUE TRAVERSABLE : aucune collision, on marche a travers comme sous
     *  un portique de parc d'attractions. Le tourniquet reste visible (modele
     *  GeckoLib) mais ne bloque ni le joueur ni les visiteurs. */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(
            BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    /** Selection (clic droit) : boite basse pour pouvoir viser le bloc au sol. */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(
            BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return net.minecraft.world.phys.shapes.Shapes.box(0.0, 0.0, 0.0, 1.0, 0.25, 1.0);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new ZooEntranceBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(
            net.minecraft.world.level.block.state.BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }


    private static final Map<ResourceKey<Level>, Set<BlockPos>> ENTRANCES = new ConcurrentHashMap<>();

    public ZooEntranceBlock(Properties properties) { super(properties); }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) {
            ENTRANCES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())) {
            Set<BlockPos> s = ENTRANCES.get(level.dimension());
            if (s != null) s.remove(pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
        var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(serverLevel);
        // Sneak : le directeur regle le prix du billet (Bas / Normal / Cher)
        if (player.isShiftKeyDown()) {
            ledger.cycleTicketPolicy();
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.ticket_policy",
                    Component.translatable("ticket.ultimatezootaming.p" + ledger.getTicketPolicy())), true);
            return InteractionResult.CONSUME;
        }
        int score = ZooScore.compute(serverLevel);
        int visitors = serverLevel.getEntitiesOfClass(
                com.lex3d.ultimatezootaming.entities.VisitorEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(200)).size();
        int price = Math.max(1, (int) Math.round(ZooScore.ticketPrice(score) * ledger.priceFactor()));
        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.entrance_info", score, price, visitors), false);
        int stars = ZooScore.starCount(serverLevel);
        if (stars > 0) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.entrance_stars", stars), false);
        }
        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.entrance_policy",
                Component.translatable("ticket.ultimatezootaming.p" + ledger.getTicketPolicy())), false);
        return InteractionResult.CONSUME;
    }

    /** Toutes les entrees d'un monde (pour le spawn des visiteurs). */
    public static Set<BlockPos> entrancesIn(Level level) {
        Set<BlockPos> set = ENTRANCES.get(level.dimension());
        if (set == null) return Set.of();
        set.removeIf(p -> !(level.getBlockState(p).getBlock() instanceof ZooEntranceBlock));
        return set;
    }

    /** Enregistre une entree trouvee au chargement (scan de secours). */
    public static void remember(Level level, BlockPos pos) {
        ENTRANCES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }
}
