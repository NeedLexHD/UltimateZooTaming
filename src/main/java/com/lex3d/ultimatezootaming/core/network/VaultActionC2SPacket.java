package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Actions sur la Caisse du Zoo : 0 = tout deposer, 1 = retirer 16, 2 = retirer 64. */
public class VaultActionC2SPacket {

    private final BlockPos pos;
    private final int action;

    public VaultActionC2SPacket(BlockPos pos, int action) {
        this.pos = pos;
        this.action = action;
    }

    public static void encode(VaultActionC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.action);
    }

    public static VaultActionC2SPacket decode(FriendlyByteBuf buf) {
        return new VaultActionC2SPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(VaultActionC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.distanceToSqr(packet.pos.getX() + 0.5, packet.pos.getY() + 0.5,
                    packet.pos.getZ() + 0.5) > 64) return;
            if (!(player.serverLevel().getBlockEntity(packet.pos) instanceof ZooVaultBlockEntity vault)) return;

            switch (packet.action) {
                case 0 -> { // tout deposer
                    int moved = 0;
                    for (ItemStack s : player.getInventory().items) {
                        if (s.is(com.lex3d.ultimatezootaming.core.init.ModItems.PARK_TICKET.get())) {
                            moved += s.getCount();
                            s.setCount(0);
                        }
                    }
                    if (moved > 0) vault.deposit(moved);
                }
                case 1, 2 -> {
                    int want = packet.action == 1 ? 16 : 64;
                    // Prendre sur la TRESORERIE TOTALE (repartie sur tous les coffres)
                    int available = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.totalBalance(player.serverLevel());
                    int take = Math.min(want, available);
                    if (take > 0 && com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.withdrawFromTreasury(player.serverLevel(), take)) {
                        player.getInventory().placeItemBackInInventory(new ItemStack(com.lex3d.ultimatezootaming.core.init.ModItems.PARK_TICKET.get(), take));
                    }
                }
            }
            // Rafraichir l'ecran
            NetworkHandler.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new OpenVaultS2CPacket(packet.pos, vault.getBalance(),
                            com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.totalBalance(player.serverLevel())));
        });
        ctx.setPacketHandled(true);
    }
}
