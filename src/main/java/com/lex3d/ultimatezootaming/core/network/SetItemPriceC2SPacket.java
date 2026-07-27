package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.PriceRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/** Le client valide le prix d'un item (defini a la caisse enregistreuse). */
public class SetItemPriceC2SPacket {

    private final ResourceLocation itemId;
    private final int price;   // 0 = retirer de la vente
    private final int shopType;

    public SetItemPriceC2SPacket(ResourceLocation itemId, int price, int shopType) {
        this.itemId = itemId;
        this.price = price;
        this.shopType = shopType;
    }

    public static void encode(SetItemPriceC2SPacket p, FriendlyByteBuf buf) {
        buf.writeResourceLocation(p.itemId);
        buf.writeInt(p.price);
        buf.writeInt(p.shopType);
    }

    public static SetItemPriceC2SPacket decode(FriendlyByteBuf buf) {
        return new SetItemPriceC2SPacket(buf.readResourceLocation(), buf.readInt(), buf.readInt());
    }

    public static void handle(SetItemPriceC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Item item = ForgeRegistries.ITEMS.getValue(packet.itemId);
            if (item == null) return;
            int price = Math.max(0, Math.min(999, packet.price));
            int type = Math.floorMod(packet.shopType, 4);
            PriceRegistry.get(player.serverLevel()).setPrice(item, price, type);
            if (price <= 0) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.price_removed", item.getDescription()), true);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.price_set", item.getDescription(), price,
                        Component.translatable("shop.ultimatezootaming."
                                + com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.values()[type]
                                        .name().toLowerCase())), true);
            }
        });
        ctx.setPacketHandled(true);
    }
}
