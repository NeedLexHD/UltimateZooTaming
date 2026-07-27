package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.SyncAnimalsS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Fiche individuelle d'un animal : la satisfaction decomposee, lisible d'un clic. */
public class AnimalCardScreen extends Screen {

    private static final int W = 260, H = 208;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int RED = 0xFFE05555, YELLOW = 0xFFE0B94F;

    private final SyncAnimalsS2CPacket.AnimalInfo a;
    private int leftPos, topPos;

    public AnimalCardScreen(SyncAnimalsS2CPacket.AnimalInfo animal) {
        super(Component.literal(animal.name()));
        this.a = animal;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(leftPos + W / 2 - 30, topPos + H - 24, 60, 18).build());
    }

    private int col(float ratio) {
        return ratio >= 0.7f ? ACCENT : ratio >= 0.4f ? YELLOW : RED;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 26, PANEL);
        g.fill(leftPos, topPos + 26, leftPos + W, topPos + 27, ACCENT);

        String head = a.name() + (a.baby() ? " \ud83d\udc23" : "");
        g.drawString(font, head, leftPos + 10, topPos + 5, TEXT, false);
        boolean hasTrait = !a.trait().isEmpty() && !a.trait().equalsIgnoreCase("none");
        String sub = a.species() + (!hasTrait ? "" : " \u00b7 "
                + Component.translatable("trait.ultimatezootaming." + a.trait().toLowerCase()).getString());
        g.drawString(font, font.plainSubstrByWidth(sub, W - 20), leftPos + 10, topPos + 16, TEXT_DIM, false);

        // Effet du trait (petite explication)
        int healthY = topPos + 32;
        if (hasTrait) {
            g.drawString(font, font.plainSubstrByWidth(
                    Component.translatable("trait.ultimatezootaming." + a.trait().toLowerCase() + ".desc").getString(),
                    W - 20), leftPos + 10, topPos + 26, 0xFF7A9BC0, false);
            healthY = topPos + 40;
        }
        // Sante
        if (a.severe()) {
            g.drawString(font, Component.literal("\u271A\u271A ").append(
                    Component.translatable("gui.ultimatezootaming.card.severe")), leftPos + 10, healthY, 0xFF8B2020, false);
        } else if (a.sick()) {
            g.drawString(font, Component.literal("\u271A ").append(
                    Component.translatable("gui.ultimatezootaming.card.sick")), leftPos + 10, healthY, RED, false);
        } else {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.card.healthy"),
                    leftPos + 10, healthY, ACCENT, false);
        }
        // Bien-etre en ETOILES au quart pres + pourcentage
        double rating = com.lex3d.ultimatezootaming.client.gui.util.Stars.percentToRating(a.welfare());
        String w = com.lex3d.ultimatezootaming.client.gui.util.Stars.fromRating(rating)
                + "  " + a.welfare() + "%";
        g.drawString(font, w, leftPos + W - 12 - font.width(w), topPos + 32, col(a.welfare() / 100f), false);

        // Les 5 composantes en grandes barres
        int[] vals = {a.space(), a.habitat(), a.food(), a.company(), a.health()};
        int[] maxs = {30, 25, 20, 15, 10};
        String[] keys = {"space", "habitat", "food", "company", "health"};
        String[] icons = {
                com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_SPACE,
                com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_HABITAT,
                com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_FOOD,
                com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_COMPANY,
                com.lex3d.ultimatezootaming.client.gui.util.Stars.ICON_HEALTH,
        };
        int bx = leftPos + 84, bw = 96;
        for (int i = 0; i < 5; i++) {
            int y = topPos + 48 + i * 14;
            g.drawString(font, icons[i], leftPos + 10, y, ACCENT, false);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.welfare." + keys[i]),
                    leftPos + 20, y, TEXT_DIM, false);
            g.fill(bx, y + 1, bx + bw, y + 8, PANEL_LIGHT);
            float ratio = Math.min(1f, vals[i] / (float) maxs[i]);
            g.fill(bx, y + 1, bx + (int) (bw * ratio), y + 8, col(ratio));
            g.drawString(font, vals[i] + "/" + maxs[i], bx + bw + 6, y, TEXT_DIM, false);
        }

        // ---- HISTORIQUE & GENETIQUE (Vague 3) ----
        int hy = topPos + 122;
        g.fill(leftPos + 8, hy - 4, leftPos + W - 8, hy - 3, PANEL_LIGHT); // separateur
        g.drawString(font, Component.translatable("gui.ultimatezootaming.card.history"),
                leftPos + 10, hy, ACCENT, false);
        hy += 12;

        // Rarete genetique : couleur + libelle
        if (a.rarity() > 0) {
            int rarCol = switch (a.rarity()) {
                case 1 -> 0xFFC0C0C0;  // argent
                case 2 -> 0xFFE0B94F;  // or
                default -> 0xFFFFF0F5; // albinos
            };
            g.drawString(font, Component.literal("\u2726 ").append(
                            Component.translatable("gui.ultimatezootaming.rarity." + a.rarity())),
                    leftPos + 10, hy, rarCol, false);
            hy += 10;
        }

        // Jour de capture / naissance
        if (a.captureDay() > 0) {
            g.drawString(font, Component.translatable(
                    "gui.ultimatezootaming.card.captured", a.captureDay()),
                    leftPos + 10, hy, TEXT_DIM, false);
        }
        // Generation (0 = capture dans la nature)
        if (a.generation() > 0) {
            String gen = Component.translatable(
                    "gui.ultimatezootaming.card.generation", a.generation()).getString();
            g.drawString(font, gen, leftPos + W - 12 - font.width(gen), hy, TEXT_DIM, false);
        }
        hy += 10;

        // Soins recus et bebes
        g.drawString(font, Component.translatable(
                "gui.ultimatezootaming.card.heals", a.healCount()),
                leftPos + 10, hy, TEXT_DIM, false);
        String babies = Component.translatable(
                "gui.ultimatezootaming.card.babies", a.babyCount()).getString();
        g.drawString(font, babies, leftPos + W - 12 - font.width(babies), hy, TEXT_DIM, false);
        hy += 10;

        // Meilleur ami
        if (!a.bestFriendName().isEmpty()) {
            g.drawString(font, Component.literal("\u2665 ").append(
                            Component.translatable("gui.ultimatezootaming.card.friend",
                                    a.bestFriendName())),
                    leftPos + 10, hy, 0xFFE08AA0, false);
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
