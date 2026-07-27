package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.SetItemPriceC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Tarification d'un item a la caisse : -/+ et validation. Aucune commande. */
public class PriceScreen extends Screen {

    private static final int W = 200, H = 120;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;

    private final ResourceLocation itemId;
    private final int shopType;
    private int price;
    private int leftPos, topPos;

    public PriceScreen(ResourceLocation itemId, int currentPrice, int shopType) {
        super(Component.translatable("gui.ultimatezootaming.price.title"));
        this.itemId = itemId;
        this.price = Math.max(1, currentPrice);
        this.shopType = shopType;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        int cy = topPos + 52;
        addRenderableWidget(Button.builder(Component.literal("-5"), b -> adjust(-5))
                .bounds(leftPos + 14, cy, 28, 18).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), b -> adjust(-1))
                .bounds(leftPos + 46, cy, 28, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), b -> adjust(1))
                .bounds(leftPos + W - 74, cy, 28, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+5"), b -> adjust(5))
                .bounds(leftPos + W - 42, cy, 28, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.price.ok"),
                        b -> { send(price); onClose(); })
                .bounds(leftPos + 14, topPos + H - 28, 84, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.price.remove"),
                        b -> { send(0); onClose(); })
                .bounds(leftPos + W - 98, topPos + H - 28, 84, 18).build());
    }

    private void adjust(int delta) { price = Math.max(1, Math.min(999, price + delta)); }

    private void send(int value) {
        NetworkHandler.CHANNEL.sendToServer(new SetItemPriceC2SPacket(itemId, value, shopType));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 24, PANEL);
        g.fill(leftPos, topPos + 24, leftPos + W, topPos + 25, ACCENT);
        g.drawString(font, title, leftPos + 10, topPos + 8, TEXT, false);

        Item item = ForgeRegistries.ITEMS.getValue(itemId);
        if (item != null) {
            g.renderItem(new ItemStack(item), leftPos + 12, topPos + 30);
            g.drawString(font, font.plainSubstrByWidth(item.getDescription().getString(), W - 46),
                    leftPos + 34, topPos + 34, TEXT, false);
        }
        String p = price + " \u01B5";
        g.drawString(font, p, leftPos + (W - font.width(p)) / 2, topPos + 57, ACCENT, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.price.category",
                        Component.translatable("shop.ultimatezootaming."
                                + com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.values()[shopType]
                                        .name().toLowerCase())),
                leftPos + 14, topPos + 76, TEXT_DIM, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
