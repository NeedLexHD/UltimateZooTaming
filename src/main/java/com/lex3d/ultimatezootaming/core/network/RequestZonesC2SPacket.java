package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Le client demande la liste de ses enclos (pour ouvrir le GUI de gestion).
 *  computerPos != null => ouvert depuis l'ordinateur du patron (onglets Direction/
 *  Avis/Recrutement) ; null => ouvert depuis le baton (Enclos/Employes/Boutiques). */
public class RequestZonesC2SPacket {

    private final net.minecraft.core.BlockPos computerPos;

    public RequestZonesC2SPacket() { this.computerPos = null; }
    public RequestZonesC2SPacket(net.minecraft.core.BlockPos computerPos) { this.computerPos = computerPos; }

    public static void encode(RequestZonesC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.computerPos != null);
        if (packet.computerPos != null) buf.writeBlockPos(packet.computerPos);
    }

    public static RequestZonesC2SPacket decode(FriendlyByteBuf buf) {
        if (buf.readBoolean()) return new RequestZonesC2SPacket(buf.readBlockPos());
        return new RequestZonesC2SPacket();
    }

    public static void handle(RequestZonesC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            sendTo(player, packet.computerPos);
        });
        ctx.setPacketHandled(true);
    }

    /** Construit et envoie le Sync au joueur. computerPos != null => mode ordinateur. */
    public static void sendTo(ServerPlayer player, net.minecraft.core.BlockPos computerPos) {
            List<SyncZonesS2CPacket.ZoneInfo> infos = new ArrayList<>();
            for (ZooZone zone : ZooSavedData.get(player.serverLevel()).getAllZones()) {
                infos.add(summarize(player.serverLevel(), zone));
            }
            // Collecte les soigneurs du joueur, charges dans le monde
            List<SyncZonesS2CPacket.KeeperInfo> keepers = new ArrayList<>();
            for (com.lex3d.ultimatezootaming.entities.ZooKeeperEntity keeper :
                    player.serverLevel().getEntitiesOfClass(
                            com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                            player.getBoundingBox().inflate(256))) {
                { // multi : tous les soigneurs du zoo sont geres en commun
                    // Localisation lisible : nom de l'enclos ou il se trouve MAINTENANT,
                    // "Dortoir" s'il dort, sinon "Sur le chemin".
                    String loc = describeLocation(player.serverLevel(), keeper);
                    keepers.add(new SyncZonesS2CPacket.KeeperInfo(
                            keeper.getId(), keeper.getName().getString(),
                            new java.util.ArrayList<>(keeper.getAssignedZones()), keeper.getJob(),
                            loc, keeper.getKeeperLevel(), keeper.getTask().ordinal()));
                }
            }
            // Note globale du zoo : moyenne ponderee du bien-etre + especes - malades
            int totalAnimals = 0, weighted = 0, totalSick = 0;
            java.util.Set<net.minecraft.world.entity.EntityType<?>> allSpecies = new java.util.HashSet<>();
            for (ZooZone zone : ZooSavedData.get(player.serverLevel()).getAllZones()) {
                if (!zone.isAnimalZone()) continue;
                for (net.minecraft.world.entity.animal.Animal a : player.serverLevel().getEntitiesOfClass(
                        net.minecraft.world.entity.animal.Animal.class, zone.boundingBox(),
                        an -> an.isAlive() && zone.contains(an.blockPosition())
                                && an.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                                    .resolve().map(com.lex3d.ultimatezootaming.capability.TamingData::isTamed).orElse(false))) {
                    allSpecies.add(a.getType());
                    totalAnimals++;
                    var d = a.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                            .resolve().orElse(null);
                    if (d != null) {
                        weighted += d.getSatisfaction();
                        if (d.isSick()) totalSick++;
                    }
                }
            }
            int avg = totalAnimals == 0 ? 0 : weighted / totalAnimals;
            int speciesBonus = Math.min(20, allSpecies.size());  // +1 par espece, max 20
            int sickMalus = Math.min(20, totalSick * 4);
            int zooScore = Math.max(0, Math.min(100, (int) (avg * 0.8) + speciesBonus - sickMalus));

            // Boutiques (lecture seule) : position, type, stock, vendeur present
            java.util.List<SyncZonesS2CPacket.ShopInfo> shops = new java.util.ArrayList<>();
            for (net.minecraft.core.BlockPos sp :
                    com.lex3d.ultimatezootaming.blocks.ShopBlock.allShops(player.serverLevel())) {
                if (!(player.serverLevel().getBlockEntity(sp)
                        instanceof com.lex3d.ultimatezootaming.blocks.ShopBlockEntity be)) continue;
                boolean vendor = !player.serverLevel().getEntitiesOfClass(
                        com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                        new net.minecraft.world.phys.AABB(sp).inflate(8),
                        kp -> kp.getJob() == 4 && !kp.isOnStrike()).isEmpty();
                // Detail des articles en vente : id, quantite, prix unitaire.
                // On agrege par item pour ne pas lister 9 fois le meme.
                var priceReg = com.lex3d.ultimatezootaming.saveddata.PriceRegistry
                        .get(player.serverLevel());
                java.util.LinkedHashMap<String, int[]> agg = new java.util.LinkedHashMap<>();
                for (int slot = 0; slot < be.getContainerSize(); slot++) {
                    var st = be.getItem(slot);
                    if (st.isEmpty()) continue;
                    var iid = net.minecraftforge.registries.ForgeRegistries.ITEMS
                            .getKey(st.getItem());
                    if (iid == null) continue;
                    int price = priceReg.priceOf(st);
                    agg.computeIfAbsent(iid.toString(), k -> new int[]{0, price})[0] += st.getCount();
                }
                java.util.List<SyncZonesS2CPacket.ShopItem> articles = new java.util.ArrayList<>();
                for (var e : agg.entrySet()) {
                    articles.add(new SyncZonesS2CPacket.ShopItem(
                            e.getKey(), e.getValue()[0], e.getValue()[1]));
                }
                shops.add(new SyncZonesS2CPacket.ShopInfo(sp.getX(), sp.getY(), sp.getZ(),
                        be.getShopTypeEnum().ordinal(), be.countStock(), vendor, articles));
                if (shops.size() >= 30) break;
            }
            var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(player.serverLevel());
            int keeperCount = keepers.size();
            int visitorsNow = player.serverLevel().getEntitiesOfClass(
                    com.lex3d.ultimatezootaming.entities.VisitorEntity.class,
                    new net.minecraft.world.phys.AABB(-30000000, -64, -30000000, 30000000, 320, 30000000)).size();
            int ticketPrice = Math.max(1, (int) Math.round(
                    com.lex3d.ultimatezootaming.welfare.ZooScore.ticketPrice(zooScore) * ledger.priceFactor()));
            SyncZonesS2CPacket.ZooStats stats = new SyncZonesS2CPacket.ZooStats(
                    ledger.getTicketPolicy(), ledger.getDay(), ledger.isOpen(),
                    ledger.getVisitorsToday(), ledger.isHyped(),
                    com.lex3d.ultimatezootaming.welfare.ZooScore.starCount(player.serverLevel()),
                    keeperCount,
                    com.lex3d.ultimatezootaming.config.ZooServerConfig.SALARY_AMOUNT.get(),
                    avg, speciesBonus, sickMalus, totalSick,
                    ticketPrice, visitorsNow,
                    com.lex3d.ultimatezootaming.config.ZooServerConfig.MAX_VISITORS.get(),
                    ledger.getTickets(), ledger.getSales(), ledger.getSalaries(),
                    (int) Math.round(com.lex3d.ultimatezootaming.welfare.AmbianceScore.zooAverage(player.serverLevel())),
                    com.lex3d.ultimatezootaming.events.EscapeHandler.anyEscapeActive(player.serverLevel()),
                    com.lex3d.ultimatezootaming.ai.VisitorOpinion.recent(),
                    com.lex3d.ultimatezootaming.events.ZooDayHandler.goalProgress(player.serverLevel(), ledger),
                    ledger.getMilestones(),
                    ledger.getLastProfits(),
                    ledger.isPaused(),
                    computeTrends(ledger, allSpecies),
                    encodeOpinionHistory());
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncZonesS2CPacket(infos, keepers, zooScore, allSpecies.size(),
                            com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.totalBalance(player.serverLevel()),
                            shops, stats, computerPos));
    }

    /** Calcule le resume d'un enclos (animaux, especes, bien-etre moyen, malades, mangeoires). */
    private static SyncZonesS2CPacket.ZoneInfo summarize(
            net.minecraft.server.level.ServerLevel level, ZooZone zone) {
        java.util.List<net.minecraft.world.entity.animal.Animal> animals =
                level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class, zone.boundingBox(),
                        a -> a.isAlive() && zone.contains(a.blockPosition())
                                && a.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                                    .resolve().map(com.lex3d.ultimatezootaming.capability.TamingData::isTamed).orElse(false));

        java.util.Set<net.minecraft.world.entity.EntityType<?>> species = new java.util.HashSet<>();
        java.util.List<String> names = new java.util.ArrayList<>();
        int welfareSum = 0, sick = 0;
        int[] bdSum = new int[5];
        for (net.minecraft.world.entity.animal.Animal a : animals) {
            species.add(a.getType());
            if (names.size() < 12) names.add(a.getName().getString());
            var data = a.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                    .resolve().orElse(null);
            if (data != null) {
                welfareSum += data.getSatisfaction();
                if (data.isSick()) sick++;
                int[] bd = data.getWelfareBreakdown();
                for (int i = 0; i < 5; i++) bdSum[i] += bd[i];
            }
        }
        int avg = animals.isEmpty() ? 0 : welfareSum / animals.size();
        int n = Math.max(1, animals.size());

        // Compte les mangeoires de l'enclos (scan du sol +/- 3, pas tout le volume)
        int feeders = 0;
        for (long packed : zone.floorColumns()) {
            net.minecraft.core.BlockPos floor = net.minecraft.core.BlockPos.of(packed);
            for (int dy = -2; dy <= 3; dy++) {
                if (level.getBlockEntity(floor.above(dy))
                        instanceof com.lex3d.ultimatezootaming.blocks.FeederBlockEntity) {
                    feeders++;
                }
            }
        }

        return new SyncZonesS2CPacket.ZoneInfo(zone.getId(), zone.getName(), zone.size(),
                animals.size(), species.size(), avg, sick, feeders, names, zone.getZoneType(),
                bdSum[0] / n, bdSum[1] / n, bdSum[2] / n, bdSum[3] / n, bdSum[4] / n);
    }

    /** Construit le Top 5 des especes RECLAMEES par les visiteurs et ABSENTES du zoo.
     *  Chaque entree = "nom_traduit|score" pour l'affichage GUI. */
    /** Encode l'historique note des avis en "key|stars" pour l'onglet Avis. */
    private static java.util.List<String> encodeOpinionHistory() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (var o : com.lex3d.ultimatezootaming.ai.VisitorOpinion.history()) {
            out.add(o.key() + "|" + o.stars());
        }
        return out;
    }

    private static java.util.List<String> computeTrends(
            com.lex3d.ultimatezootaming.saveddata.ZooLedger ledger,
            java.util.Set<net.minecraft.world.entity.EntityType<?>> presentTypes) {
        java.util.Set<String> presentIds = new java.util.HashSet<>();
        for (var t : presentTypes) {
            var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(t);
            if (id != null) presentIds.add(id.toString());
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        for (var e : ledger.topDemands(presentIds, 5)) {
            var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                    .getValue(net.minecraft.resources.ResourceLocation.tryParse(e.getKey()));
            String name = type != null
                    ? net.minecraft.network.chat.Component.translatable(type.getDescriptionId()).getString()
                    : e.getKey();
            out.add(name + "|" + e.getValue());
        }
        return out;
    }

    /**
     * Ou se trouve l'employe en ce moment, en texte lisible pour le GUI :
     * le nom de l'enclos qui le contient, "Dortoir" s'il dort, ou "Sur le chemin".
     */
    private static String describeLocation(net.minecraft.server.level.ServerLevel level,
                                           com.lex3d.ultimatezootaming.entities.ZooKeeperEntity keeper) {
        if (keeper.isOnStrike()) return "greve";
        if (keeper.isSleeping()) return "dortoir";
        var pos = keeper.blockPosition();
        for (ZooZone z : ZooSavedData.get(level).getAllZones()) {
            if (!z.contains(pos)) continue;
            if (z.getZoneType() == 1) return "repos";
            return z.getName();
        }
        return "chemin";
    }
}