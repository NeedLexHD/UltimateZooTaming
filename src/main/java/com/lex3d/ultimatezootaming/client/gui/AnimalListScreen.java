package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.ReleaseAnimalC2SPacket;
import com.lex3d.ultimatezootaming.core.network.SyncAnimalsS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fiche des animaux d'un enclos : etat de chacun + liberation. */
public class AnimalListScreen extends Screen {

    private static final int W = 320, H = 230, ROW = 22, VISIBLE = 7;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int RED = 0xFFE05555, YELLOW = 0xFFE0B94F;

    private final UUID zoneId;
    private final String zoneName;
    private final List<SyncAnimalsS2CPacket.AnimalInfo> animals;
    private int leftPos, topPos, scroll = 0;
    private int confirming = -1; // entityId en attente de confirmation

    public AnimalListScreen(UUID zoneId, String zoneName, List<SyncAnimalsS2CPacket.AnimalInfo> animals) {
        super(Component.literal(zoneName));
        this.zoneId = zoneId;
        this.zoneName = zoneName;
        this.animals = new ArrayList<>(animals);
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int max = Math.max(0, animals.size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll, max));

        for (int row = 0; row < VISIBLE; row++) {
            int idx = scroll + row;
            if (idx >= animals.size()) break;
            SyncAnimalsS2CPacket.AnimalInfo a = animals.get(idx);
            int y = topPos + 40 + row * ROW;
            boolean confirm = confirming == a.entityId();
            addRenderableWidget(Button.builder(
                            Component.translatable(confirm
                                    ? "gui.ultimatezootaming.animals.confirm"
                                    : "gui.ultimatezootaming.animals.release"),
                            b -> onRelease(a))
                    .bounds(leftPos + W - 78, y, 68, 18).build());
        }
        if (animals.size() > VISIBLE) {
            Button up = Button.builder(Component.literal("\u25B2"), b -> { scroll--; rebuild(); })
                    .bounds(leftPos + W - 96, topPos + 40, 14, 16).build();
            up.active = scroll > 0;
            addRenderableWidget(up);
            Button down = Button.builder(Component.literal("\u25BC"), b -> { scroll++; rebuild(); })
                    .bounds(leftPos + W - 96, topPos + 40 + (VISIBLE - 1) * ROW, 14, 16).build();
            down.active = scroll < max;
            addRenderableWidget(down);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + W / 2 - 30, topPos + H - 26, 60, 18).build());
    }

    /** Double-clic de securite : premier clic = demande confirmation. */
    private void onRelease(SyncAnimalsS2CPacket.AnimalInfo a) {
        if (confirming != a.entityId()) {
            confirming = a.entityId();
            rebuild();
            return;
        }
        NetworkHandler.CHANNEL.sendToServer(new ReleaseAnimalC2SPacket(a.entityId()));
        animals.remove(a);
        confirming = -1;
        rebuild();
    }

    private int welfareColor(int w) {
        if (w >= 75) return ACCENT;
        if (w >= 40) return YELLOW;
        return RED;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 30, PANEL);
        g.fill(leftPos, topPos + 30, leftPos + W, topPos + 31, ACCENT);

        g.drawString(font, Component.translatable("gui.ultimatezootaming.animals.title", zoneName),
                leftPos + 10, topPos + 6, TEXT, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.animals.count", animals.size()),
                leftPos + 10, topPos + 18, TEXT_DIM, false);

        if (animals.isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.animals.none"),
                    leftPos + 10, topPos + 60, TEXT_DIM, false);
        }
        for (int row = 0; row < VISIBLE; row++) {
            int idx = scroll + row;
            if (idx >= animals.size()) break;
            SyncAnimalsS2CPacket.AnimalInfo a = animals.get(idx);
            int y = topPos + 40 + row * ROW;
            g.fill(leftPos + 10, y, leftPos + W - 100, y + ROW - 3, PANEL);

            String name = (a.severe() ? "\u271A\u271A " : a.sick() ? "\u271A " : "")
                    + a.name() + (a.baby() ? " \ud83d\udc23" : "");
            g.drawString(font, name, leftPos + 16, y + 2,
                    a.severe() ? 0xFF8B2020 : a.sick() ? RED : TEXT, false);
            if (a.severe()) {
                g.drawString(font, Component.translatable("gui.ultimatezootaming.animals.severe"),
                        leftPos + 16 + font.width(name) + 6, y + 2, 0xFF8B2020, false);
            }
            String sub = a.species() + (a.trait().isEmpty() ? "" : " \u00b7 "
                    + Component.translatable("trait.ultimatezootaming." + a.trait().toLowerCase()).getString());
            g.drawString(font, font.plainSubstrByWidth(sub, W - 190), leftPos + 16, y + 12, TEXT_DIM, false);

            // 5 micro-colonnes Espace/Habitat/Nourriture/Compagnie/Sante + note globale
            int[] vals = {a.space(), a.habitat(), a.food(), a.company(), a.health()};
            int[] maxs = {30, 25, 20, 15, 10};
            int bx = leftPos + W - 158, colH = 14;
            for (int ci = 0; ci < 5; ci++) {
                int cx = bx + ci * 6;
                g.fill(cx, y + 2, cx + 4, y + 2 + colH, PANEL_LIGHT);
                float ratio = Math.min(1f, vals[ci] / (float) maxs[ci]);
                int h = (int) (colH * ratio);
                int col = ratio >= 0.7f ? ACCENT : ratio >= 0.4f ? YELLOW : RED;
                g.fill(cx, y + 2 + colH - h, cx + 4, y + 2 + colH, col);
            }
            g.drawString(font, a.welfare() + "%", bx + 34, y + 5, welfareColor(a.welfare()), false);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
