package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.SyncFeedS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

/**
 * ZooTok : le flux social du zoo.
 *
 * Chaque post affiche son auteur, son texte, sa vignette d'espece quand il y en
 * a une, et ses likes. Un bandeau resume l'humeur generale et l'effet du buzz
 * sur l'affluence.
 */
public class FeedScreen extends Screen {

    private static final int W = 320, H = 240;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int GOLD = 0xFFE0B94F, RED = 0xFFD06A6A, PINK = 0xFFE07A9A;
    private static final int VISIBLE = 5;

    private final SyncFeedS2CPacket data;
    private int leftPos, topPos, scroll;

    public FeedScreen(SyncFeedS2CPacket data) {
        super(Component.translatable("gui.ultimatezootaming.feed.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int max = Math.max(0, data.entries.size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll, max));
        if (data.entries.size() > VISIBLE) {
            addRenderableWidget(Button.builder(Component.literal("\u25B2"),
                            b -> { scroll--; rebuild(); })
                    .bounds(leftPos + W - 26, topPos + 52, 16, 16).build());
            addRenderableWidget(Button.builder(Component.literal("\u25BC"),
                            b -> { scroll++; rebuild(); })
                    .bounds(leftPos + W - 26, topPos + H - 48, 16, 16).build());
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

        // --- Bandeau de temperature : humeur et effet sur l'affluence ---
        int mood = data.moodPercent;
        int moodCol = mood >= 60 ? ACCENT : mood >= 40 ? GOLD : RED;
        g.drawString(font, Component.translatable("gui.ultimatezootaming.feed.mood", mood),
                leftPos + 12, topPos + 30, moodCol, false);

        int buzz = data.buzzPercent;
        Component buzzTxt = Component.translatable("gui.ultimatezootaming.feed.buzz", buzz);
        g.drawString(font, buzzTxt, leftPos + W - 12 - font.width(buzzTxt.getString()),
                topPos + 30, buzz >= 100 ? ACCENT : RED, false);

        // Barre d'humeur
        int barW = W - 24;
        g.fill(leftPos + 12, topPos + 42, leftPos + 12 + barW, topPos + 47, 0xFF3A3B45);
        g.fill(leftPos + 12, topPos + 42, leftPos + 12 + (barW * mood / 100), topPos + 47, moodCol);

        if (data.entries.isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.feed.empty"),
                    leftPos + 12, topPos + 60, TEXT_DIM, false);
            super.render(g, mx, my, pt);
            return;
        }

        // --- Les posts ---
        for (int row = 0; row < VISIBLE; row++) {
            int idx = scroll + row;
            if (idx >= data.entries.size()) break;
            var e = data.entries.get(idx);
            int y = topPos + 54 + row * 34;
            g.fill(leftPos + 10, y, leftPos + W - 30, y + 32, row % 2 == 0 ? PANEL : PANEL_LIGHT);

            // Vignette : le spawn egg de l'espece concernee
            if (!e.species().isEmpty()) {
                var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                        .getValue(ResourceLocation.tryParse(e.species()));
                if (type != null) {
                    SpawnEggItem egg = SpawnEggItem.byId(type);
                    if (egg != null) g.renderItem(new ItemStack(egg), leftPos + 14, y + 8);
                }
            }
            int textX = e.species().isEmpty() ? leftPos + 16 : leftPos + 36;

            // Auteur + jour
            g.drawString(font, e.author(), textX, y + 3, ACCENT, false);
            String when = "J" + e.day();
            g.drawString(font, when, leftPos + W - 36 - font.width(when), y + 3, TEXT_DIM, false);

            // Corps du post, compose depuis la cle de traduction
            Component body = Component.translatable(
                    "post.ultimatezootaming." + kindKey(e.kind()),
                    e.subject().isEmpty()
                            ? Component.translatable("gui.ultimatezootaming.feed.the_zoo")
                            : Component.literal(e.subject()));
            var lines = font.split(body, W - 60);
            int ty = y + 13;
            for (int i = 0; i < lines.size() && i < 2; i++) {
                g.drawString(font, lines.get(i), textX, ty, TEXT, false);
                ty += 9;
            }

            // Likes
            String likes = "\u2665 " + e.likes();
            g.drawString(font, likes, leftPos + W - 36 - font.width(likes), y + 21, PINK, false);
        }
        super.render(g, mx, my, pt);
    }

    /** Le nom d'enum arrive en majuscules : on retrouve la cle de traduction. */
    private static String kindKey(String enumName) {
        return switch (enumName) {
            case "BEAUTIFUL_ENCLOSURE" -> "beautiful";
            case "BABY_BORN" -> "baby";
            case "RARE_SPOTTED" -> "rare";
            case "CUTE_MOMENT" -> "cute";
            case "DIRTY_PARK" -> "dirty";
            case "SAD_ANIMAL" -> "sad_animal";
            case "TOO_CROWDED" -> "crowded";
            case "OVERPRICED" -> "pricey";
            case "LONG_QUEUE" -> "queue";
            default -> "great_day";
        };
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
