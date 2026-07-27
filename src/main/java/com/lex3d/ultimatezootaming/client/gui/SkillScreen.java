package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.SyncSkillsS2CPacket;
import com.lex3d.ultimatezootaming.core.network.UpgradeSkillC2SPacket;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.progression.KeeperSkill;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Fiche de competences d'un employe : niveau, XP, points disponibles, et les
 * six competences avec leurs rangs et un bouton pour investir un point.
 */
public class SkillScreen extends Screen {

    private static final int W = 300, H = 236;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int GOLD = 0xFFE0B94F, DIM_SLOT = 0xFF3A3B45;

    private SyncSkillsS2CPacket data;
    private int leftPos, topPos;

    public SkillScreen(SyncSkillsS2CPacket data) {
        super(Component.translatable("gui.ultimatezootaming.skills.title"));
        this.data = data;
    }

    /** Rafraichi apres chaque investissement de point. */
    public void update(SyncSkillsS2CPacket fresh) {
        this.data = fresh;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();
        var all = KeeperSkill.values();
        for (int i = 0; i < all.length; i++) {
            final int idx = i;
            KeeperSkill s = all[i];
            int rank = idx < data.ranks.length ? data.ranks[idx] : 0;
            int y = topPos + 52 + i * 26;
            boolean canUp = data.freePoints > 0 && rank < s.maxRank;
            Button b = Button.builder(Component.literal("+"),
                            btn -> NetworkHandler.CHANNEL.sendToServer(
                                    new UpgradeSkillC2SPacket(data.entityId, idx)))
                    .bounds(leftPos + W - 34, y + 3, 20, 18).build();
            b.active = canUp;
            addRenderableWidget(b);
        }
        // Reinitialiser : rend tous les points investis
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.skills.reset"),
                        b -> NetworkHandler.CHANNEL.sendToServer(
                                new UpgradeSkillC2SPacket(data.entityId, -1)))
                .bounds(leftPos + 12, topPos + H - 24, 100, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + W - 72, topPos + H - 24, 60, 18).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 22, PANEL);
        g.fill(leftPos, topPos + 22, leftPos + W, topPos + 23, ACCENT);

        // En-tete : nom, metier, niveau
        g.drawString(font, data.name, leftPos + 10, topPos + 7, TEXT, false);
        Component job = Component.translatable(
                "job.ultimatezootaming." + ZooKeeperEntity.jobKey(data.job));
        g.drawString(font, job, leftPos + 10 + font.width(data.name) + 8, topPos + 7, ACCENT, false);
        String lvl = "Nv." + data.level;
        g.drawString(font, lvl, leftPos + W - 10 - font.width(lvl), topPos + 7, GOLD, false);

        // Barre d'XP vers le niveau suivant (50 XP par niveau)
        int intoLevel = data.xp % 50;
        int barW = W - 24;
        g.fill(leftPos + 12, topPos + 28, leftPos + 12 + barW, topPos + 34, DIM_SLOT);
        g.fill(leftPos + 12, topPos + 28,
                leftPos + 12 + (int) (barW * (intoLevel / 50.0)), topPos + 34, ACCENT);
        String xpTxt = intoLevel + " / 50 XP";
        g.drawString(font, xpTxt, leftPos + 12, topPos + 37, TEXT_DIM, false);

        // Points disponibles
        Component pts = Component.translatable("gui.ultimatezootaming.skills.points", data.freePoints);
        g.drawString(font, pts, leftPos + W - 10 - font.width(pts.getString()), topPos + 37,
                data.freePoints > 0 ? GOLD : TEXT_DIM, false);

        // Les six competences
        var all = KeeperSkill.values();
        for (int i = 0; i < all.length; i++) {
            KeeperSkill s = all[i];
            int rank = i < data.ranks.length ? data.ranks[i] : 0;
            int y = topPos + 52 + i * 26;
            g.fill(leftPos + 10, y, leftPos + W - 10, y + 24, i % 2 == 0 ? PANEL : PANEL_LIGHT);

            // Nom + description (la Maitrise depend du metier)
            g.drawString(font, Component.translatable("skill.ultimatezootaming." + s.key),
                    leftPos + 16, y + 3, rank > 0 ? TEXT : TEXT_DIM, false);
            String descKey = (s == KeeperSkill.MASTERY)
                    ? KeeperSkill.masteryDescriptionKey(data.job)
                    : "skill.ultimatezootaming." + s.key + ".desc";
            g.drawString(font, Component.translatable(descKey), leftPos + 16, y + 13, TEXT_DIM, false);

            // Rangs sous forme de pastilles
            int px = leftPos + W - 34 - (s.maxRank * 12) - 6;
            for (int r = 0; r < s.maxRank; r++) {
                int cx = px + r * 12;
                g.fill(cx, y + 8, cx + 9, y + 17, r < rank ? ACCENT : DIM_SLOT);
            }
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
