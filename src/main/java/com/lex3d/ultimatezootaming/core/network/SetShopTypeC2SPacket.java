package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ShopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le client choisit le type de commerce d'une caisse (bouton du GUI). */
public class SetShopTypeC2SPacket {

    private final BlockPos pos;
    private final int type;

    public SetShopTypeC2SPacket(BlockPos pos, int type) {
        this.pos = pos;
        this.type = type;
    }

    public static void encode(SetShopTypeC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.type);
    }

    public static SetShopTypeC2SPacket decode(FriendlyByteBuf buf) {
        return new SetShopTypeC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(SetShopTypeC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5,
                    packet.pos.getZ() + 0.5) > 64) return;
            if (player.serverLevel().getBlockEntity(packet.pos) instanceof ShopBlockEntity shop) {
                shop.setShopType(packet.type);
            }
        });
        ctx.setPacketHandled(true);
    }
}
