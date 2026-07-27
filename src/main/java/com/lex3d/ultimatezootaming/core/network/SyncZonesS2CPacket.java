package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Envoie au client la liste des enclos + la liste des soigneurs du joueur, pour
 * afficher le GUI de gestion (renommage des enclos + affectation des soigneurs).
 */
public class SyncZonesS2CPacket {

    public record ZoneInfo(UUID id, String name, int size,
                            int animalCount, int speciesCount, int avgWelfare,
                            int sickCount, int feederCount, List<String> animalNames,
                            int zoneType, int avgSpace, int avgHabitat, int avgFood,
                            int avgCompany, int avgHealth) {}

    /** Un soigneur : id d'entite (pour le retrouver), nom affiche, id de l'enclos assigne (ou null). */
    public record KeeperInfo(int entityId, String name, List<UUID> assignedZones, int job,
                             String location, int level, int task) {
        /** Le premier enclos (compat avec l'ancien affichage). */
        public UUID assignedZone() { return assignedZones.isEmpty() ? null : assignedZones.get(0); }
        public boolean hasZone(UUID id) { return id != null && assignedZones.contains(id); }
    }

    /** Une caisse enregistreuse vue du GUI (lecture seule). */
    /** Un article en vente : son identifiant, la quantite en stock, son prix unitaire. */
    public record ShopItem(String itemId, int count, int price) {}

    public record ShopInfo(int x, int y, int z, int shopType, int stock, boolean vendor,
                           List<ShopItem> articles) {}

    /** Etat de direction du zoo (onglet Direction). */
    public record ZooStats(int ticketPolicy, int day, boolean open, int visitorsToday,
                           boolean hyped, int stars, int keeperCount, int salaryAmount,
                           int welfareAvg, int speciesBonus, int sickMalus, int sick,
                           int ticketPrice, int visitorsNow, int maxVisitors,
                           int dayTickets, int daySales, int daySalaries,
                           int ambiance, boolean escapeActive, List<String> opinions,
                           int[] goalProgress, boolean[] goalDone, int[] lastProfits,
                           boolean paused, List<String> trends, List<String> opinionHistory) {

        public void write(FriendlyByteBuf buf) {
            buf.writeInt(ticketPolicy); buf.writeInt(day); buf.writeBoolean(open);
            buf.writeInt(visitorsToday); buf.writeBoolean(hyped); buf.writeInt(stars);
            buf.writeInt(keeperCount); buf.writeInt(salaryAmount);
            buf.writeInt(welfareAvg); buf.writeInt(speciesBonus); buf.writeInt(sickMalus);
            buf.writeInt(sick); buf.writeInt(ticketPrice); buf.writeInt(visitorsNow);
            buf.writeInt(maxVisitors); buf.writeInt(dayTickets); buf.writeInt(daySales);
            buf.writeInt(daySalaries); buf.writeInt(ambiance); buf.writeBoolean(escapeActive);
            buf.writeInt(opinions.size());
            for (String o : opinions) buf.writeUtf(o);
            buf.writeVarIntArray(goalProgress);
            buf.writeInt(goalDone.length);
            for (boolean b : goalDone) buf.writeBoolean(b);
            buf.writeVarIntArray(lastProfits);
            buf.writeBoolean(paused);
            buf.writeInt(trends.size());
            for (String t : trends) buf.writeUtf(t);
            buf.writeInt(opinionHistory.size());
            for (String h : opinionHistory) buf.writeUtf(h);
        }

        public static ZooStats read(FriendlyByteBuf buf) {
            int tp = buf.readInt(), day = buf.readInt(); boolean open = buf.readBoolean();
            int vt = buf.readInt(); boolean hy = buf.readBoolean(); int st = buf.readInt();
            int kc = buf.readInt(), sa = buf.readInt();
            int wa = buf.readInt(), sb = buf.readInt(), sm = buf.readInt(), sick = buf.readInt();
            int tpr = buf.readInt(), vn = buf.readInt(), mv = buf.readInt();
            int dt = buf.readInt(), ds = buf.readInt(), dsa = buf.readInt();
            int amb = buf.readInt(); boolean esc = buf.readBoolean();
            int n = buf.readInt();
            List<String> ops = new ArrayList<>();
            for (int i = 0; i < n; i++) ops.add(buf.readUtf());
            int[] gp = buf.readVarIntArray();
            int gn = buf.readInt();
            boolean[] gd = new boolean[gn];
            for (int i = 0; i < gn; i++) gd[i] = buf.readBoolean();
            int[] lp = buf.readVarIntArray();
            boolean paused = buf.readBoolean();
            int tn = buf.readInt();
            List<String> trends = new ArrayList<>();
            for (int i = 0; i < tn; i++) trends.add(buf.readUtf());
            int hn = buf.readInt();
            List<String> opinionHistory = new ArrayList<>();
            for (int i = 0; i < hn; i++) opinionHistory.add(buf.readUtf());
            return new ZooStats(tp, day, open, vt, hy, st, kc, sa, wa, sb, sm, sick,
                    tpr, vn, mv, dt, ds, dsa, amb, esc, ops, gp, gd, lp, paused, trends, opinionHistory);
        }
    }

    private final List<ZoneInfo> zones;
    private final List<KeeperInfo> keepers;
    private final int zooScore;      // 0-100
    private final int globalSpecies; // especes distinctes dans tout le zoo
    private final int treasury;      // emeraudes dans les Caisses du Zoo
    private final List<ShopInfo> shops;
    private final ZooStats stats;
    /** Position de l'ordinateur si ouvert depuis lui (null = baton). */
    private final net.minecraft.core.BlockPos computerPos;

