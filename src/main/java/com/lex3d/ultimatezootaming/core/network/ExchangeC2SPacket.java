package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le joueur echange des billets de parc contre un minerai au comptoir. */
public class ExchangeC2SPacket {

    private final BlockPos pos;
    private final int index; // index du minerai dans la table d'echange

    public ExchangeC2SPacket(BlockPos pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    public static void encode(ExchangeC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.index);
    }

    public static ExchangeC2SPacket decode(FriendlyByteBuf buf) {
        return new ExchangeC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    /** Table d'echange : {item, cout en billets, quantite rendue}. Partagee client/serveur. */
    public static final Object[][] TABLE = {
            {Items.COAL, 3, 4},
            {Items.COPPER_INGOT, 4, 2},
            {Items.IRON_INGOT, 8, 1},
            {Items.REDSTONE, 5, 6},
            {Items.LAPIS_LAZULI, 6, 4},
            {Items.GOLD_INGOT, 18, 1},
            {Items.DIAMOND, 45, 1},
            {Items.EMERALD, 30, 1},
            {Items.NETHERITE_INGOT, 400, 1},
    };

    public static void handle(ExchangeC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5,
                    packet.pos.getZ() + 0.5) > 64) return;
            if (packet.index < 0 || packet.index >= TABLE.length) return;
            Object[] row = TABLE[packet.index];
            int cost = (int) row[1];
            int qty = (int) row[2];
            // Preleve les billets sur la TRESORERIE totale du zoo
            if (!ZooVaultBlock.withdrawFromTreasury(level, cost)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.exchange_poor", cost), true);
                return;
            }
            // Donne le minerai au joueur
            player.getInventory().placeItemBackInInventory(
                    new ItemStack((net.minecraft.world.item.Item) row[0], qty));
            // Re-sync le solde a l'ecran
            ZooVaultBlockEntity vault = ZooVaultBlock.nearestVault(level, packet.pos, 8);
            if (vault == null && level.getBlockEntity(packet.pos) instanceof ZooVaultBlockEntity v) vault = v;
            int balance = vault != null ? vault.getBalance() : 0;
            NetworkHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new OpenVaultS2CPacket(packet.pos, balance, ZooVaultBlock.totalBalance(level)));
        });
        ctx.setPacketHandled(true);
    }
}
