package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.ClaimMissionC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.SyncMissionsS2CPacket;
import com.lex3d.ultimatezootaming.progression.DailyMission;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Missions journalieres : 3 defis du jour avec barres de progression et boutons "Reclamer". */
public class MissionsScreen extends Screen {

    private static final int W = 280, H = 220;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0, GOLD = 0xFFE0B94F;

    private int[] missions;
    private int[] progress;
    private boolean[] claimed;
    private int leftPos, topPos;

    public MissionsScreen(SyncMissionsS2CPacket data) {
        super(Component.translatable("gui.ultimatezootaming.missions.title"));
        this.missions = data.missions;
        this.progress = data.progress;
        this.claimed = data.claimed;
    }

    /** Rafraichi apres reclamation. */
    public void update(SyncMissionsS2CPacket data) {
        this.missions = data.missions;
        this.progress = data.progress;
        this.claimed = data.claimed;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        clearWidgets();
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            DailyMission m = DailyMission.values()[missions[i]];
            boolean done = progress[i] >= m.target;
            int y = topPos + 30 + i * 54;
            String label = claimed[i] ? "gui.ultimatezootaming.missions.claimed"
                    : done ? "gui.ultimatezootaming.missions.claim"
                    : "gui.ultimatezootaming.missions.in_progress";
            Button b = Button.builder(Component.translatable(label),
                            btn -> NetworkHandler.CHANNEL.sendToServer(new ClaimMissionC2SPacket(slot)))
                    .bounds(leftPos + W - 76, y + 24, 66, 20).build();
            b.active = done && !claimed[i];
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + W / 2 - 30, topPos + H - 22, 60, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 22, PANEL);
        g.fill(leftPos, topPos + 22, leftPos + W, topPos + 23, ACCENT);
        g.drawString(font, title, leftPos + 10, topPos + 7, TEXT, false);

        for (int i = 0; i < 3; i++) {
            DailyMission m = DailyMission.values()[missions[i]];
            int y = topPos + 30 + i * 54;
            g.fill(leftPos + 10, y, leftPos + W - 10, y + 50, i % 2 == 0 ? PANEL : PANEL_LIGHT);
            // Titre + description
            g.drawString(font, Component.translatable("mission.ultimatezootaming." + m.key),
                    leftPos + 16, y + 6, TEXT, false);
            g.drawString(font, Component.translatable("mission.ultimatezootaming." + m.key + ".desc"),
                    leftPos + 16, y + 18, TEXT_DIM, false);
            // Barre de progression
            int barW = 150;
            int filled = Math.min(barW, (int) (barW * (progress[i] / (double) m.target)));
            g.fill(leftPos + 16, y + 32, leftPos + 16 + barW, y + 40, 0xFF3A3B45);
            g.fill(leftPos + 16, y + 32, leftPos + 16 + filled, y + 40, claimed[i] ? TEXT_DIM : ACCENT);
            // Progression + prime
            String prog = progress[i] + "/" + m.target;
            g.drawString(font, prog, leftPos + 16 + barW + 6, y + 32, TEXT_DIM, false);
            g.drawString(font, "+" + m.reward + " Ƶ", leftPos + 16 + barW + 6, y + 42, GOLD, false);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
