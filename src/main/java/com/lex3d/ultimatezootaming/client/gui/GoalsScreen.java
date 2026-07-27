package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.SyncZonesS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Les 10 objectifs du zoo : progression, primes, et ce qu'il reste a accomplir. */
public class GoalsScreen extends Screen {

    private static final int W = 300, H = 232;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int GOLD = 0xFFE0B94F;
    /** {id, cible, prime} — miroir de ZooDayHandler.GOALS. */
    private static final int[][] GOALS = {
            {0, 5, 50}, {1, 10, 100}, {2, 20, 100}, {3, 25, 30}, {4, 50, 80},
            {5, 70, 150}, {6, 90, 300}, {7, 100, 120}, {8, 1000, 200}, {9, 5, 80},
    };

    private final Screen parent;
    private final SyncZonesS2CPacket.ZooStats stats;
    private int leftPos, topPos;

    public GoalsScreen(Screen parent, SyncZonesS2CPacket.ZooStats stats) {
        super(Component.translatable("gui.ultimatezootaming.goals.title"));
        this.parent = parent;
        this.stats = stats;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
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

        int[] progress = stats.goalProgress();
        boolean[] done = stats.goalDone();
        for (int i = 0; i < GOALS.length; i++) {
            int id = GOALS[i][0], target = GOALS[i][1], reward = GOALS[i][2];
            int y = topPos + 30 + i * 18;
            boolean ok = id < done.length && done[id];
            int cur = id < progress.length ? Math.min(progress[id], target) : 0;

            g.drawString(font, (ok ? "\u2714 " : "\u25CB "), leftPos + 8, y + 2, ok ? ACCENT : TEXT_DIM, false);
            g.drawString(font, Component.translatable("goal.ultimatezootaming." + id),
                    leftPos + 22, y + 2, ok ? ACCENT : TEXT, false);
            // barre de progression
            int bw = 60, bx = leftPos + W - 130;
            g.fill(bx, y + 3, bx + bw, y + 10, PANEL_LIGHT);
            g.fill(bx, y + 3, bx + (int) (bw * (cur / (double) target)), y + 10, ok ? ACCENT : GOLD);
            String p = cur + "/" + target;
            g.pose().pushPose();
            g.pose().translate(bx + bw + 4, y + 3, 0);
            g.pose().scale(0.8f, 0.8f, 1f);
            g.drawString(font, p, 0, 0, TEXT_DIM, false);
            g.pose().popPose();
            g.drawString(font, "+" + reward + "\u01B5", leftPos + W - 34, y + 2, GOLD, false);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
