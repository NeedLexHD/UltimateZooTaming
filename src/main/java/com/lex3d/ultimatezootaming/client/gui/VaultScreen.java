package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.OpenVaultS2CPacket;
import com.lex3d.ultimatezootaming.core.network.VaultActionC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** La Caisse du Zoo : solde, depot en un clic, retraits. */
public class VaultScreen extends Screen {

    private static final int W = 210, H = 170;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;

    private final BlockPos pos;
    private int balance, total;
    private int leftPos, topPos;

    public VaultScreen(OpenVaultS2CPacket data) {
        super(Component.translatable("gui.ultimatezootaming.vault.title"));
        this.pos = data.pos;
        this.balance = data.balance;
        this.total = data.total;
    }

    public void update(OpenVaultS2CPacket data) {
        this.balance = data.balance;
        this.total = data.total;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.vault.deposit_all"),
                        b -> act(0))
                .bounds(leftPos + 12, topPos + 62, 186, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.vault.take16"),
                        b -> act(1))
                .bounds(leftPos + 12, topPos + 84, 90, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.vault.take64"),
                        b -> act(2))
                .bounds(leftPos + 108, topPos + 84, 90, 18).build());
        // Comptoir d'echange : billets -> minerais
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.vault.exchange"),
                        b -> minecraft.setScreen(new ExchangeScreen(this, pos, total)))
                .bounds(leftPos + 12, topPos + 104, 186, 18).build());
        // Recyclage : dechets ramasses -> billets
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.vault.recycle"),
                        b -> NetworkHandler.CHANNEL.sendToServer(
                                new com.lex3d.ultimatezootaming.core.network.RecycleWasteC2SPacket(pos)))
                .bounds(leftPos + 12, topPos + 124, 186, 18).build());
    }

    private void act(int action) {
        NetworkHandler.CHANNEL.sendToServer(new VaultActionC2SPacket(pos, action));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 22, PANEL);
        g.fill(leftPos, topPos + 22, leftPos + W, topPos + 23, ACCENT);
        g.drawString(font, title, leftPos + 10, topPos + 7, TEXT, false);

        String bal = balance + " \u01B5";
        g.pose().pushPose();
        g.pose().translate(leftPos + W / 2f - font.width(bal), topPos + 32, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, bal, 0, 0, ACCENT, false);
        g.pose().popPose();
        g.drawString(font, Component.translatable("gui.ultimatezootaming.vault.total", total),
                leftPos + 12, topPos + 50, TEXT_DIM, false);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
