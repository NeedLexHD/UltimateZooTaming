package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Ajoute/retire le bloc TENU EN MAIN de la liste des allees (bouton de la Carte). */
public class TogglePathBlockC2SPacket {

    public TogglePathBlockC2SPacket() {}

    public static void encode(TogglePathBlockC2SPacket p, FriendlyByteBuf buf) {}

    public static TogglePathBlockC2SPacket decode(FriendlyByteBuf buf) {
        return new TogglePathBlockC2SPacket();
    }

    public static void handle(TogglePathBlockC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack held = player.getOffhandItem(); // main secondaire
            if (held.isEmpty() || !(held.getItem() instanceof BlockItem blockItem)) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.pathblock_hold"), true);
                return;
            }
            boolean added = com.lex3d.ultimatezootaming.saveddata.ZooPathBlocks.get(player.serverLevel())
                    .toggle(blockItem.getBlock());
            player.displayClientMessage(Component.translatable(
                    added ? "message.ultimatezootaming.pathblock_added"
                          : "message.ultimatezootaming.pathblock_removed",
                    held.getHoverName()), true);
        });
        ctx.setPacketHandled(true);
    }
}
