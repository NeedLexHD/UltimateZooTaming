package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.AssignKeeperC2SPacket;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.RenameZoneC2SPacket;
import com.lex3d.ultimatezootaming.core.network.SyncZonesS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tableau de bord du zoo, look "tycoon" : fond sombre, header avec la note
 * globale + rang, liste des enclos avec barres de bien-etre colorees et
 * alertes, panneau de details + affectation des soigneurs.
 */
public class ZoneManagerScreen extends Screen {

    private static final int PANEL_W = 460;
    private static final int PANEL_H = 270;
    private static final int ROW_H = 24;
    private static final int VISIBLE = 5;

    // Palette sombre "pro"
    private static final int BG = 0xF01A1B22;
    private static final int PANEL = 0xFF23242E;
    private static final int PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A;
    private static final int TEXT = 0xFFE8E8F0;
    private static final int TEXT_DIM = 0xFF9A9CB0;
    private static final int GOLD = 0xFFE0B94F;
    private static final int RED = 0xFFE05555;
    private static final int YELLOW = 0xFFE0B94F;

    private final List<SyncZonesS2CPacket.ZoneInfo> zones;
    private final List<SyncZonesS2CPacket.KeeperInfo> keepers;
    private final int zooScore;
    private final int globalSpecies;
    private final int treasury;
    private final List<SyncZonesS2CPacket.ShopInfo> shops;
    private final SyncZonesS2CPacket.ZooStats stats;
    /** Boutique selectionnee dans l'onglet Boutiques (-1 = aucune). */
    private int selectedShop = -1;
    private int shopScroll = 0;
    private int reviewScroll = 0;
    private int localPolicy;

    private int leftPos, topPos;
    private int selectedZone = 0;
    private UUID deleteConfirmId = null; // zone en attente de confirmation de suppression
    private boolean resetConfirm = false; // confirmation du reset zoo
    /** Mode ordinateur du patron : onglets Direction/Avis/Recrutement.
     *  Sinon (baton) : onglets Enclos/Employes/Boutiques. */
    private final boolean computerMode;
    /** Onglet courant (indices differents selon le mode). */
    private int tab = 0;
    private int zoneScroll = 0, keeperScroll = 0;
    private EditBox renameBox;

    public ZoneManagerScreen(List<SyncZonesS2CPacket.ZoneInfo> zones,
                             List<SyncZonesS2CPacket.KeeperInfo> keepers,
                             int zooScore, int globalSpecies, int treasury,
                             List<SyncZonesS2CPacket.ShopInfo> shops,
                             SyncZonesS2CPacket.ZooStats stats) {
        this(zones, keepers, zooScore, globalSpecies, treasury, shops, stats, false);
    }

    public ZoneManagerScreen(List<SyncZonesS2CPacket.ZoneInfo> zones,
                             List<SyncZonesS2CPacket.KeeperInfo> keepers,
                             int zooScore, int globalSpecies, int treasury,
                             List<SyncZonesS2CPacket.ShopInfo> shops,
                             SyncZonesS2CPacket.ZooStats stats, boolean computerMode) {
        super(Component.translatable("gui.ultimatezootaming.zones.title"));
        this.zones = new ArrayList<>(zones);
        this.keepers = new ArrayList<>(keepers);
        this.zooScore = zooScore;
        this.globalSpecies = globalSpecies;
        this.treasury = treasury;
        this.shops = new ArrayList<>(shops);
        this.stats = stats;
        this.localPolicy = stats.ticketPolicy();
        this.computerMode = computerMode;
        // L'ordinateur s'ouvre sur Direction ; le baton sur Enclos.
        this.tab = computerMode ? TAB_DIRECTION : TAB_ENCLOS;
    }

    // Indices d'onglets (constants, independants du mode d'affichage)
    private static final int TAB_ENCLOS = 0, TAB_STAFF = 1, TAB_SHOPS = 2;
    private static final int TAB_DIRECTION = 3, TAB_REVIEWS = 4, TAB_RECRUIT = 5;

