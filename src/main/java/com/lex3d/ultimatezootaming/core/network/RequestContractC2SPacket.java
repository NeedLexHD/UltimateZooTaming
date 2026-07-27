package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.contracts.ZooContract;
import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Le client ouvre l'ecran des contrats : on lui envoie l'offre en cours et la
 * liste des animaux du zoo qui satisfont l'exigence.
 */
public class RequestContractC2SPacket {

    public RequestContractC2SPacket() {}
    public static void encode(RequestContractC2SPacket p, FriendlyByteBuf buf) {}
    public static RequestContractC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestContractC2SPacket();
    }

    public static void handle(RequestContractC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            ZooContract c = ZooLedger.get(level).getContract();
            List<SyncContractS2CPacket.Candidate> candidates = new ArrayList<>();
            if (c != null && c.isActive()) {
                candidates.addAll(findCandidates(level, c));
            }
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncContractS2CPacket(c, candidates));
        });
        ctx.setPacketHandled(true);
    }

    /** Les animaux du zoo qui remplissent les conditions du contrat. */
    public static List<SyncContractS2CPacket.Candidate> findCandidates(ServerLevel level, ZooContract c) {
        List<SyncContractS2CPacket.Candidate> out = new ArrayList<>();
        for (var zone : ZooSavedData.get(level).getAllZones()) {
            if (!zone.isAnimalZone()) continue;
            for (Animal a : level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                    an -> zone.contains(an.blockPosition()))) {
                if (!matches(level, a, c)) continue;
                var d = a.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
                out.add(new SyncContractS2CPacket.Candidate(
                        a.getId(), a.getName().getString(),
                        d != null ? d.getSatisfaction() : 0,
                        d != null ? d.getRarity() : 0,
                        a.isBaby()));
                if (out.size() >= 20) return out; // securite paquet
            }
        }
        return out;
    }

    /** Cet animal satisfait-il l'exigence du contrat ? */
    public static boolean matches(ServerLevel level, Animal a, ZooContract c) {
        var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(a.getType());
        if (id == null || !id.toString().equals(c.species)) return false;
        var d = a.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (d == null || !d.isTamed()) return false;
        return switch (c.requirement) {
            case ANY -> true;
            case BABY -> a.isBaby();
            case HEALTHY -> !a.isBaby() && d.getSatisfaction() >= 80 && !d.isSick();
            case RARE -> d.getRarity() >= 1;
        };
    }
}
