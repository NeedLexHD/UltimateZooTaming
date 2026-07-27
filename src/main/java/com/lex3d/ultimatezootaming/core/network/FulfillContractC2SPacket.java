package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Le joueur livre un animal pour honorer le contrat : l'animal part vers le zoo
 * demandeur (il disparait dans une gerbe de particules) et la recompense est
 * versee a la Tresorerie.
 */
public class FulfillContractC2SPacket {

    private final int entityId;

    public FulfillContractC2SPacket(int entityId) { this.entityId = entityId; }

    public static void encode(FulfillContractC2SPacket p, FriendlyByteBuf buf) { buf.writeInt(p.entityId); }
    public static FulfillContractC2SPacket decode(FriendlyByteBuf buf) {
        return new FulfillContractC2SPacket(buf.readInt());
    }

    public static void handle(FulfillContractC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            var ledger = ZooLedger.get(level);
            var contract = ledger.getContract();
            if (contract == null || !contract.isActive()) return;

            if (!(level.getEntity(packet.entityId) instanceof Animal animal)) return;
            // On revalide cote serveur : le client ne decide pas de l'eligibilite
            if (!RequestContractC2SPacket.matches(level, animal, contract)) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.contract_invalid")
                        .withStyle(ChatFormatting.RED), true);
                return;
            }

            // Depart de l'animal
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    animal.getX(), animal.getY() + 0.8, animal.getZ(), 30, 0.4, 0.6, 0.4, 0.1);
            level.playSound(null, animal.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.7f, 1.1f);
            animal.discard();

            // Recompense
            var vault = ZooVaultBlock.anyVault(level);
            if (vault != null) vault.deposit(contract.reward);
            ledger.addPrestige(contract.prestige); // le prestige amene du monde
            int reward = contract.reward;
            String client = contract.client;
            ledger.clearContract();

            for (var p : level.getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.literal("\u2709 ").withStyle(ChatFormatting.GREEN)
                        .append(Component.translatable(
                                "message.ultimatezootaming.contract_done", client, reward)
                                .withStyle(ChatFormatting.WHITE)));
            }
        });
        ctx.setPacketHandled(true);
    }
}
