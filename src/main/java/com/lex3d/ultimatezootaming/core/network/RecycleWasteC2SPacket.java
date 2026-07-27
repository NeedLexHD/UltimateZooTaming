package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.core.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * RECYCLAGE : convertit les Dechets recyclables de l'inventaire du joueur en
 * billets verses a la Tresorerie. Nettoyer le parc rapporte donc vraiment.
 */
public class RecycleWasteC2SPacket {

    /** Combien de dechets pour combien de billets. */
    public static final int WASTE_PER_LOT = 8;
    public static final int TICKETS_PER_LOT = 5;

    private final net.minecraft.core.BlockPos pos;

    public RecycleWasteC2SPacket(net.minecraft.core.BlockPos pos) { this.pos = pos; }

    public static void encode(RecycleWasteC2SPacket p, FriendlyByteBuf buf) { buf.writeBlockPos(p.pos); }
    public static RecycleWasteC2SPacket decode(FriendlyByteBuf buf) {
        return new RecycleWasteC2SPacket(buf.readBlockPos());
    }

    public static void handle(RecycleWasteC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5,
                    packet.pos.getZ() + 0.5) > 64) return;

            // Compte les dechets dans l'inventaire
            int total = 0;
            var inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(ModItems.RECYCLABLE_WASTE.get())) total += s.getCount();
            }
            int lots = total / WASTE_PER_LOT;
            if (lots <= 0) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.recycle_none", WASTE_PER_LOT), true);
                return;
            }

            // Retire les dechets consommes
            int toRemove = lots * WASTE_PER_LOT;
            for (int i = 0; i < inv.getContainerSize() && toRemove > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.is(ModItems.RECYCLABLE_WASTE.get())) continue;
                int take = Math.min(toRemove, s.getCount());
                s.shrink(take);
                toRemove -= take;
            }

            // Verse les billets a la Tresorerie
            int gain = lots * TICKETS_PER_LOT;
            var vault = ZooVaultBlock.anyVault(player.serverLevel());
            if (vault != null) vault.deposit(gain);

            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.recycle_ok", lots * WASTE_PER_LOT, gain)
                    .withStyle(ChatFormatting.GREEN), true);

            // Rafraichit l'ecran de la Tresorerie
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new OpenVaultS2CPacket(packet.pos,
                            vault != null ? vault.getBalance() : 0,
                            ZooVaultBlock.totalBalance(player.serverLevel())));
        });
        ctx.setPacketHandled(true);
    }
}
