package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.RequestSkillsC2SPacket;
import com.lex3d.ultimatezootaming.core.network.SyncZonesS2CPacket;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Registre du personnel, ouvert depuis l'ordinateur du patron : la liste de tous
 * les employes avec leur metier, leur niveau et leur position actuelle.
 * Un clic sur une ligne ouvre la fiche de competences de cet employe.
 */
public class StaffListScreen extends Screen {

    private static final int W = 300, H = 220;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int GOLD = 0xFFE0B94F, RED = 0xFFD06A6A;
    private static final int VISIBLE = 7;

    private final Screen parent;
    private final List<SyncZonesS2CPacket.KeeperInfo> keepers;
    private int leftPos, topPos, scroll;

    public StaffListScreen(Screen parent, List<SyncZonesS2CPacket.KeeperInfo> keepers) {
        super(Component.translatable("gui.ultimatezootaming.staff.title"));
        this.parent = parent;
        this.keepers = keepers;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int max = Math.max(0, keepers.size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll, max));

        for (int row = 0; row < VISIBLE; row++) {
            int idx = scroll + row;
            if (idx >= keepers.size()) break;
            final var k = keepers.get(idx);
            int y = topPos + 34 + row * 24;
            // Toute la ligne est cliquable : ouvre la fiche de competences
            var open = Button.builder(Component.literal(""),
                            b -> NetworkHandler.CHANNEL.sendToServer(
                                    new RequestSkillsC2SPacket(k.entityId())))
                    .bounds(leftPos + 10, y, W - 20, 22).build();
            open.setAlpha(0f); // invisible : c'est la ligne dessinee qui sert de visuel
            addRenderableWidget(open);
        }
        if (keepers.size() > VISIBLE) {
            addRenderableWidget(Button.builder(Component.literal("\u25B2"), b -> { scroll--; rebuild(); })
                    .bounds(leftPos + W - 26, topPos + 34, 16, 16).build());
            addRenderableWidget(Button.builder(Component.literal("\u25BC"), b -> { scroll++; rebuild(); })
                    .bounds(leftPos + W - 26, topPos + H - 46, 16, 16).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> minecraft.setScreen(parent))
                .bounds(leftPos + W / 2 - 30, topPos + H - 24, 60, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 22, PANEL);
        g.fill(leftPos, topPos + 22, leftPos + W, topPos + 23, ACCENT);
        g.drawString(font, title, leftPos + 10, topPos + 7, TEXT, false);
        String count = keepers.size() + "";
        g.drawString(font, Component.translatable("gui.ultimatezootaming.staff.roster", count),
                leftPos + W - 10 - font.width(
                        Component.translatable("gui.ultimatezootaming.staff.roster", count).getString()),
                topPos + 7, TEXT_DIM, false);

        if (keepers.isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.staff.none"),
                    leftPos + 12, topPos + 40, TEXT_DIM, false);
        }

        for (int row = 0; row < VISIBLE; row++) {
            int idx = scroll + row;
            if (idx >= keepers.size()) break;
            var k = keepers.get(idx);
            int y = topPos + 34 + row * 24;
            g.fill(leftPos + 10, y, leftPos + W - 30, y + 22, row % 2 == 0 ? PANEL : PANEL_LIGHT);

            g.drawString(font, k.name(), leftPos + 16, y + 3, TEXT, false);
            g.drawString(font, Component.translatable(
                            "jobshort.ultimatezootaming." + ZooKeeperEntity.jobKey(k.job())),
                    leftPos + 16, y + 13, ACCENT, false);
            // Ce qu'il fait en ce moment, puis ou il se trouve
            var task = com.lex3d.ultimatezootaming.entities.KeeperTask.byOrdinal(k.task());
            if (task != com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE) {
                Component act = Component.literal(task.icon + " ")
                        .append(Component.translatable(task.translationKey()));
                g.drawString(font, act, leftPos + 60, y + 3, ACCENT, false);
            }
            boolean strike = "greve".equals(k.location());
            g.drawString(font, locationLabel(k.location()), leftPos + 60, y + 13,
                    strike ? RED : TEXT_DIM, false);
            // Niveau
            String lvl = "Nv." + k.level();
            g.drawString(font, lvl, leftPos + W - 44 - font.width(lvl), y + 8, GOLD, false);
        }
        // Invite : cliquer pour ouvrir la fiche
        g.drawString(font, Component.translatable("gui.ultimatezootaming.staff.hint"),
                leftPos + 12, topPos + H - 40, TEXT_DIM, false);
        super.render(g, mx, my, pt);
    }

    private static Component locationLabel(String loc) {
        return switch (loc) {
            case "greve"   -> Component.translatable("gui.ultimatezootaming.staff.loc_strike");
            case "dortoir" -> Component.translatable("gui.ultimatezootaming.staff.loc_bed");
            case "repos"   -> Component.translatable("gui.ultimatezootaming.staff.loc_rest");
            case "chemin"  -> Component.translatable("gui.ultimatezootaming.staff.loc_path");
            default        -> Component.literal(loc);
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
