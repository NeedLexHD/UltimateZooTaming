package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoie la fiche de competences d'un employe au client. */
public class SyncSkillsS2CPacket {

    public final int entityId;
    public final String name;
    public final int job;
    public final int level;
    public final int xp;
    public final int freePoints;
    public final int[] ranks;

    public SyncSkillsS2CPacket(int entityId, String name, int job, int level,
                               int xp, int freePoints, int[] ranks) {
        this.entityId = entityId;
        this.name = name;
        this.job = job;
        this.level = level;
        this.xp = xp;
        this.freePoints = freePoints;
        this.ranks = ranks;
    }

    public static void encode(SyncSkillsS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId);
        buf.writeUtf(p.name);
        buf.writeInt(p.job);
        buf.writeInt(p.level);
        buf.writeInt(p.xp);
        buf.writeInt(p.freePoints);
        buf.writeVarIntArray(p.ranks);
    }

    public static SyncSkillsS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncSkillsS2CPacket(buf.readInt(), buf.readUtf(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readVarIntArray());
    }

    public static void handle(SyncSkillsS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.openSkills(packet)));
        ctx.setPacketHandled(true);
    }
}
