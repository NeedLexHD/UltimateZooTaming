package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.progression.KeeperSkill;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Le joueur investit un point dans une competence (ou remet tout a zero). */
public class UpgradeSkillC2SPacket {

    /** Index de la competence, ou -1 pour tout reinitialiser. */
    private final int entityId, skillIndex;

    public UpgradeSkillC2SPacket(int entityId, int skillIndex) {
        this.entityId = entityId;
        this.skillIndex = skillIndex;
    }

    public static void encode(UpgradeSkillC2SPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.entityId); buf.writeInt(p.skillIndex);
    }
    public static UpgradeSkillC2SPacket decode(FriendlyByteBuf buf) {
        return new UpgradeSkillC2SPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(UpgradeSkillC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.serverLevel().getEntity(packet.entityId) instanceof ZooKeeperEntity k)) return;

            if (packet.skillIndex == -1) {
                k.resetSkills();
            } else {
                var all = KeeperSkill.values();
                if (packet.skillIndex < 0 || packet.skillIndex >= all.length) return;
                if (!k.upgradeSkill(all[packet.skillIndex])) return; // pas de point libre ou rang max
                player.serverLevel().playSound(null, k.blockPosition(),
                        net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.4f, 1.8f);
            }
            // Renvoie la fiche a jour
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncSkillsS2CPacket(k.getId(), k.getName().getString(), k.getJob(),
                            k.getKeeperLevel(), k.getXp(), k.getFreePoints(), k.getSkillRanks()));
        });
        ctx.setPacketHandled(true);
    }
}
