package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.HireCandidateC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.OpenRecruitmentS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Ordinateur de recrutement : 3 candidatures, chacune avec metier/etoiles/cout. */
public class RecruitmentScreen extends Screen {

    private static final int W = 300, H = 200;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0, GOLD = 0xFFE0B94F;
    private static final String[] JOB_KEYS = {"generalist", "vet", "feeder", "guard", "vendor"};

    private final OpenRecruitmentS2CPacket data;
    private int leftPos, topPos;

    public RecruitmentScreen(OpenRecruitmentS2CPacket data) {
        super(Component.translatable("gui.ultimatezootaming.recruit.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        for (int i = 0; i < data.candidates.size(); i++) {
            var c = data.candidates.get(i);
            int y = topPos + 34 + i * 46;
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.ultimatezootaming.recruit.hire", c.cost()),
                            b -> {
                                NetworkHandler.CHANNEL.sendToServer(new HireCandidateC2SPacket(
                                        data.pos, c.name(), c.job(), c.cost()));
                                onClose();
                            })
                    .bounds(leftPos + W - 90, y + 10, 78, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
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

        for (int i = 0; i < data.candidates.size(); i++) {
            var c = data.candidates.get(i);
            int y = topPos + 34 + i * 46;
            g.fill(leftPos + 10, y, leftPos + W - 10, y + 40, PANEL);
            g.fill(leftPos + 10, y, leftPos + 12, y + 40, ACCENT);
            g.drawString(font, c.name(), leftPos + 20, y + 6, TEXT, false);
            g.drawString(font, Component.translatable("job.ultimatezootaming." + JOB_KEYS[c.job()]),
                    leftPos + 20, y + 18, ACCENT, false);
            // etoiles de competence
            StringBuilder stars = new StringBuilder();
            for (int s = 0; s < 5; s++) stars.append(s < c.skill() ? "\u2605" : "\u2606");
            g.drawString(font, stars.toString(), leftPos + 20, y + 29, GOLD, false);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.recruit.salary", c.cost()),
                    leftPos + 130, y + 18, TEXT_DIM, false);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
