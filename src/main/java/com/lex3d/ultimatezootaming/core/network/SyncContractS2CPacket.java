package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.contracts.ZooContract;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Envoie le contrat en cours et les animaux eligibles au client. */
public class SyncContractS2CPacket {

    /** Un animal du zoo qui remplit les conditions. */
    public record Candidate(int entityId, String name, int welfare, int rarity, boolean baby) {}

    public final boolean hasContract;
    public final String species, client, requirement;
    public final int reward, daysLeft, prestige;
    public final List<Candidate> candidates;

    public SyncContractS2CPacket(ZooContract c, List<Candidate> candidates) {
        boolean active = c != null && c.isActive();
        this.hasContract = active;
        this.species = active ? c.species : "";
        this.client = active ? c.client : "";
        this.requirement = active ? c.requirement.name() : "ANY";
        this.reward = active ? c.reward : 0;
        this.daysLeft = active ? c.daysLeft : 0;
        this.prestige = active ? c.prestige : 0;
        this.candidates = candidates;
    }

    private SyncContractS2CPacket(boolean has, String species, String client, String req,
                                  int reward, int days, int prestige, List<Candidate> cands) {
        this.hasContract = has; this.species = species; this.client = client;
        this.requirement = req; this.reward = reward; this.daysLeft = days;
        this.prestige = prestige; this.candidates = cands;
    }

    public static void encode(SyncContractS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.hasContract);
        buf.writeUtf(p.species);
        buf.writeUtf(p.client);
        buf.writeUtf(p.requirement);
        buf.writeInt(p.reward);
        buf.writeInt(p.daysLeft);
        buf.writeInt(p.prestige);
        buf.writeInt(p.candidates.size());
        for (Candidate c : p.candidates) {
            buf.writeInt(c.entityId());
            buf.writeUtf(c.name());
            buf.writeInt(c.welfare());
            buf.writeInt(c.rarity());
            buf.writeBoolean(c.baby());
        }
    }

    public static SyncContractS2CPacket decode(FriendlyByteBuf buf) {
        boolean has = buf.readBoolean();
        String sp = buf.readUtf(), cl = buf.readUtf(), rq = buf.readUtf();
        int rw = buf.readInt(), dl = buf.readInt(), pr = buf.readInt();
        int n = buf.readInt();
        List<Candidate> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Candidate(buf.readInt(), buf.readUtf(), buf.readInt(),
                    buf.readInt(), buf.readBoolean()));
        }
        return new SyncContractS2CPacket(has, sp, cl, rq, rw, dl, pr, list);
    }

    public static void handle(SyncContractS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.openContract(packet)));
        ctx.setPacketHandled(true);
    }
}
