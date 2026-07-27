package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.client.ClientWelfareCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Reponse du serveur : le bien-etre d'un familier vise, stocke dans un cache client. */
public class WelfareInfoS2CPacket {

    private final int entityId;
    private final int satisfaction;
    private final boolean sick;
    private final boolean inZone;
    private final String trait;

    public WelfareInfoS2CPacket(int entityId, int satisfaction, boolean sick, boolean inZone, String trait) {
        this.entityId = entityId;
        this.satisfaction = satisfaction;
        this.sick = sick;
        this.inZone = inZone;
        this.trait = trait;
    }

    public static void encode(WelfareInfoS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
        buf.writeInt(packet.satisfaction);
        buf.writeBoolean(packet.sick);
        buf.writeBoolean(packet.inZone);
        buf.writeUtf(packet.trait);
    }

    public static WelfareInfoS2CPacket decode(FriendlyByteBuf buf) {
        return new WelfareInfoS2CPacket(buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readUtf());
    }

    public static void handle(WelfareInfoS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientWelfareCache.put(packet.entityId, packet.satisfaction, packet.sick, packet.inZone, packet.trait)));
        ctx.setPacketHandled(true);
    }
}
