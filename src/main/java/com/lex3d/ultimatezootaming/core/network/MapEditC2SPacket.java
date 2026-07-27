package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ZooPathBlock;
import com.lex3d.ultimatezootaming.core.init.ModBlocks;
import com.lex3d.ultimatezootaming.saveddata.ZooTerritory;
import com.lex3d.ultimatezootaming.welfare.ZooScore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Edition depuis la Carte : revendiquer/liberer un chunk, poser/retirer une
 * allee. Les allees CONSOMMENT des "Allee de zoo" de l'inventaire (et les
 * rendent au retrait) — la carte est un pinceau, pas de la triche.
 */
public class MapEditC2SPacket {

    /** 0 = claim chunk, 1 = unclaim chunk, 2 = poser allee, 3 = retirer allee. */
    private final int action;
    private final int x, z;

    public MapEditC2SPacket(int action, int x, int z) {
        this.action = action;
        this.x = x;
        this.z = z;
    }

    public static void encode(MapEditC2SPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.action);
        buf.writeInt(p.x);
        buf.writeInt(p.z);
    }

    public static MapEditC2SPacket decode(FriendlyByteBuf buf) {
        return new MapEditC2SPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(MapEditC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            // Garde-fou distance : on n'edite pas a l'autre bout du monde
            if (Math.abs(player.getBlockX() - packet.x) > 128
                    || Math.abs(player.getBlockZ() - packet.z) > 128) return;
            ZooTerritory territory = ZooTerritory.get(level);

            switch (packet.action) {
                case 0 -> {
                    territory.claim(new ChunkPos(packet.x >> 4, packet.z >> 4));
                    // Le chunk revendique reste charge : le zoo tourne sans joueur
                    level.setChunkForced(packet.x >> 4, packet.z >> 4, true);
                }
                case 1 -> {
                    territory.unclaim(new ChunkPos(packet.x >> 4, packet.z >> 4));
                    level.setChunkForced(packet.x >> 4, packet.z >> 4, false);
                }
                case 2 -> placePath(player, level, packet.x, packet.z, territory);
                case 3 -> removePath(player, level, packet.x, packet.z);
            }
            // Rafraichir la carte du joueur (meme centre que sa vue actuelle : on
            // renvoie centre sur la case editee arrondie — le client garde son centre)
        });
        ctx.setPacketHandled(true);
    }

    /** MARQUE une case comme chemin (aucun bloc pose : tu construis tes allees
     *  toi-meme, la carte enregistre juste le plan — les visiteurs le suivent). */
    private static void placePath(ServerPlayer player, ServerLevel level, int x, int z,
                                  ZooTerritory territory) {
        com.lex3d.ultimatezootaming.saveddata.ZooPaths.get(level).mark(x, z);
    }

    private static void removePath(ServerPlayer player, ServerLevel level, int x, int z) {
        com.lex3d.ultimatezootaming.saveddata.ZooPaths.get(level).unmark(x, z);
    }
}
