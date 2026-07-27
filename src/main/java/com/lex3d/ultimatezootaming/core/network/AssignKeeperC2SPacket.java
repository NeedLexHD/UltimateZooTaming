package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Le client (via le GUI) affecte un soigneur a un enclos. Le soigneur essaie de
 * s'y RENDRE A PIED ; s'il ne peut pas (trop loin, chemin introuvable), il est
 * teleporte a l'entree de l'enclos.
 */
public class AssignKeeperC2SPacket {

    private final int keeperId;
    private final UUID zoneId; // null = desassigner

    public AssignKeeperC2SPacket(int keeperId, UUID zoneId) {
        this.keeperId = keeperId;
        this.zoneId = zoneId;
    }

    public static void encode(AssignKeeperC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.keeperId);
        buf.writeBoolean(packet.zoneId != null);
        if (packet.zoneId != null) buf.writeUUID(packet.zoneId);
    }

    public static AssignKeeperC2SPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        UUID zone = buf.readBoolean() ? buf.readUUID() : null;
        return new AssignKeeperC2SPacket(id, zone);
    }

    public static void handle(AssignKeeperC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();

            Entity entity = level.getEntity(packet.keeperId);
            if (!(entity instanceof ZooKeeperEntity keeper)) return;
            // Securite : proprietaire uniquement (ou soigneur sans proprietaire)
            // multi : zoo gere en commun, pas de verrou proprietaire

            if (packet.zoneId == null) {
                // Retirer TOUTE l'affectation
                keeper.setAssignedZone(null);
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.keeper_unassigned"), true);
                return;
            }

            ZooZone zone = ZooSavedData.get(level).getZone(packet.zoneId);
            if (zone == null) return;

            // On n'affecte un employe qu'a un ENCLOS. Une salle de repos ou une
            // zone de vente n'est pas un poste de travail : sans ce garde-fou,
            // elle occupait une place dans sa charge et faussait le compteur.
            if (!zone.isAnimalZone()) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.keeper_not_enclosure")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }

            // MULTI-ENCLOS : le clic bascule cet enclos dans/hors de sa charge.
            boolean wasAssigned = keeper.hasZone(zone.getId());
            if (!keeper.toggleZone(zone.getId())) {
                // Refus : il a deja le maximum d'enclos
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.keeper_zone_limit",
                        ZooKeeperEntity.MAX_ZONES), true);
                return;
            }
            keeper.setOwnerUUID(player.getUUID());

            if (wasAssigned) {
                // On vient de lui RETIRER cet enclos : pas de deplacement a faire
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.keeper_zone_removed",
                        zone.getName(), keeper.getZoneCount()), true);
                return;
            }

            // Point d'arrivee : la case de sol de l'enclos la plus proche du soigneur
            BlockPos dest = zone.nearestFloorPos(keeper.blockPosition());
            if (dest == null) return;

            boolean sameDimension = true; // (mono-dimension ici, garde la logique simple)
            double distSq = keeper.blockPosition().distSqr(dest);

            // Tente d'y aller a pied si c'est raisonnablement proche et accessible
            boolean walking = false;
            if (sameDimension && distSq <= 48 * 48) {
                walking = keeper.getNavigation().moveTo(
                        dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 0.8);
            }

            if (walking) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.keeper_walking", zone.getName()), true);
            } else {
                // Trop loin ou chemin introuvable : teleportation de secours
                keeper.moveTo(dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5,
                        keeper.getYRot(), keeper.getXRot());
                keeper.getNavigation().stop();
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 20, 0.4, 0.6, 0.4, 0.1);
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.keeper_teleported", zone.getName()), true);
            }
        });
        ctx.setPacketHandled(true);
    }
}
