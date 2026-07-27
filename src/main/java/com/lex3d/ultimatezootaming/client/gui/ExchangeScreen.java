package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.ExchangeC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Comptoir d'echange : convertit les billets de parc en minerais. */
public class ExchangeScreen extends Screen {

    private static final int W = 260, H = 240;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0, GOLD = 0xFFE0B94F;

    private final BlockPos pos;
    private final Screen parent;
    private int balance;
    private int leftPos, topPos;

    public ExchangeScreen(Screen parent, BlockPos pos, int balance) {
        super(Component.translatable("gui.ultimatezootaming.exchange.title"));
        this.parent = parent;
        this.pos = pos;
        this.balance = balance;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        for (int i = 0; i < ExchangeC2SPacket.TABLE.length; i++) {
            final int idx = i;
            int y = topPos + 30 + i * 22;
            addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.exchange.buy"),
                            b -> NetworkHandler.CHANNEL.sendToServer(new ExchangeC2SPacket(pos, idx)))
                    .bounds(leftPos + W - 66, y, 56, 18).build());
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
        // Solde bien decale a droite pour ne pas chevaucher le titre
        String bal = balance + " \u01B5";
        g.drawString(font, bal, leftPos + W - font.width(bal) - 10, topPos + 7, GOLD, false);

        for (int i = 0; i < ExchangeC2SPacket.TABLE.length; i++) {
            Object[] row = ExchangeC2SPacket.TABLE[i];
            int y = topPos + 30 + i * 22;
            g.fill(leftPos + 8, y, leftPos + W - 70, y + 18, i % 2 == 0 ? PANEL : PANEL_LIGHT);
            ItemStack stack = new ItemStack((Item) row[0], (int) row[2]);
            g.renderItem(stack, leftPos + 12, y + 1);
            g.renderItemDecorations(font, stack, leftPos + 12, y + 1);
            g.drawString(font, stack.getHoverName(), leftPos + 34, y + 5, TEXT, false);
            int cost = (int) row[1];
            boolean afford = balance >= cost;
            g.drawString(font, cost + " \u01B5", leftPos + W - 108, y + 5, afford ? ACCENT : 0xFFD06A6A, false);
        }
        super.render(g, mx, my, pt);
    }

    /** Rafraichit le solde apres un echange (appele par le paquet S2C). */
    public void setBalance(int b) { this.balance = b; }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
