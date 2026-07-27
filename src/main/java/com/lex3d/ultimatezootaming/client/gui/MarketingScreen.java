package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.BuyCampaignC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.marketing.AdCampaign;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ecran marketing : liste les 4 campagnes publicitaires achetables.
 * Chaque campagne : cout, duree, bonus d'affluence, rang requis.
 */
public class MarketingScreen extends Screen {

    private static final int W = 320, H = 220;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0, GOLD = 0xFFE0B94F;
    private static final int RED = 0xFFD06A6A;

    private final int highestRank;
    private final int activeCampaign;
    private final int campaignDaysLeft;
    private int leftPos, topPos;

    public MarketingScreen(int highestRank, int activeCampaign, int campaignDaysLeft) {
        super(Component.translatable("gui.ultimatezootaming.marketing.title"));
        this.highestRank = highestRank;
        this.activeCampaign = activeCampaign;
        this.campaignDaysLeft = campaignDaysLeft;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        // 4 boutons (une par campagne, sauf NONE)
        var camps = AdCampaign.values();
        for (int i = 1; i < camps.length; i++) {
            final int ord = i;
            AdCampaign c = camps[i];
            int y = topPos + 40 + (i - 1) * 40;
            boolean unlocked = c.isUnlocked(highestRank);
            boolean noneActive = campaignDaysLeft <= 0;
            Button b = Button.builder(Component.translatable("gui.ultimatezootaming.marketing.buy"),
                            btn -> NetworkHandler.CHANNEL.sendToServer(new BuyCampaignC2SPacket(ord)))
                    .bounds(leftPos + W - 76, y + 10, 66, 20).build();
            b.active = unlocked && noneActive;
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
        // Bandeau campagne en cours (si active)
        if (campaignDaysLeft > 0) {
            var active = AdCampaign.values()[activeCampaign];
            g.drawString(font, Component.translatable("gui.ultimatezootaming.marketing.active",
                    Component.translatable("campaign.ultimatezootaming." + active.key), campaignDaysLeft),
                    leftPos + 12, topPos + 26, GOLD, false);
        }
        // Les 4 campagnes
        var camps = AdCampaign.values();
        for (int i = 1; i < camps.length; i++) {
            AdCampaign c = camps[i];
            int y = topPos + 40 + (i - 1) * 40;
            boolean unlocked = c.isUnlocked(highestRank);
            g.fill(leftPos + 10, y, leftPos + W - 10, y + 36, i % 2 == 0 ? PANEL : PANEL_LIGHT);
            int col = unlocked ? TEXT : TEXT_DIM;
            g.drawString(font, Component.translatable("campaign.ultimatezootaming." + c.key),
                    leftPos + 16, y + 5, col, false);
            String desc = String.format("+%d%%  \u00b7  %sj  \u00b7  %s \u01B5",
                    (int)(c.crowdBonus * 100), c.durationDays, c.cost);
            g.drawString(font, desc, leftPos + 16, y + 18, unlocked ? TEXT_DIM : RED, false);
            if (!unlocked) {
                g.drawString(font, Component.translatable("gui.ultimatezootaming.marketing.locked",
                                Component.translatable("rank.ultimatezootaming." + c.minRank.key)),
                        leftPos + 16, y + 27, RED, false);
            }
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
