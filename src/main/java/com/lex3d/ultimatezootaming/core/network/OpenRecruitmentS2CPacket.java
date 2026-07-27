package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OpenRecruitmentS2CPacket {

    public record Candidate(String name, int job, int skill, int cost) {}

    public final BlockPos pos;
    public final List<Candidate> candidates;

    public OpenRecruitmentS2CPacket(BlockPos pos, List<Candidate> candidates) {
        this.pos = pos;
        this.candidates = candidates;
    }

    public static void encode(OpenRecruitmentS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.candidates.size());
        for (Candidate c : p.candidates) {
            buf.writeUtf(c.name());
            buf.writeInt(c.job());
            buf.writeInt(c.skill());
            buf.writeInt(c.cost());
        }
    }

    public static OpenRecruitmentS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int n = buf.readInt();
        List<Candidate> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Candidate(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt()));
        }
        return new OpenRecruitmentS2CPacket(pos, list);
    }

    public static void handle(OpenRecruitmentS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.lex3d.ultimatezootaming.client.ClientSetup.openRecruitment(packet)));
        ctx.setPacketHandled(true);
    }
}
