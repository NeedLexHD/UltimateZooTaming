package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.client.gui.menu.ShopMenu;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.SetShopTypeC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ecran de la caisse enregistreuse : stock (prix affiche sous chaque slot),
 * choix du type de commerce par BOUTONS, inventaire joueur.
 */
public class ShopScreen extends AbstractContainerScreen<ShopMenu> {

    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final String[] TYPE_KEYS = {"souvenir", "meal", "icecream", "drink", "photo"};

    private final Button[] typeButtons = new Button[5];

    public ShopScreen(ShopMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 4 boutons de type au-dessus du stock
        int bw = 32, bx = leftPos + 4, by = topPos + 14;
        for (int i = 0; i < 5; i++) {
            final int type = i;
            typeButtons[i] = Button.builder(
                            Component.translatable("shop.ultimatezootaming." + TYPE_KEYS[i]),
                            b -> NetworkHandler.CHANNEL.sendToServer(
                                    new SetShopTypeC2SPacket(menu.getPos(), type)))
                    .bounds(bx + i * (bw + 1), by, bw, 14).build();
            addRenderableWidget(typeButtons[i]);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        int active = menu.getShopType();
        for (int i = 0; i < 5; i++) typeButtons[i].active = i != active;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.fill(leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BG);
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + 12, PANEL);
        g.fill(leftPos, topPos + 12, leftPos + imageWidth, topPos + 13, ACCENT);
        // Cases des slots
        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
            int x = leftPos + slot.x - 1, y = topPos + slot.y - 1;
            g.fill(x, y, x + 18, y + 18, PANEL_LIGHT);
            g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF15161C);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        // Prix sous chaque slot du stock
        for (int i = 0; i < 9; i++) {
            int price = menu.priceAt(i);
            var slot = menu.slots.get(i);
            if (!slot.hasItem()) continue;
            int x = leftPos + slot.x, y = topPos + slot.y + 18;
            String txt = price > 0 ? price + "\u01B5" : "\u2716";
            int color = price > 0 ? ACCENT : 0xFFE05555;
            g.pose().pushPose();
            g.pose().translate(x + 8 - font.width(txt) * 0.35, y, 0);
            g.pose().scale(0.7f, 0.7f, 1f);
            g.drawString(font, txt, 0, 0, color, false);
            g.pose().popPose();
        }
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        g.drawString(font, title, 5, 3, TEXT, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT_DIM, false);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.shop.hint"),
                5, 60, TEXT_DIM, false);
    }
}
