package com.lex3d.ultimatezootaming.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boutique du zoo (souvenir / repas / glace / boisson). Le joueur y depose le
 * STOCK, un Vendeur (soigneur metier Vendeur) se poste derriere, et les
 * visiteurs achetent : l'emeraude part dans la Caisse du Zoo.
 */
public class ShopBlock extends BaseEntityBlock {

    /** Type de boutique : 0 souvenir, 1 repas, 2 glace, 3 boisson. */
    public enum ShopType { SOUVENIR, MEAL, ICECREAM, DRINK, PHOTO }

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> SHOPS = new ConcurrentHashMap<>();

    public ShopBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE =
            net.minecraft.world.level.block.Block.box(3, 0, 4, 13, 10, 12);

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShopBlockEntity(pos, state);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!level.isClientSide()) {
            SHOPS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide() && !state.is(next.getBlock())) {
            Set<BlockPos> s = SHOPS.get(level.dimension());
            if (s != null) s.remove(pos);
        }
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof ShopBlockEntity shop)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);

        if (!held.isEmpty()) {
            var reg = com.lex3d.ultimatezootaming.saveddata.PriceRegistry.get(
                    (net.minecraft.server.level.ServerLevel) level);
            int price = reg.priceOf(held);
            // Sneak avec l'item, ou item pas encore tarife : ouvrir l'ecran de prix
            if (player.isShiftKeyDown() || price <= 0) {
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(held.getItem());
                    if (id != null) {
                        com.lex3d.ultimatezootaming.core.network.NetworkHandler.CHANNEL.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                                new com.lex3d.ultimatezootaming.core.network.OpenPriceScreenS2CPacket(
                                        id, Math.max(1, price), shop.getShopTypeEnum().ordinal()));
                    }
                }
                return InteractionResult.CONSUME;
            }
            if (!shop.accepts(held)) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.shop_wrong_item"), true);
                return InteractionResult.CONSUME;
            }
            ItemStack rest = shop.insert(held.copy());
            held.shrink(held.getCount() - rest.getCount());
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        if (held.isEmpty()) {
            // Main vide : le GUI de la caisse (stock + type + prix)
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                net.minecraftforge.network.NetworkHooks.openScreen(sp,
                        new net.minecraft.world.SimpleMenuProvider(
                                (id, inv, p) -> new com.lex3d.ultimatezootaming.client.gui.menu.ShopMenu(
                                        id, inv, shop,
                                        new com.lex3d.ultimatezootaming.client.gui.menu.ShopMenu.ShopData(shop),
                                        pos),
                                Component.translatable("block.ultimatezootaming.cash_register")),
                        buf -> buf.writeBlockPos(pos));
            }
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.shop_stock", shop.countStock(),
                Component.translatable("shop.ultimatezootaming."
                        + shop.getShopTypeEnum().name().toLowerCase())), true);
        return InteractionResult.CONSUME;
    }

    /** Toutes les caisses du monde (GUI de direction). */
    public static java.util.List<BlockPos> allShops(Level level) {
        Set<BlockPos> set = SHOPS.get(level.dimension());
        if (set == null) return java.util.List.of();
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        for (BlockPos p : set) {
            if (level.getBlockState(p).getBlock() instanceof ShopBlock) out.add(p);
        }
        return out;
    }

    /** La boutique la plus proche (pour le Vendeur). */
    @Nullable
    public static BlockPos nearestShop(Level level, BlockPos from, double maxDist) {
        Set<BlockPos> set = SHOPS.get(level.dimension());
        if (set == null || set.isEmpty()) return null;
        BlockPos best = null;
        double bd = maxDist * maxDist;
        for (BlockPos p : set) {
            if (!(level.getBlockState(p).getBlock() instanceof ShopBlock)) { set.remove(p); continue; }
            double d = from.distSqr(p);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }

    /** Scan de secours apres un redemarrage. */
    @Nullable
    public static BlockPos scanForShop(Level level, BlockPos center, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -6, -radius), center.offset(radius, 6, radius))) {
            if (level.getBlockState(p).getBlock() instanceof ShopBlock) return p.immutable();
        }
        return null;
    }
}