    /** Position de l'ordinateur (mode ordinateur uniquement, pour l'embauche). */
    private net.minecraft.core.BlockPos computerPos = null;
    public void setComputerPos(net.minecraft.core.BlockPos p) { this.computerPos = p; }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - PANEL_W) / 2;
        topPos = (height - PANEL_H) / 2;
        rebuild();
    }

    /** Les zones de l'onglet courant. */
    private List<SyncZonesS2CPacket.ZoneInfo> shown() {
        List<SyncZonesS2CPacket.ZoneInfo> out = new ArrayList<>();
        for (SyncZonesS2CPacket.ZoneInfo z : zones) {
            boolean staff = z.zoneType() != 0;
            if ((tab == TAB_STAFF) == staff) out.add(z);
        }
        return out;
    }

    private SyncZonesS2CPacket.ZoneInfo selected() {
        List<SyncZonesS2CPacket.ZoneInfo> list = shown();
        if (list.isEmpty()) return null;
        if (selectedZone >= list.size()) selectedZone = 0;
        return list.get(selectedZone);
    }

    private UUID selectedZoneId() {
        SyncZonesS2CPacket.ZoneInfo z = selected();
        return z == null ? null : z.id();
    }

    /** Redemande les donnees a jour au serveur. Le serveur repond via un
     *  SyncZonesS2CPacket qui recree/rafraichit ce Screen avec les nouvelles
     *  donnees. A appeler apres chaque action qui modifie l'etat (affectation
     *  d'employe, changement de metier, etc.). */
    private void refreshFromServer() {
        NetworkHandler.CHANNEL.sendToServer(
                new com.lex3d.ultimatezootaming.core.network.RequestZonesC2SPacket(computerPos));
    }

    private void rebuild() {
        clearWidgets();
        int leftX = leftPos + 10;
        int rightX = leftPos + 226;
        int listY = topPos + 74;
        List<SyncZonesS2CPacket.ZoneInfo> list = shown();

        // ---- Onglets (3 par mode) ----
        // Baton : Enclos / Employes / Boutiques. Ordinateur : Direction / Avis / Recrutement.
        int[] modeTabs = computerMode
                ? new int[]{TAB_DIRECTION, TAB_REVIEWS, TAB_RECRUIT}
                : new int[]{TAB_ENCLOS, TAB_STAFF, TAB_SHOPS};
        String[] modeKeys = computerMode
                ? new String[]{"direction", "reviews", "recruit"}
                : new String[]{"animals", "staff", "shops"};
        for (int t = 0; t < 3; t++) {
            final int target = modeTabs[t];
            Button tb = Button.builder(Component.translatable("gui.ultimatezootaming.tab." + modeKeys[t]),
                            b -> { tab = target; selectedZone = 0; zoneScroll = 0; shopScroll = 0; rebuild(); })
                    .bounds(leftPos + 12 + t * 148, topPos + 42, 144, 14).build();
            tb.active = tab != target;
            addRenderableWidget(tb);
        }
        // Onglets speciaux : ils construisent leur propre contenu
        if (tab == TAB_SHOPS) { rebuildShops(); return; }
        if (tab == TAB_DIRECTION) { rebuildDirection(); return; }
        if (tab == TAB_REVIEWS) { rebuildReviews(); return; }
        if (tab == TAB_RECRUIT) { rebuildRecruit(); return; }

        // ---- Zones (gauche) : lignes cliquables ----
        int zoneMax = Math.max(0, list.size() - VISIBLE);
        zoneScroll = Math.max(0, Math.min(zoneScroll, zoneMax));
        for (int row = 0; row < VISIBLE; row++) {
            int idx = zoneScroll + row;
            if (idx >= list.size()) break;
            final int fi = idx;
            Button b = Button.builder(Component.empty(), btn -> { selectedZone = fi; keeperScroll = 0; rebuild(); })
                    .bounds(leftX, listY + row * ROW_H, 168, ROW_H - 2).build();
            b.setAlpha(0.0f);
            addRenderableWidget(b);
        }
        if (list.size() > VISIBLE) {
            addScroll(leftX + 170, listY, () -> { zoneScroll--; rebuild(); },
                    () -> { zoneScroll++; rebuild(); }, zoneScroll > 0, zoneScroll < zoneMax);
        }

        // Renommage + changement de type de la zone selectionnee
        SyncZonesS2CPacket.ZoneInfo sel = selected();
        if (sel != null) {
            renameBox = new EditBox(font, leftX, topPos + PANEL_H - 30, 118, 16, Component.empty());
            renameBox.setMaxLength(32);
            renameBox.setValue(sel.name());
            addRenderableWidget(renameBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.save"),
                            b -> saveRename())
                    .bounds(leftX + 122, topPos + PANEL_H - 31, 46, 18).build());
            // Fiche des animaux (enclos uniquement)
            if (sel.zoneType() == 0) {
                addRenderableWidget(Button.builder(
                                Component.translatable("gui.ultimatezootaming.zones.view_animals",
                                        sel.animalCount()),
                                b -> NetworkHandler.CHANNEL.sendToServer(
                                        new com.lex3d.ultimatezootaming.core.network.RequestAnimalsC2SPacket(sel.id())))
                        .bounds(rightX, topPos + PANEL_H - 26, 140, 16).build());
            }
            // Cycle de type : Enclos -> Repos -> Vente -> Stockage
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.ultimatezootaming.zonetype."
                                    + typeKey(sel.zoneType())),
                            b -> cycleType())
                    .bounds(leftX, topPos + PANEL_H - 50, 168, 16).build());
            // Supprimer l'enclos (confirmation en 2 clics)
            boolean confirming = sel.id().equals(deleteConfirmId);
            addRenderableWidget(Button.builder(
                            Component.translatable(confirming
                                    ? "gui.ultimatezootaming.zones.delete_confirm"
                                    : "gui.ultimatezootaming.zones.delete"),
                            b -> {
                                if (confirming) {
                                    NetworkHandler.CHANNEL.sendToServer(
                                            new com.lex3d.ultimatezootaming.core.network.DeleteZoneC2SPacket(sel.id()));
                                    zones.removeIf(z -> z.id().equals(sel.id()));
                                    deleteConfirmId = null;
                                    selectedZone = 0;
                                    rebuild();
                                } else {
                                    deleteConfirmId = sel.id();
                                    rebuild();
                                }
                            })
                    .bounds(leftX, topPos + PANEL_H - 70, 168, 16).build());
        }

        // ---- Soigneurs (droite) : seulement dans l'onglet ENCLOS (tab 0), pour
        // voir qui est affecte a l'enclos. Pas dans "Zones employes".
        if (tab == TAB_ENCLOS) {
        UUID zoneId = selectedZoneId();
        int keeperMax = Math.max(0, keepers.size() - VISIBLE);
        keeperScroll = Math.max(0, Math.min(keeperScroll, keeperMax));
        for (int row = 0; row < VISIBLE; row++) {
            int idx = keeperScroll + row;
            if (idx >= keepers.size()) break;
            SyncZonesS2CPacket.KeeperInfo k = keepers.get(idx);
            final int fi = idx;
            // MULTI-ENCLOS : coche si cet enclos fait partie de sa charge.
            boolean here = zoneId != null && k.hasZone(zoneId);
            int nz = countEnclosures(k);
            boolean elsewhere = nz > 0 && !here;
            String prefix = here ? "\u2714 " : elsewhere ? "\u25CB " : "   ";
            // Suffixe : nombre d'enclos pris en charge (ex " 2/3")
            String suffix = nz > 0 ? "  " + nz + "/" + ZooKeeperEntity.MAX_ZONES : "";
            String jobKey = ZooKeeperEntity.jobKey(k.job());
            int rowY = listY + row * (ROW_H - 4);
            // Bouton NOM : clic = affecter a l'enclos selectionne (ou retirer si deja ici)
            String line = prefix + k.name() + suffix;
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.literal(line), btn -> toggleKeeper(fi))
                    .bounds(rightX, rowY, 96, 18).build())
                    // Affectation possible seulement sur un ENCLOS : une salle de
                    // repos ou une zone de vente n'est pas un poste de travail.
                    .active = zoneId != null && selectedIsEnclosure();
            // Bouton METIER : clic = cycle le metier (cote serveur + maj locale immediate)
            addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                            Component.translatable("jobshort.ultimatezootaming." + jobKey),
                            btn -> cycleJob(fi))
                    .bounds(rightX + 98, rowY, 44, 18).build());
        }
        if (keepers.size() > VISIBLE) {
            addScroll(rightX + 142, listY, () -> { keeperScroll--; rebuild(); },
                    () -> { keeperScroll++; rebuild(); }, keeperScroll > 0, keeperScroll < keeperMax);
        }
        } // fin if (tab == TAB_ENCLOS) : liste des soigneurs

        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + PANEL_W - 66, topPos + PANEL_H - 31, 56, 18).build());
    }

    /** Onglet Boutiques : lecture seule, scroll. */
    private void rebuildShops() {
        int max = Math.max(0, shops.size() - 6);
        shopScroll = Math.max(0, Math.min(shopScroll, max));
        if (shops.size() > 6) {
            addScroll(leftPos + PANEL_W - 26, topPos + 62, () -> { shopScroll--; rebuild(); },
                    () -> { shopScroll++; rebuild(); }, shopScroll > 0, shopScroll < max);
        }

        // Boutons INVISIBLES sur chaque ligne : selectionner une boutique
        for (int row = 0; row < 6; row++) {
            int idx = shopScroll + row;
            if (idx >= shops.size()) break;
            final int fi = idx;
            int ly = topPos + 62 + row * 24;
            var pick = Button.builder(Component.literal(""), b -> {
                        selectedShop = (selectedShop == fi) ? -1 : fi;
                        rebuild();
                    })
                    .bounds(leftPos + 10, ly, PANEL_W - 44, 21).build();
            pick.setAlpha(0f); // transparent : c'est la ligne dessinee qui sert de visuel
            addRenderableWidget(pick);
        }

        // ---- Boutons de PRIX de la boutique selectionnee ----
        if (selectedShop >= 0 && selectedShop < shops.size()) {
            var sel = shops.get(selectedShop);
            int dy = topPos + 62 + 6 * 24 + 8 + 12;
            for (int i = 0; i < sel.articles().size() && i < 4; i++) {
                var a = sel.articles().get(i);
                final String itemId = a.itemId();
                final int shopType = sel.shopType();
                final int price = a.price();
                int ay2 = dy + i * 20;
                // Bouton MOINS
                addRenderableWidget(Button.builder(Component.literal("-"),
                                b -> setArticlePrice(itemId, Math.max(0, price - 1), shopType))
                        .bounds(leftPos + PANEL_W - 78, ay2, 16, 16).build());
                // Bouton PLUS
                addRenderableWidget(Button.builder(Component.literal("+"),
                                b -> setArticlePrice(itemId, Math.min(999, price + 1), shopType))
                        .bounds(leftPos + PANEL_W - 58, ay2, 16, 16).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + PANEL_W - 66, topPos + PANEL_H - 31, 56, 18).build());
    }

    /** Onglet Avis : historique note des 30 derniers avis, avec scroll. */
    private void rebuildReviews() {
        int total = stats == null ? 0 : stats.opinionHistory().size();
        int max = Math.max(0, total - REVIEW_VISIBLE);
        reviewScroll = Math.max(0, Math.min(reviewScroll, max));
        if (total > REVIEW_VISIBLE) {
            addScroll(leftPos + PANEL_W - 26, topPos + 62, () -> { reviewScroll--; rebuild(); },
                    () -> { reviewScroll++; rebuild(); }, reviewScroll > 0, reviewScroll < max);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + PANEL_W - 66, topPos + PANEL_H - 31, 56, 18).build());
    }
    /** Onglet Recrutement (ordinateur) : 3 candidats du jour + bouton embaucher. */
    private void rebuildRecruit() {
        var cands = generateCandidates();
        for (int i = 0; i < cands.size(); i++) {
            final int[] c = cands.get(i);
            int y = topPos + 66 + i * 50;
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.ultimatezootaming.recruit.hire", c[2]),
                            b -> {
                                if (computerPos != null) {
                                    NetworkHandler.CHANNEL.sendToServer(
                                        new com.lex3d.ultimatezootaming.core.network.HireCandidateC2SPacket(
                                            computerPos, RECRUIT_NAMES[c[3]], c[0], c[2]));
                                    onClose();
                                }
                            })
                    .bounds(leftPos + PANEL_W - 96, y + 14, 84, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + PANEL_W - 66, topPos + PANEL_H - 31, 56, 18).build());
    }

    private static final String[] RECRUIT_NAMES = {
            "Lucas", "Emma", "Hugo", "Lea", "Nathan", "Chloe", "Louis", "Manon",
            "Jules", "Sarah", "Tom", "Camille", "Enzo", "Ines", "Theo", "Jade"};

    /** Genere les 3 candidats du jour (meme logique que le serveur : seed = jour+pos). */
    private java.util.List<int[]> generateCandidates() {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        long seed = (long) stats.day() * 31 + (computerPos != null ? computerPos.hashCode() : 0);
        java.util.Random rng = new java.util.Random(seed);
        for (int i = 0; i < 3; i++) {
            int nameIdx = rng.nextInt(RECRUIT_NAMES.length);
            int job = rng.nextInt(5);
            int skill = 1 + rng.nextInt(5);
            int cost = 20 + skill * 10;
            out.add(new int[]{job, skill, cost, nameIdx});
        }
        return out;
    }


    private void rebuildDirection() {
        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.ultimatezootaming.direction.goals"),
                        b -> minecraft.setScreen(new GoalsScreen(this, stats)))
                .bounds(leftPos + 10, topPos + 86, 120, 18).build());
        // Bouton MISSIONS : ouvre les missions journalieres
        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.ultimatezootaming.direction.missions"),
                        b -> NetworkHandler.CHANNEL.sendToServer(
                                new com.lex3d.ultimatezootaming.core.network.RequestMissionsC2SPacket()))
                .bounds(leftPos + 136, topPos + 108, 120, 18).build());
        // Bouton PERSONNEL : registre des employes et leurs competences.
        // 3e colonne de la ligne du milieu : la case y=108 x=10 est deja prise
        // par le bouton Ouvrir/Fermer le zoo.
        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.ultimatezootaming.direction.staff"),
                        b -> minecraft.setScreen(new StaffListScreen(this, keepers)))
                .bounds(leftPos + 266, topPos + 86, 120, 18).build());
        // Bouton CONTRATS : demandes des zoos etrangers (4e ligne de la grille)
        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.ultimatezootaming.direction.contracts"),
                        b -> NetworkHandler.CHANNEL.sendToServer(
                                new com.lex3d.ultimatezootaming.core.network.RequestContractC2SPacket()))
                .bounds(leftPos + 10, topPos + 130, 120, 18).build());
        // Bouton MARKETING : ouvre les campagnes publicitaires
        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.ultimatezootaming.direction.marketing"),
                        b -> NetworkHandler.CHANNEL.sendToServer(
                                new com.lex3d.ultimatezootaming.core.network.RequestMarketingC2SPacket()))
                .bounds(leftPos + 266, topPos + 108, 120, 18).build());
        // Bouton RESET (test) : remet le zoo a zero, confirmation en 2 clics
        addRenderableWidget(Button.builder(Component.translatable(resetConfirm
                        ? "gui.ultimatezootaming.direction.reset_confirm"
                        : "gui.ultimatezootaming.direction.reset"),
                        b -> {
                            if (resetConfirm) {
                                NetworkHandler.CHANNEL.sendToServer(
                                        new com.lex3d.ultimatezootaming.core.network.ResetZooC2SPacket());
                                resetConfirm = false;
                                onClose();
                            } else {
                                resetConfirm = true;
                                rebuild();
                            }
                        })
                .bounds(leftPos + 136, topPos + 86, 130, 18).build());
        // Bouton FERMER / OUVRIR le zoo (pause manuelle, bascule cote serveur)
        addRenderableWidget(Button.builder(Component.translatable(
                        stats != null && stats.paused()
                                ? "gui.ultimatezootaming.direction.open"
                                : "gui.ultimatezootaming.direction.close"),
                        b -> {
                            NetworkHandler.CHANNEL.sendToServer(
                                    new com.lex3d.ultimatezootaming.core.network.PauseZooC2SPacket());
                            onClose();
                        })
                .bounds(leftPos + 10, topPos + 108, 120, 18).build());
        // Reglage des habitats par espece (necessite les droits OP cote serveur)
        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.ultimatezootaming.direction.habitats"),
                        b -> NetworkHandler.CHANNEL.sendToServer(
                                new com.lex3d.ultimatezootaming.core.network.RequestHabitatGuiC2SPacket()))
                .bounds(leftPos + 266, topPos + 64, 120, 18).build());
        addRenderableWidget(Button.builder(Component.translatable(
                        "gui.ultimatezootaming.direction.ticket",
                        Component.translatable("ticket.ultimatezootaming.p" + localPolicy)),
                        b -> {
                            localPolicy = (localPolicy + 1) % 3;
                            NetworkHandler.CHANNEL.sendToServer(
                                    new com.lex3d.ultimatezootaming.core.network.SetTicketPolicyC2SPacket());
                            rebuild();
                        })
                .bounds(leftPos + 10, topPos + 64, 250, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + PANEL_W - 66, topPos + PANEL_H - 31, 56, 18).build());
    }

    /** Onglet Boutiques : chaque caisse, son type, son stock, son vendeur. */
    private void renderShops(GuiGraphics g) {
        int y = topPos + 62;
        g.drawString(font, Component.translatable("gui.ultimatezootaming.shops.header", shops.size()),
                leftPos + 10, y - 14, TEXT_DIM, false);
        if (shops.isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.shops.none"),
                    leftPos + 10, y + 10, TEXT_DIM, false);
        }
        for (int row = 0; row < 6; row++) {
            int idx = shopScroll + row;
            if (idx >= shops.size()) break;
            SyncZonesS2CPacket.ShopInfo s = shops.get(idx);
            int ly = y + row * 24;
            g.fill(leftPos + 10, ly, leftPos + PANEL_W - 34, ly + 21, PANEL);
            String[] keys = {"souvenir", "meal", "icecream", "drink", "photo"};
            g.drawString(font, Component.translatable("shop.ultimatezootaming."
                    + keys[Math.floorMod(s.shopType(), 5)]), leftPos + 16, ly + 3, ACCENT, false);
            g.drawString(font, s.x() + ", " + s.y() + ", " + s.z(), leftPos + 90, ly + 3, TEXT_DIM, false);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.shops.stock", s.stock()),
                    leftPos + 16, ly + 12, s.stock() > 0 ? TEXT : RED, false);
            g.drawString(font, Component.translatable(s.vendor()
                            ? "gui.ultimatezootaming.shops.vendor_ok"
                            : "gui.ultimatezootaming.shops.vendor_none"),
                    leftPos + 90, ly + 12, s.vendor() ? ACCENT : YELLOW, false);
            // Marqueur de selection
            if (idx == selectedShop) {
                g.fill(leftPos + 10, ly, leftPos + 12, ly + 21, ACCENT);
            }
        }

        // ---- DETAIL DE LA BOUTIQUE SELECTIONNEE : articles et prix ----
        if (selectedShop >= 0 && selectedShop < shops.size()) {
            var sel = shops.get(selectedShop);
            int dy = topPos + 62 + 6 * 24 + 8;
            g.drawString(font, Component.translatable("gui.ultimatezootaming.shops.articles"),
                    leftPos + 10, dy, ACCENT, false);
            dy += 12;
            if (sel.articles().isEmpty()) {
                g.drawString(font, Component.translatable("gui.ultimatezootaming.shops.no_articles"),
                        leftPos + 10, dy, TEXT_DIM, false);
            }
            for (int i = 0; i < sel.articles().size() && i < 4; i++) {
                var a = sel.articles().get(i);
                int ay2 = dy + i * 20;
                g.fill(leftPos + 10, ay2, leftPos + PANEL_W - 34, ay2 + 18,
                        i % 2 == 0 ? PANEL : PANEL_LIGHT);
                // Icone de l'article
                var item = net.minecraftforge.registries.ForgeRegistries.ITEMS
                        .getValue(net.minecraft.resources.ResourceLocation.tryParse(a.itemId()));
                if (item != null) {
                    g.renderItem(new net.minecraft.world.item.ItemStack(item), leftPos + 14, ay2 + 1);
                    g.drawString(font, item.getDescription(), leftPos + 36, ay2 + 5, TEXT, false);
                }
                g.drawString(font, "x" + a.count(), leftPos + 190, ay2 + 5, TEXT_DIM, false);
                // Prix (rouge si non defini : l'article ne sera pas vendu)
                String pr = a.price() > 0 ? a.price() + " \u01B5"
                        : Component.translatable("gui.ultimatezootaming.shops.unpriced").getString();
                g.drawString(font, pr, leftPos + 226, ay2 + 5, a.price() > 0 ? GOLD : RED, false);
            }
        }
    }

    /** Onglet Direction : la note decomposee, l'affluence, le bilan du jour, les avis. */
    private void renderDirection(GuiGraphics g) {
        int lx = leftPos + 10, rx = leftPos + 226;
        int y = topPos + 154; // sous la 4e ligne de boutons (Contrats)
        // -- Colonne gauche : note + affluence --
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.score_detail"), lx, y, TEXT, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.welfare",
                stats.welfareAvg(), (int) (stats.welfareAvg() * 0.8)), lx, y + 12, TEXT_DIM, false);
        g.drawString(font, Component.literal("+ ").append(Component.translatable(
                "gui.ultimatezootaming.dir.species_bonus", stats.speciesBonus())), lx, y + 22, ACCENT, false);
        g.drawString(font, Component.literal("- ").append(Component.translatable(
                "gui.ultimatezootaming.dir.sick_malus", stats.sickMalus(), stats.sick())), lx, y + 32,
                stats.sickMalus() > 0 ? RED : TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.total", zooScore),
                lx, y + 44, welfareColor(zooScore, 1), false);

        int ay = y + 62;
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.crowd"), lx, ay, TEXT, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.visitors",
                stats.visitorsNow(), stats.maxVisitors()), lx, ay + 12, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.ticket_price",
                stats.ticketPrice()), lx, ay + 22, TEXT_DIM, false);
        int fy = ay + 34;
        if (stats.stars() > 0) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.stars", stats.stars()),
                    lx, fy, 0xFFE0B94F, false);
            fy += 10;
        }
        if (stats.hyped()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.hype"), lx, fy, 0xFFE0B94F, false);
            fy += 10;
        }
        if (stats.ambiance() > 0) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.ambiance",
                    stats.ambiance()), lx, fy, TEXT_DIM, false);
            fy += 10;
        }
        if (stats.escapeActive()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.escape"), lx, fy, RED, false);
            fy += 10;
        }

        // -- Colonne droite : jour + bilan + employes + avis --
        g.drawString(font, Component.translatable(stats.open()
                        ? "gui.ultimatezootaming.dir.open" : "gui.ultimatezootaming.dir.closed",
                stats.day()), rx, y, stats.open() ? ACCENT : TEXT_DIM, false);
        int profit = stats.dayTickets() + stats.daySales() - stats.daySalaries();
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.day_visitors",
                stats.visitorsToday()), rx, y + 12, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.day_tickets",
                stats.dayTickets()), rx, y + 22, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.day_sales",
                stats.daySales()), rx, y + 32, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.day_salaries",
                stats.daySalaries(), stats.keeperCount()), rx, y + 42, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.day_profit", profit),
                rx, y + 54, profit >= 0 ? ACCENT : RED, false);
        // Historique : les 7 derniers benefices en mini-barres
        int[] hist = stats.lastProfits();
        if (hist.length > 0) {
            int maxAbs = 1;
            for (int v : hist) maxAbs = Math.max(maxAbs, Math.abs(v));
            int gx = rx + 130, base = y + 60;
            for (int i = 0; i < hist.length; i++) {
                int h = Math.max(1, (int) (18.0 * Math.abs(hist[i]) / maxAbs));
                int x1 = gx + i * 7;
                if (hist[i] >= 0) g.fill(x1, base - h, x1 + 5, base, ACCENT);
                else g.fill(x1, base, x1 + 5, base + Math.min(h, 8), RED);
            }
        }

        int oy = y + 72;
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.opinions"), rx, oy, TEXT, false);
        if (stats.opinions().isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.no_opinion"),
                    rx, oy + 12, TEXT_DIM, false);
        }
        int ly2 = oy + 12;
        for (int i = 0; i < Math.min(5, stats.opinions().size()); i++) {
            var line = Component.literal("\u00ab ").append(
                    Component.translatable("opinion.ultimatezootaming." + stats.opinions().get(i)))
                    .append(" \u00bb");
            for (var seq : font.split(line, PANEL_W - 246)) {
                g.drawString(font, seq, rx, ly2, 0xFF8FB8D8, false);
                ly2 += 9;
                if (ly2 > topPos + PANEL_H - 40) break;
            }
            if (ly2 > topPos + PANEL_H - 40) break;
        }
    }

    private static final int REVIEW_VISIBLE = 9;

    /** Onglet Avis : liste notee (etoiles) des 30 derniers avis des visiteurs. */
    private void renderReviews(GuiGraphics g) {
        int lx = leftPos + 12;
        g.drawString(font, Component.translatable("gui.ultimatezootaming.reviews.title"),
                lx, topPos + 62, TEXT, false);
        if (stats == null || stats.opinionHistory().isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.reviews.empty"),
                    lx, topPos + 78, TEXT_DIM, false);
            return;
        }
        // Moyenne des notes (a droite du titre)
        var hist = stats.opinionHistory();
        int sum = 0, cnt = 0;
        for (String h : hist) {
            int bar = h.lastIndexOf('|');
            if (bar > 0) { try { sum += Integer.parseInt(h.substring(bar + 1)); cnt++; } catch (Exception ignored) {} }
        }
        if (cnt > 0) {
            int avgStars = Math.round(sum / (float) cnt);
            StringBuilder avgBar = new StringBuilder();
            for (int s = 0; s < 5; s++) avgBar.append(s < avgStars ? "\u2605" : "\u2606");
            g.drawString(font, Component.literal(avgBar.toString() + " (" + cnt + ")"),
                    lx + 175, topPos + 62, 0xFFE0B94F, false);
        }
        int y = topPos + 78;  // les avis commencent SOUS le titre
        for (int i = reviewScroll; i < Math.min(hist.size(), reviewScroll + REVIEW_VISIBLE); i++) {
            String h = hist.get(i);
            int bar = h.lastIndexOf('|');
            String key = bar > 0 ? h.substring(0, bar) : h;
            int st = 3;
            if (bar > 0) { try { st = Integer.parseInt(h.substring(bar + 1)); } catch (Exception ignored) {} }
            // Etoiles pleines/vides
            StringBuilder stars = new StringBuilder();
            for (int s = 0; s < 5; s++) stars.append(s < st ? "\u2605" : "\u2606");
            int col = st >= 4 ? ACCENT : st <= 2 ? 0xFFD06A6A : 0xFFE0B94F;
            g.drawString(font, Component.literal(stars.toString()), lx, y, col, false);
            // Texte de l'avis (tronque a la largeur dispo avant le Top 5)
            var txt = Component.translatable("opinion.ultimatezootaming." + key);
            var seqs = font.split(txt, 175);
            if (!seqs.isEmpty()) {
                g.drawString(font, seqs.get(0), lx + 58, y, TEXT_DIM, false);
            }
            y += 15;
        }

        // -- Colonne droite : Top 5 des especes reclamees par les visiteurs --
        int tx = leftPos + PANEL_W - 165;
        int ty = topPos + 78;
        g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.trends"), tx, ty, 0xFFE0B94F, false);
        ty += 12;
        if (stats.trends().isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.dir.no_trends"),
                    tx, ty, TEXT_DIM, false);
        } else {
            int rank = 1;
            for (String t : stats.trends()) {
                String[] parts = t.split("\\|");
                String name = parts.length > 0 ? parts[0] : t;
                g.drawString(font, Component.literal(rank + ". " + name), tx, ty, 0xFF8FB8D8, false);
                ty += 11;
                rank++;
            }
        }
    }


    /** Rendu de l'onglet Recrutement. */
    private void renderRecruit(GuiGraphics g) {
        int lx = leftPos + 12;
        g.drawString(font, Component.translatable("gui.ultimatezootaming.recruit.title"),
                lx, topPos + 62, TEXT, false);
        String[] jobKeys = {"generalist", "vet", "feeder", "guard", "vendor"};
        var cands = generateCandidates();
        for (int i = 0; i < cands.size(); i++) {
            int[] c = cands.get(i);
            int y = topPos + 66 + i * 50;
            g.fill(lx, y, leftPos + PANEL_W - 12, y + 44, PANEL);
            g.fill(lx, y, lx + 2, y + 44, ACCENT);
            g.drawString(font, RECRUIT_NAMES[c[3]], lx + 10, y + 6, TEXT, false);
            g.drawString(font, Component.translatable("job.ultimatezootaming." + jobKeys[c[0]]),
                    lx + 10, y + 19, ACCENT, false);
            StringBuilder stars = new StringBuilder();
            for (int s = 0; s < 5; s++) stars.append(s < c[1] ? "\u2605" : "\u2606");
            g.drawString(font, stars.toString(), lx + 10, y + 31, YELLOW, false);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.recruit.salary", c[2]),
                    lx + 140, y + 19, TEXT_DIM, false);
        }
    }


    private void cycleJob(int index) {
        SyncZonesS2CPacket.KeeperInfo k = keepers.get(index);
        int next = Math.floorMod(k.job() + 1, ZooKeeperEntity.JOB_COUNT);
        NetworkHandler.CHANNEL.sendToServer(
                new com.lex3d.ultimatezootaming.core.network.SetKeeperJobC2SPacket(k.entityId()));
        keepers.set(index, new SyncZonesS2CPacket.KeeperInfo(k.entityId(), k.name(), k.assignedZones(), next, k.location(), k.level(), k.task()));
        rebuild();
    }

    private void toggleKeeper(int index) {
        UUID zoneId = selectedZoneId();
        if (zoneId == null) return;
        SyncZonesS2CPacket.KeeperInfo k = keepers.get(index);
        // Le serveur fait un TOGGLE de cet enclos dans sa charge.
        List<UUID> updated = new ArrayList<>(k.assignedZones());
        if (updated.contains(zoneId)) {
            updated.remove(zoneId);
        } else {
            if (updated.size() >= ZooKeeperEntity.MAX_ZONES) return; // deja au maximum
            updated.add(zoneId);
        }
        NetworkHandler.CHANNEL.sendToServer(new AssignKeeperC2SPacket(k.entityId(), zoneId));
        keepers.set(index, new SyncZonesS2CPacket.KeeperInfo(
                k.entityId(), k.name(), updated, k.job(), k.location(), k.level(), k.task()));
        rebuild();
    }

    private void saveRename() {
        SyncZonesS2CPacket.ZoneInfo z = selected();
        if (renameBox == null || z == null) return;
        String newName = renameBox.getValue().trim();
        if (newName.isEmpty()) return;
        NetworkHandler.CHANNEL.sendToServer(new RenameZoneC2SPacket(z.id(), newName));
        replaceZone(z, new SyncZonesS2CPacket.ZoneInfo(z.id(), newName, z.size(),
                z.animalCount(), z.speciesCount(), z.avgWelfare(), z.sickCount(),
                z.feederCount(), z.animalNames(), z.zoneType(),
                z.avgSpace(), z.avgHabitat(), z.avgFood(), z.avgCompany(), z.avgHealth()));
        rebuild();
    }

    /** Change le type de la zone selectionnee (cycle 0-3). */
    private void cycleType() {
        SyncZonesS2CPacket.ZoneInfo z = selected();
        if (z == null) return;
        int next = (z.zoneType() + 1) % 3; // stockage retire : la Salle de repos sert aussi de stock
        NetworkHandler.CHANNEL.sendToServer(
                new com.lex3d.ultimatezootaming.core.network.SetZoneTypeC2SPacket(z.id(), next));
        replaceZone(z, new SyncZonesS2CPacket.ZoneInfo(z.id(), z.name(), z.size(),
                z.animalCount(), z.speciesCount(), z.avgWelfare(), z.sickCount(),
                z.feederCount(), z.animalNames(), next,
                z.avgSpace(), z.avgHabitat(), z.avgFood(), z.avgCompany(), z.avgHealth()));
        selectedZone = 0;
        rebuild();
    }

    private void replaceZone(SyncZonesS2CPacket.ZoneInfo oldZ, SyncZonesS2CPacket.ZoneInfo newZ) {
        int i = zones.indexOf(oldZ);
        if (i >= 0) zones.set(i, newZ);
    }

    private static String typeKey(int type) {
        return switch (type) {
            case 1 -> "rest";
            case 2 -> "sale";
            case 3 -> "storage";
            default -> "animal";
        };
    }

    private void addScroll(int x, int y, Runnable up, Runnable down, boolean upA, boolean downA) {
        Button u = Button.builder(Component.literal("\u25B2"), b -> up.run()).bounds(x, y, 12, 16).build();
        u.active = upA; addRenderableWidget(u);
        Button d = Button.builder(Component.literal("\u25BC"), b -> down.run())
                .bounds(x, y + (VISIBLE - 1) * ROW_H, 12, 16).build();
        d.active = downA; addRenderableWidget(d);
    }

    private String rankKey() {
        if (zooScore >= 90) return "world";
        if (zooScore >= 70) return "national";
        if (zooScore >= 50) return "regional";
        if (zooScore >= 25) return "local";
        return "refuge";
    }

    private int welfareColor(int w, int animals) {
        if (animals == 0) return TEXT_DIM;
        if (w >= 75) return ACCENT;
        if (w >= 40) return YELLOW;
        return RED;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);

        // Panneau principal
        g.fill(leftPos - 2, topPos - 2, leftPos + PANEL_W + 2, topPos + PANEL_H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + PANEL_W, topPos + PANEL_H, BG);

        // ---- HEADER : titre + note + rang ----
        g.fill(leftPos, topPos, leftPos + PANEL_W, topPos + 40, PANEL);
        g.fill(leftPos, topPos + 40, leftPos + PANEL_W, topPos + 41, ACCENT);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.tycoon.title"),
                leftPos + 10, topPos + 7, TEXT, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.tycoon.rank_" + rankKey()),
                leftPos + 10, topPos + 22, ACCENT, false);
        // NOTE EN ETOILES (precision 0.25) + valeur numerique
        double rating = com.lex3d.ultimatezootaming.client.gui.util.Stars.percentToRating(zooScore);
        String stars = com.lex3d.ultimatezootaming.client.gui.util.Stars.fromRating(rating);
        String ratingTxt = com.lex3d.ultimatezootaming.client.gui.util.Stars.ratingText(rating);
        String score = stars + " " + ratingTxt;
        g.drawString(font, score, leftPos + PANEL_W - 10 - font.width(score), topPos + 7, GOLD, false);
        int barX = leftPos + PANEL_W - 110, barY = topPos + 24, barW = 100;
        g.fill(barX, barY, barX + barW, barY + 8, PANEL_LIGHT);
        g.fill(barX, barY, barX + (int) (barW * (zooScore / 100.0)), barY + 8,
                welfareColor(zooScore, 1));
        // especes + tresorerie
        g.drawString(font, Component.translatable("gui.ultimatezootaming.tycoon.species", globalSpecies),
                leftPos + 118, topPos + 22, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.tycoon.treasury", treasury),
                leftPos + 118, topPos + 7, 0xFF6FE0A0, false);

        // Les onglets qui ont leur propre mise en page rendent AVANT les en-tetes
        // de colonnes : sinon "Enclos" et "Soigneurs" s'affichaient par-dessus
        // les ecrans Direction, Avis et Recrutement.
        if (tab == TAB_SHOPS) { renderShops(g); super.render(g, mx, my, pt); return; }
        if (tab == TAB_DIRECTION) { renderDirection(g); super.render(g, mx, my, pt); return; }
        if (tab == TAB_REVIEWS) { renderReviews(g); super.render(g, mx, my, pt); return; }
        if (tab == TAB_RECRUIT) { renderRecruit(g); super.render(g, mx, my, pt); return; }

        // ---- En-tetes de colonnes (onglets Enclos et Employes seulement) ----
        g.drawString(font, Component.translatable("gui.ultimatezootaming.zones.col_zones"),
                leftPos + 10, topPos + 60, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.zones.col_keepers"),
                leftPos + 196, topPos + 60, TEXT_DIM, false);

        // ---- Lignes de zones (dessinees, les boutons invisibles gerent le clic) ----
        int listY = topPos + 74;
        List<SyncZonesS2CPacket.ZoneInfo> list = shown();
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable(tab == TAB_STAFF
                            ? "gui.ultimatezootaming.zones.empty_staff"
                            : "gui.ultimatezootaming.zones.empty"),
                    leftPos + 10, topPos + 100, TEXT_DIM, false);
        }
        for (int row = 0; row < VISIBLE; row++) {
            int idx = zoneScroll + row;
            if (idx >= list.size()) break;
            SyncZonesS2CPacket.ZoneInfo z = list.get(idx);
            int y = listY + row * ROW_H;
            boolean sel = idx == selectedZone;
            g.fill(leftPos + 10, y, leftPos + 178, y + ROW_H - 3, sel ? PANEL_LIGHT : PANEL);
            if (sel) g.fill(leftPos + 10, y, leftPos + 12, y + ROW_H - 3, ACCENT);

            String name = z.name();
            if (z.zoneType() == 0 && (z.sickCount() > 0 || (z.animalCount() > 0 && z.avgWelfare() < 25))) {
                name = "\u26A0 " + name;
            }
            g.drawString(font, name, leftPos + 16, y + 3, z.sickCount() > 0 ? YELLOW : TEXT, false);
            if (z.zoneType() == 0) {
                int bw = 70, bx = leftPos + 16, by = y + 14;
                g.fill(bx, by, bx + bw, by + 5, PANEL_LIGHT);
                if (z.animalCount() > 0) {
                    g.fill(bx, by, bx + (int) (bw * (z.avgWelfare() / 100.0)), by + 5,
                            welfareColor(z.avgWelfare(), z.animalCount()));
                }
                g.drawString(font, z.animalCount() + "\u2665", leftPos + 92, y + 13, TEXT_DIM, false);
                g.drawString(font, "(" + countKeepersIn(z.id()) + "\u2691)", leftPos + 122, y + 13, TEXT_DIM, false);
            } else {
                g.drawString(font, Component.translatable("gui.ultimatezootaming.zonetype."
                                + typeKey(z.zoneType())), leftPos + 16, y + 13, ACCENT, false);
                g.drawString(font, z.size() + " \u25A0", leftPos + 122, y + 13, TEXT_DIM, false);
            }
        }

        // ---- Panneau de details (droite bas) ----
        SyncZonesS2CPacket.ZoneInfo z = selected();
        if (z != null && z.zoneType() != 0) {
            int ix = leftPos + 226, iy = topPos + PANEL_H - 82;
            g.fill(ix - 4, iy - 4, leftPos + PANEL_W - 8, iy + 46, PANEL);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.zonetype."
                    + typeKey(z.zoneType())), ix, iy, ACCENT, false);
            var lines = font.split(Component.translatable("gui.ultimatezootaming.zonehelp."
                    + typeKey(z.zoneType())), PANEL_W - 210);
            int hy = iy + 12;
            for (var l : lines) { g.drawString(font, l, ix, hy, TEXT_DIM, false); hy += 10; }
        } else if (z != null) {
            int ix = leftPos + 226, iy = topPos + PANEL_H - 112;
            g.fill(ix - 4, iy - 4, leftPos + PANEL_W - 8, iy + 76, PANEL);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.zones.info_animals",
                    z.animalCount(), z.speciesCount()), ix, iy, TEXT, false);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.zones.info_welfare")
                            .append(" " + (z.animalCount() == 0 ? "-" : z.avgWelfare() + "%")),
                    ix, iy + 11, welfareColor(z.avgWelfare(), z.animalCount()), false);
            // Les 5 composantes, comme le jeu : Espace/Habitat/Nourriture/Compagnie/Sante
            if (z.animalCount() > 0) {
                int[] vals = {z.avgSpace(), z.avgHabitat(), z.avgFood(), z.avgCompany(), z.avgHealth()};
                int[] maxs = {30, 25, 20, 15, 10};
                String[] keys = {"space", "habitat", "food", "company", "health"};
                // Emoji par categorie (aide a lire les colonnes d'un coup d'oeil)
                String[] icons = {
                        com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_SPACE,
                        com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_HABITAT,
                        com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_FOOD,
                        com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_COMPANY,
                        com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_HEALTH,
                };
                int bx = ix + 74, bw = 46;
                for (int i = 0; i < 5; i++) {
                    int by2 = iy + 22 + i * 9;
                    g.drawString(font, icons[i], ix, by2, ACCENT, false);
                    g.drawString(font, Component.translatable(
                            "gui.ultimatezootaming.welfare." + keys[i]), ix + 10, by2, TEXT_DIM, false);
                    g.fill(bx, by2 + 1, bx + bw, by2 + 6, PANEL_LIGHT);
                    float ratio = Math.min(1f, vals[i] / (float) maxs[i]);
                    int col = ratio >= 0.7f ? ACCENT : ratio >= 0.4f ? YELLOW : RED;
                    g.fill(bx, by2 + 1, bx + (int) (bw * ratio), by2 + 6, col);
                    g.drawString(font, vals[i] + "/" + maxs[i], bx + bw + 4, by2, TEXT_DIM, false);
                }
            }
            g.drawString(font, Component.translatable("gui.ultimatezootaming.zones.info_feeders",
                    z.feederCount()), ix + 150, iy, TEXT, false);
            if (z.sickCount() > 0) {
                g.drawString(font, Component.translatable("gui.ultimatezootaming.zones.info_sick",
                        z.sickCount()), ix + 150, iy + 11, RED, false);
            }
            // Les animaux de l'enclos (jusqu'a 12 noms, en petit)
            if (!z.animalNames().isEmpty()) {
                String joined = String.join(", ", z.animalNames());
                if (z.animalCount() > z.animalNames().size()) joined += " +" + (z.animalCount() - z.animalNames().size());
                // coupe en 2 lignes max
                int maxW = PANEL_W - 210;
                String l1 = font.plainSubstrByWidth(joined, maxW);
                String rest = joined.substring(l1.length());
                g.drawString(font, l1, ix, iy - 16, TEXT_DIM, false);
                if (!rest.isEmpty()) g.drawString(font, font.plainSubstrByWidth(rest.trim(), maxW), ix, iy - 26, TEXT_DIM, false);
            }
        }

        super.render(g, mx, my, pt);
    }

    private int countKeepersIn(UUID zoneId) {
        int n = 0;
        for (SyncZonesS2CPacket.KeeperInfo k : keepers) if (k.hasZone(zoneId)) n++;
        return n;
    }

    @Override
    public boolean isPauseScreen() { return false; }


    /**
     * Change le prix d'un article et redemande les donnees au serveur pour voir
     * le nouveau tarif tout de suite. Un prix a 0 retire l'article de la vente.
     */
    private void setArticlePrice(String itemId, int newPrice, int shopType) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(itemId);
        if (rl == null) return;
        NetworkHandler.CHANNEL.sendToServer(
                new com.lex3d.ultimatezootaming.core.network.SetItemPriceC2SPacket(
                        rl, newPrice, shopType));
        refreshFromServer();
    }

    /** La zone actuellement selectionnee est-elle un enclos a animaux ? */
    private boolean selectedIsEnclosure() {
        UUID id = selectedZoneId();
        if (id == null) return false;
        for (SyncZonesS2CPacket.ZoneInfo z : zones) {
            if (z.id().equals(id)) return z.zoneType() == 0;
        }
        return false;
    }

    /**
     * Nombre d'ENCLOS a la charge d'un employe.
     * On ignore les zones d'un autre type : elles ne devraient plus s'y trouver,
     * mais un ancien monde peut en contenir tant que la purge n'est pas passee.
     */
    private int countEnclosures(SyncZonesS2CPacket.KeeperInfo k) {
        int n = 0;
        for (UUID id : k.assignedZones()) {
            for (SyncZonesS2CPacket.ZoneInfo z : zones) {
                if (z.id().equals(id) && z.zoneType() == 0) { n++; break; }
            }
        }
        return n;
    }
}
