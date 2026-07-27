package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.FulfillContractC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.SyncContractS2CPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Contrats internationaux : l'offre du jour et les animaux du zoo qui peuvent
 * l'honorer. Un clic sur "Livrer" envoie l'animal et encaisse la recompense.
 */
public class ContractScreen extends Screen {

    private static final int W = 300, H = 252;
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int GOLD = 0xFFE0B94F, RED = 0xFFD06A6A;

    private final Screen parent;
    private final SyncContractS2CPacket data;
    private int leftPos, topPos;

    public ContractScreen(Screen parent, SyncContractS2CPacket data) {
        super(Component.translatable("gui.ultimatezootaming.contract.title"));
        this.parent = parent;
        this.data = data;
    }

    @Override
    protected void init() {
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;

        // Un bouton Livrer par animal eligible
        for (int i = 0; i < data.candidates.size() && i < 5; i++) {
            final var cand = data.candidates.get(i);
            int y = topPos + 112 + i * 22;
            addRenderableWidget(Button.builder(
                            Component.translatable("gui.ultimatezootaming.contract.deliver"),
                            b -> {
                                NetworkHandler.CHANNEL.sendToServer(
                                        new FulfillContractC2SPacket(cand.entityId()));
                                onClose();
                            })
                    .bounds(leftPos + W - 76, y, 66, 18).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> minecraft.setScreen(parent))
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

        if (!data.hasContract) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.contract.none"),
                    leftPos + 12, topPos + 40, TEXT_DIM, false);
            super.render(g, mx, my, pt);
            return;
        }

        // Le demandeur et l'espece reclamee
        g.drawString(font, data.client, leftPos + 12, topPos + 32, ACCENT, false);
        var type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(ResourceLocation.tryParse(data.species));
        Component sp = type != null ? Component.translatable(type.getDescriptionId())
                                    : Component.literal(data.species);
        g.drawString(font, Component.translatable("gui.ultimatezootaming.contract.wants", sp),
                leftPos + 12, topPos + 44, TEXT, false);

        // Colonne de droite : recompense, prestige, delai (trois lignes courtes)
        String pay = data.reward + " \u01B5";
        g.drawString(font, pay, leftPos + W - 12 - font.width(pay), topPos + 32, GOLD, false);
        String pres = "+" + data.prestige + " prestige";
        g.drawString(font, pres, leftPos + W - 12 - font.width(pres), topPos + 44, TEXT_DIM, false);
        Component days = Component.translatable("gui.ultimatezootaming.contract.days", data.daysLeft);
        String daysTxt = days.getString();
        g.drawString(font, daysTxt, leftPos + W - 12 - font.width(daysTxt), topPos + 56,
                data.daysLeft <= 1 ? RED : TEXT_DIM, false);

        // L'exigence occupe SA PROPRE ligne, sur toute la largeur : elle est bien
        // trop longue pour cohabiter avec le delai sur la meme ligne.
        Component req = Component.translatable(
                "contract.ultimatezootaming.req." + data.requirement.toLowerCase());
        int ry = topPos + 70;
        for (var line : font.split(req, W - 24)) {
            g.drawString(font, line, leftPos + 12, ry, TEXT_DIM, false);
            ry += 10;
        }

        g.fill(leftPos + 10, topPos + 92, leftPos + W - 10, topPos + 93, PANEL_LIGHT);

        // Les candidats
        if (data.candidates.isEmpty()) {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.contract.no_match"),
                    leftPos + 12, topPos + 100, RED, false);
        } else {
            g.drawString(font, Component.translatable("gui.ultimatezootaming.contract.eligible",
                            data.candidates.size()), leftPos + 12, topPos + 100, ACCENT, false);
            for (int i = 0; i < data.candidates.size() && i < 5; i++) {
                var c = data.candidates.get(i);
                int y = topPos + 112 + i * 22;
                g.fill(leftPos + 10, y, leftPos + W - 80, y + 20, i % 2 == 0 ? PANEL : PANEL_LIGHT);
                g.drawString(font, c.name(), leftPos + 16, y + 2, TEXT, false);
                // Bien-etre en etoiles + marqueurs
                String stars = com.lex3d.ultimatezootaming.client.gui.util.Stars
                        .fromPercent(c.welfare());
                g.drawString(font, stars, leftPos + 16, y + 11, GOLD, false);
                if (c.baby()) {
                    g.drawString(font, Component.translatable("gui.ultimatezootaming.contract.baby"),
                            leftPos + 70, y + 11, ACCENT, false);
                }
                if (c.rarity() > 0) {
                    g.drawString(font, "\u2726", leftPos + 110, y + 11, GOLD, false);
                }
            }
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
