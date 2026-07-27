package com.lex3d.ultimatezootaming.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Manuel du zoo, organise en CATEGORIES.
 *
 * Ecran d'accueil : le sommaire, une ligne cliquable par categorie.
 * Une fois dans une categorie, on feuillette ses pages et on peut revenir au
 * sommaire a tout moment.
 */
public class ZooGuideScreen extends Screen {

    private static final int W = 320, H = 280;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;

    /** null = on est au sommaire ; sinon la categorie en cours de lecture. */
    private GuideCategory category;
    private int page;
    private int leftPos, topPos;

    public ZooGuideScreen() {
        super(Component.translatable("guide.ultimatezootaming.title"));
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        if (category == null) buildIndex(); else buildPage();
        // Fermer : toujours disponible
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + W - 68, topPos + H - 26, 58, 18).build());
    }

    /** Sommaire : une ligne cliquable par categorie. */
    private void buildIndex() {
        var all = GuideCategory.values();
        for (int i = 0; i < all.length; i++) {
            final GuideCategory cat = all[i];
            int y = topPos + 30 + i * 21;
            var pick = Button.builder(Component.literal(""),
                            b -> { category = cat; page = 0; rebuild(); })
                    .bounds(leftPos + 10, y, W - 20, 19).build();
            pick.setAlpha(0f); // la ligne dessinee sert de visuel
            addRenderableWidget(pick);
        }
    }

    /** Lecture d'une categorie : navigation entre ses pages + retour au sommaire. */
    private void buildPage() {
        addRenderableWidget(Button.builder(Component.translatable("guide.ultimatezootaming.back"),
                        b -> { category = null; page = 0; rebuild(); })
                .bounds(leftPos + 10, topPos + H - 26, 70, 18).build());
        if (page > 0) {
            addRenderableWidget(Button.builder(Component.literal("<"),
                            b -> { page--; rebuild(); })
                    .bounds(leftPos + 88, topPos + H - 26, 20, 18).build());
        }
        if (page < category.pageCount - 1) {
            addRenderableWidget(Button.builder(Component.literal(">"),
                            b -> { page++; rebuild(); })
                    .bounds(leftPos + 112, topPos + H - 26, 20, 18).build());
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 22, PANEL);
        g.fill(leftPos, topPos + 22, leftPos + W, topPos + 23, ACCENT);

        if (category == null) renderIndex(g);
        else renderPage(g);

        super.render(g, mx, my, pt);
    }

    private void renderIndex(GuiGraphics g) {
        g.drawString(font, title, leftPos + 10, topPos + 7, ACCENT, false);
        Component hint = Component.translatable("guide.ultimatezootaming.index_hint");
        g.drawString(font, hint, leftPos + W - 10 - font.width(hint.getString()),
                topPos + 7, TEXT_DIM, false);

        var all = GuideCategory.values();
        for (int i = 0; i < all.length; i++) {
            GuideCategory cat = all[i];
            int y = topPos + 30 + i * 21;
            g.fill(leftPos + 10, y, leftPos + W - 10, y + 19, i % 2 == 0 ? PANEL : PANEL_LIGHT);
            g.drawString(font, Component.translatable(cat.titleKey()),
                    leftPos + 16, y + 2, TEXT, false);
            g.drawString(font, Component.translatable(cat.summaryKey()),
                    leftPos + 16, y + 11, TEXT_DIM, false);
            // Nombre de pages, cale a droite
            String n = cat.pageCount + " p.";
            g.drawString(font, n, leftPos + W - 18 - font.width(n), y + 6, TEXT_DIM, false);
        }
    }

    private void renderPage(GuiGraphics g) {
        // En-tete : categorie + numero de page
        g.drawString(font, Component.translatable(category.titleKey()),
                leftPos + 10, topPos + 7, ACCENT, false);
        String pg = (page + 1) + "/" + category.pageCount;
        g.drawString(font, pg, leftPos + W - 10 - font.width(pg), topPos + 7, TEXT_DIM, false);

        // Titre de la page
        g.drawString(font, Component.translatable(category.pageTitleKey(page)),
                leftPos + 12, topPos + 30, TEXT, false);

        // Corps, decoupe a la largeur du panneau
        var lines = font.split(Component.translatable(category.pageBodyKey(page)), W - 26);
        int y = topPos + 44;
        for (var line : lines) {
            if (y > topPos + H - 34) break;
            g.drawString(font, line, leftPos + 12, y, TEXT_DIM, false);
            y += 11;
        }
    }

    /** Le retour clavier ramene au sommaire avant de fermer le manuel. */
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && category != null) { // Echap
            category = null;
            page = 0;
            rebuild();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
