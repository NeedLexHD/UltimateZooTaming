package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.social.ZooPost;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Envoie le flux social au client. */
public class SyncFeedS2CPacket {

    /** Un post allege pour le transport : on n'envoie que l'essentiel. */
    public record Entry(String kind, String author, String subject,
                        String species, int likes, int day) {}

    public final List<Entry> entries;
    public final int moodPercent;
    public final int buzzPercent;
    public final int currentDay;

    public SyncFeedS2CPacket(List<ZooPost> posts, int mood, int buzz, int day) {
        this.entries = new ArrayList<>();
        for (ZooPost p : posts) {
            entries.add(new Entry(p.kind.name(), p.author, p.subject,
                    p.speciesId, p.likes, p.day));
        }
        this.moodPercent = mood;
        this.buzzPercent = buzz;
        this.currentDay = day;
    }

    /**
     * Fabrique utilisee au decodage.
     *
     * On ne peut PAS en faire un constructeur : apres effacement de type,
     * SyncFeedS2CPacket(List<Entry>, ...) et SyncFeedS2CPacket(List<ZooPost>, ...)
     * ont exactement la meme signature et le compilateur les refuse.
     */
    private static SyncFeedS2CPacket fromEntries(List<Entry> entries, int mood, int buzz, int day) {
        SyncFeedS2CPacket p = new SyncFeedS2CPacket(java.util.List.<ZooPost>of(), mood, buzz, day);
        p.entries.addAll(entries);
        return p;
    }

    public static void encode(SyncFeedS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.moodPercent);
        buf.writeInt(p.buzzPercent);
        buf.writeInt(p.currentDay);
        buf.writeInt(p.entries.size());
        for (Entry e : p.entries) {
            buf.writeUtf(e.kind());
            buf.writeUtf(e.author());
            buf.writeUtf(e.subject());
            buf.writeUtf(e.species());
            buf.writeInt(e.likes());
            buf.writeInt(e.day());
        }
    }

    public static SyncFeedS2CPacket decode(FriendlyByteBuf buf) {
        int mood = buf.readInt(), buzz = buf.readInt(), day = buf.readInt();
        int n = buf.readInt();
        List<Entry> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readUtf(), buf.readUtf(), buf.readUtf(),
                    buf.readUtf(), buf.readInt(), buf.readInt()));
        }
        return fromEntries(list, mood, buzz, day);
    }

    public static void handle(SyncFeedS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.openFeed(packet)));
        ctx.setPacketHandled(true);
    }
}
