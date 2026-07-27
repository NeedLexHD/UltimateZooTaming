package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le serveur demande au client d'ouvrir l'ecran de tarification d'un item. */
public class OpenPriceScreenS2CPacket {

    private final ResourceLocation itemId;
    private final int currentPrice;
    private final int shopType;

    public OpenPriceScreenS2CPacket(ResourceLocation itemId, int currentPrice, int shopType) {
        this.itemId = itemId;
        this.currentPrice = currentPrice;
        this.shopType = shopType;
    }

    public static void encode(OpenPriceScreenS2CPacket p, FriendlyByteBuf buf) {
        buf.writeResourceLocation(p.itemId);
        buf.writeInt(p.currentPrice);
        buf.writeInt(p.shopType);
    }

    public static OpenPriceScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenPriceScreenS2CPacket(buf.readResourceLocation(), buf.readInt(), buf.readInt());
    }

    public static void handle(OpenPriceScreenS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.lex3d.ultimatezootaming.client.ClientSetup.openPriceScreen(
                                packet.itemId, packet.currentPrice, packet.shopType)));
        ctx.setPacketHandled(true);
    }
}
