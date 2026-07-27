package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Le client SUPPRIME un enclos depuis le GUI (bouton Supprimer). Les animaux
 *  qui y etaient assignes sont liberes (zoneId remis a null), pas tues. */
public class DeleteZoneC2SPacket {

    private final UUID zoneId;

    public DeleteZoneC2SPacket(UUID zoneId) {
        this.zoneId = zoneId;
    }

    public static void encode(DeleteZoneC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.zoneId);
    }

    public static DeleteZoneC2SPacket decode(FriendlyByteBuf buf) {
        return new DeleteZoneC2SPacket(buf.readUUID());
    }

    public static void handle(DeleteZoneC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            ZooZone zone = ZooSavedData.get(level).getZone(packet.zoneId);
            if (zone == null) return;

            // Liberer tous les animaux assignes a cet enclos (dans la zone chargee)
            for (Animal a : level.getEntitiesOfClass(Animal.class,
                    zone.boundingBox().inflate(8))) {
                a.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(d -> {
                    if (packet.zoneId.equals(d.getZoneId())) d.setZoneId(null);
                });
            }

            // Retire cet enclos de la charge de tous les employes qui l'avaient
            // (sinon ils gardent une reference morte et cherchent un enclos disparu).
            for (var k : level.getEntitiesOfClass(
                    com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                    new net.minecraft.world.phys.AABB(-30000000, -64, -30000000,
                            30000000, 320, 30000000))) {
                k.forgetZone(packet.zoneId);
            }

            ZooSavedData.get(level).removeZone(packet.zoneId);
            ZooSavedData.get(level).markChanged();
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.zone_deleted", zone.getName()), true);
        });
        ctx.setPacketHandled(true);
    }
}