    public SyncZonesS2CPacket(List<ZoneInfo> zones, List<KeeperInfo> keepers,
                              int zooScore, int globalSpecies, int treasury,
                              List<ShopInfo> shops, ZooStats stats) {
        this(zones, keepers, zooScore, globalSpecies, treasury, shops, stats, null);
    }

    public SyncZonesS2CPacket(List<ZoneInfo> zones, List<KeeperInfo> keepers,
                              int zooScore, int globalSpecies, int treasury,
                              List<ShopInfo> shops, ZooStats stats,
                              net.minecraft.core.BlockPos computerPos) {
        this.zones = zones;
        this.keepers = keepers;
        this.zooScore = zooScore;
        this.globalSpecies = globalSpecies;
        this.treasury = treasury;
        this.shops = shops;
        this.stats = stats;
        this.computerPos = computerPos;
    }

    public static void encode(SyncZonesS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.zones.size());
        for (ZoneInfo z : packet.zones) {
            buf.writeUUID(z.id());
            buf.writeUtf(z.name());
            buf.writeInt(z.size());
            buf.writeInt(z.animalCount());
            buf.writeInt(z.speciesCount());
            buf.writeInt(z.avgWelfare());
            buf.writeInt(z.sickCount());
            buf.writeInt(z.feederCount());
            buf.writeInt(z.animalNames().size());
            for (String n2 : z.animalNames()) buf.writeUtf(n2);
            buf.writeInt(z.zoneType());
            buf.writeInt(z.avgSpace());
            buf.writeInt(z.avgHabitat());
            buf.writeInt(z.avgFood());
            buf.writeInt(z.avgCompany());
            buf.writeInt(z.avgHealth());
        }
        buf.writeInt(packet.zooScore);
        buf.writeInt(packet.globalSpecies);
        buf.writeInt(packet.treasury);
        packet.stats.write(buf);
        buf.writeInt(packet.shops.size());
        for (ShopInfo s : packet.shops) {
            buf.writeInt(s.x()); buf.writeInt(s.y()); buf.writeInt(s.z());
            buf.writeInt(s.shopType()); buf.writeInt(s.stock()); buf.writeBoolean(s.vendor());
            buf.writeInt(s.articles().size());
            for (ShopItem a : s.articles()) {
                buf.writeUtf(a.itemId());
                buf.writeInt(a.count());
                buf.writeInt(a.price());
            }
        }
        buf.writeInt(packet.keepers.size());
        for (KeeperInfo k : packet.keepers) {
            buf.writeInt(k.entityId());
            buf.writeUtf(k.name());
            buf.writeInt(k.job());
            buf.writeInt(k.assignedZones().size());
            for (UUID z : k.assignedZones()) buf.writeUUID(z);
            buf.writeUtf(k.location());
            buf.writeInt(k.level());
            buf.writeInt(k.task());
        }
        buf.writeBoolean(packet.computerPos != null);
        if (packet.computerPos != null) buf.writeBlockPos(packet.computerPos);
    }

    public static SyncZonesS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<ZoneInfo> zones = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            UUID zid = buf.readUUID(); String zname = buf.readUtf(); int zsize = buf.readInt();
            int ac = buf.readInt(), sc = buf.readInt(), aw = buf.readInt(), sk = buf.readInt(), fc = buf.readInt();
            int nn = buf.readInt();
            List<String> names = new ArrayList<>();
            for (int j = 0; j < nn; j++) names.add(buf.readUtf());
            int ztype = buf.readInt();
            zones.add(new ZoneInfo(zid, zname, zsize, ac, sc, aw, sk, fc, names, ztype,
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt()));
        }
        int zooScore = buf.readInt();
        int globalSpecies = buf.readInt();
        int treasury = buf.readInt();
        ZooStats stats = ZooStats.read(buf);
        int ns = buf.readInt();
        List<ShopInfo> shops = new ArrayList<>();
        for (int i = 0; i < ns; i++) {
            int sx = buf.readInt(), sy = buf.readInt(), sz = buf.readInt();
            int stype = buf.readInt(), sstock = buf.readInt();
            boolean svendor = buf.readBoolean();
            int na = buf.readInt();
            List<ShopItem> articles = new ArrayList<>();
            for (int a = 0; a < na; a++) {
                articles.add(new ShopItem(buf.readUtf(), buf.readInt(), buf.readInt()));
            }
            shops.add(new ShopInfo(sx, sy, sz, stype, sstock, svendor, articles));
        }
        int m = buf.readInt();
        List<KeeperInfo> keepers = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int id = buf.readInt();
            String name = buf.readUtf();
            int job = buf.readInt();
            int zn = buf.readInt();
            List<UUID> zoneList = new ArrayList<>();
            for (int z = 0; z < zn; z++) zoneList.add(buf.readUUID());
            String loc = buf.readUtf();
            int lvl = buf.readInt();
            int tsk = buf.readInt();
            keepers.add(new KeeperInfo(id, name, zoneList, job, loc, lvl, tsk));
        }
        net.minecraft.core.BlockPos computerPos = buf.readBoolean() ? buf.readBlockPos() : null;
        return new SyncZonesS2CPacket(zones, keepers, zooScore, globalSpecies, treasury, shops, stats, computerPos);
    }

    public static void handle(SyncZonesS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.openZoneScreen(
                        packet.zones, packet.keepers, packet.zooScore,
                        packet.globalSpecies, packet.treasury, packet.shops, packet.stats,
                        packet.computerPos)));
        ctx.setPacketHandled(true);
    }

    public List<ZoneInfo> getZones() { return zones; }
    public List<KeeperInfo> getKeepers() { return keepers; }
}
