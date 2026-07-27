package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Le serveur renvoie la fiche des animaux d'un enclos. */
public class SyncAnimalsS2CPacket {

    public record AnimalInfo(int entityId, String name, String species,
                             int welfare, boolean sick, boolean severe, boolean baby, String trait,
                             int space, int habitat, int food, int company, int health,
                             // --- Fiche approfondie (Vague 3) ---
                             String customName, int captureDay, int healCount, int babyCount,
                             int rarity, int generation, String bestFriendName) {}

    private final UUID zoneId;
    private final String zoneName;
    private final List<AnimalInfo> animals;

    public SyncAnimalsS2CPacket(UUID zoneId, String zoneName, List<AnimalInfo> animals) {
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.animals = animals;
    }

    public static void encode(SyncAnimalsS2CPacket p, FriendlyByteBuf buf) {
        buf.writeUUID(p.zoneId);
        buf.writeUtf(p.zoneName);
        buf.writeInt(p.animals.size());
        for (AnimalInfo a : p.animals) {
            buf.writeInt(a.entityId());
            buf.writeUtf(a.name());
            buf.writeUtf(a.species());
            buf.writeInt(a.welfare());
            buf.writeBoolean(a.sick());
            buf.writeBoolean(a.severe());
            buf.writeBoolean(a.baby());
            buf.writeUtf(a.trait());
            buf.writeInt(a.space());
            buf.writeInt(a.habitat());
            buf.writeInt(a.food());
            buf.writeInt(a.company());
            buf.writeInt(a.health());
            buf.writeUtf(a.customName());
            buf.writeInt(a.captureDay());
            buf.writeInt(a.healCount());
            buf.writeInt(a.babyCount());
            buf.writeInt(a.rarity());
            buf.writeInt(a.generation());
            buf.writeUtf(a.bestFriendName());
        }
    }

    public static SyncAnimalsS2CPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String name = buf.readUtf();
        int n = buf.readInt();
        List<AnimalInfo> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new AnimalInfo(buf.readInt(), buf.readUtf(), buf.readUtf(),
                    buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(),
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readInt(), buf.readInt(), buf.readUtf()));
        }
        return new SyncAnimalsS2CPacket(id, name, list);
    }

    public static void handle(SyncAnimalsS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.lex3d.ultimatezootaming.client.ClientSetup.openAnimalScreen(
                                packet.zoneId, packet.zoneName, packet.animals)));
        ctx.setPacketHandled(true);
    }

    /**
     * Construit la fiche complete d'un animal depuis son entite et sa capability.
     * Centralise la logique pour que les 4 points d'entree (tablette, clic direct,
     * liste d'enclos, ouverture de carte) renvoient exactement les memes donnees.
     */
    public static AnimalInfo describe(net.minecraft.world.entity.animal.Animal a,
                                      com.lex3d.ultimatezootaming.capability.TamingData d,
                                      net.minecraft.server.level.ServerLevel level) {
        String trait = d.getTrait() == null ? "" : d.getTrait().name();
        int[] bd = d.getWelfareBreakdown();
        // Meilleur ami : on cherche l'entite correspondante pour afficher son nom
        String friend = "";
        if (d.getBestFriend() != null && level != null) {
            var e = level.getEntity(d.getBestFriend());
            if (e != null) friend = e.getName().getString();
        }
        return new AnimalInfo(
                a.getId(),
                a.getName().getString(),
                net.minecraft.network.chat.Component
                        .translatable(a.getType().getDescriptionId()).getString(),
                d.getSatisfaction(), d.isSick(), d.isSevereSick(), a.isBaby(), trait,
                bd[0], bd[1], bd[2], bd[3], bd[4],
                d.getCustomName(), d.getCaptureDay(), d.getHealCount(), d.getBabyCount(),
                d.getRarity(), d.getGeneration(), friend);
    }
}
