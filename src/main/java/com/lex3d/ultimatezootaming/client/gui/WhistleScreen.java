package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.SyncFamiliarsS2CPacket;
import com.lex3d.ultimatezootaming.core.network.UpdateWanderRadiusC2SPacket;
import com.lex3d.ultimatezootaming.core.network.WhistleCommandC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Vrai GUI a panneau (style vanilla dessine par code : fond gris, biseaux 3D,
 * liste en creux) au lieu de boutons flottants sur fond transparent.
 * Liste des familiers a gauche (avec defilement si plus de 6), actions a droite.
 */
public class WhistleScreen extends Screen {

    private static final int PANEL_W = 312;
    private static final int PANEL_H = 214;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 6;

    // Couleurs style vanilla
    private static final int COL_BG = 0xFFC6C6C6;
    private static final int COL_BORDER = 0xFF000000;
    private static final int COL_LIGHT = 0xFFFFFFFF;
    private static final int COL_DARK = 0xFF555555;
    private static final int COL_INSET_BG = 0xFF8B8B8B;

    private List<SyncFamiliarsS2CPacket.FamiliarInfo> familiars;
    private final UUID focusUUID;
    private int selectedIndex = 0;
    private int scrollOffset = 0;
    private boolean confirmingRelease = false;

    private int leftPos;
    private int topPos;

    public WhistleScreen(List<SyncFamiliarsS2CPacket.FamiliarInfo> familiars, UUID focusUUID) {
        super(Component.translatable("gui.ultimatezootaming.whistle.title"));
        this.familiars = new ArrayList<>(familiars);
        this.focusUUID = focusUUID;
    }

    @Override
    protected void init() {
        super.init();
        this.leftPos = (this.width - PANEL_W) / 2;
        this.topPos = (this.height - PANEL_H) / 2;

        if (focusUUID != null) {
            for (int i = 0; i < familiars.size(); i++) {
                if (familiars.get(i).uuid().equals(focusUUID)) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        // garder la selection visible
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        if (selectedIndex >= scrollOffset + VISIBLE_ROWS) scrollOffset = selectedIndex - VISIBLE_ROWS + 1;
        rebuildButtons();
    }

    private static Component statusLabel(SyncFamiliarsS2CPacket.FamiliarInfo info) {
        String key;
        if (info.sitting()) key = "gui.ultimatezootaming.whistle.status_sitting";
        else if (info.guarding()) key = "gui.ultimatezootaming.whistle.status_guarding";
        else key = "gui.ultimatezootaming.whistle.status_following";
        return Component.translatable(key);
    }

    private void rebuildButtons() {
        clearWidgets();

        int listX = leftPos + 12;
        int listY = topPos + 26;
        int rightX = leftPos + 168;

        // ---- Liste (fenetre defilante) ----
        int maxScroll = Math.max(0, familiars.size() - VISIBLE_ROWS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scrollOffset + row;
            if (index >= familiars.size()) break;
            SyncFamiliarsS2CPacket.FamiliarInfo info = familiars.get(index);
            boolean isSelected = index == selectedIndex;
            final int fi = index;

            Component label = Component.literal(isSelected ? "\u25B6 " : "")
                    .append(Component.translatable(info.descriptionId()))
                    .append(" - ")
                    .append(statusLabel(info));

            Button rowButton = Button.builder(label,
                            btn -> {
                                selectedIndex = fi;
                                confirmingRelease = false;
                                rebuildButtons();
                            })
                    .bounds(listX, listY + row * ROW_HEIGHT, 122, 20)
                    .build();
            rowButton.active = !isSelected;
            addRenderableWidget(rowButton);
        }

        // Fleches de defilement (uniquement si necessaire)
        if (familiars.size() > VISIBLE_ROWS) {
            Button up = Button.builder(Component.literal("\u25B2"), btn -> {
                scrollOffset--;
                rebuildButtons();
            }).bounds(listX + 126, listY, 18, 20).build();
            up.active = scrollOffset > 0;
            addRenderableWidget(up);

            Button down = Button.builder(Component.literal("\u25BC"), btn -> {
                scrollOffset++;
                rebuildButtons();
            }).bounds(listX + 126, listY + (VISIBLE_ROWS - 1) * ROW_HEIGHT, 18, 20).build();
            down.active = scrollOffset < maxScroll;
            addRenderableWidget(down);
        }

        if (familiars.isEmpty() || selectedIndex >= familiars.size()) {
            return;
        }

        SyncFamiliarsS2CPacket.FamiliarInfo selected = familiars.get(selectedIndex);

        // ---- Actions a droite ----
        addRenderableWidget(Button.builder(
                        Component.translatable(selected.sitting()
                                ? "gui.ultimatezootaming.whistle.stand"
                                : "gui.ultimatezootaming.whistle.sit"),
                        btn -> sendAndClose(selected, WhistleCommandC2SPacket.Command.SIT))
                .bounds(rightX, topPos + 38, 132, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimatezootaming.whistle.follow"),
                        btn -> sendAndClose(selected, WhistleCommandC2SPacket.Command.FOLLOW))
                .bounds(rightX, topPos + 61, 132, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimatezootaming.whistle.recall"),
                        btn -> sendAndClose(selected, WhistleCommandC2SPacket.Command.RECALL))
                .bounds(rightX, topPos + 84, 132, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimatezootaming.whistle.guard"),
                        btn -> sendAndClose(selected, WhistleCommandC2SPacket.Command.GUARD))
                .bounds(rightX, topPos + 107, 132, 20).build());

        addRenderableWidget(new RadiusSlider(rightX, topPos + 132, 132, 20, selected));

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimatezootaming.whistle.locate"),
                        btn -> {
                            var player = this.minecraft.player;
                            if (player != null) {
                                double dx = selected.x() - player.getX();
                                double dy = selected.y() - player.getY();
                                double dz = selected.z() - player.getZ();
                                int dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
                                player.displayClientMessage(Component.translatable(
                                        "message.ultimatezootaming.locate",
                                        Component.translatable(selected.descriptionId()),
                                        dist,
                                        Component.translatable(directionKey(dx, dz))), true);
                            }
                            this.minecraft.setScreen(null);
                        })
                .bounds(rightX, topPos + 157, 132, 20).build());

        addRenderableWidget(Button.builder(
                        Component.translatable(confirmingRelease
                                ? "gui.ultimatezootaming.whistle.release_confirm"
                                : "gui.ultimatezootaming.whistle.release"),
                        btn -> {
                            if (!confirmingRelease) {
                                confirmingRelease = true;
                                rebuildButtons();
                            } else {
                                sendAndClose(selected, WhistleCommandC2SPacket.Command.RELEASE);
                            }
                        })
                .bounds(rightX, topPos + 186, 132, 20).build());
    }

    private void sendAndClose(SyncFamiliarsS2CPacket.FamiliarInfo target, WhistleCommandC2SPacket.Command command) {
        NetworkHandler.CHANNEL.sendToServer(new WhistleCommandC2SPacket(target.uuid(), command));
        this.minecraft.setScreen(null);
    }

    /** 8 points cardinaux a partir du delta (Minecraft : -Z = Nord, +X = Est). */
    private static String directionKey(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) angle += 360;
        String[] keys = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        int idx = (int) Math.round(angle / 45.0) % 8;
        return "direction.ultimatezootaming." + keys[idx];
    }

    private static class RadiusSlider extends AbstractSliderButton {
        private final SyncFamiliarsS2CPacket.FamiliarInfo target;

        RadiusSlider(int x, int y, int w, int h, SyncFamiliarsS2CPacket.FamiliarInfo target) {
            super(x, y, w, h, Component.literal("Radius: " + (int) target.wanderRadius()),
                    (target.wanderRadius() - 2.0) / 62.0);
            this.target = target;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("Radius: " + (int) (2 + value * 62)));
        }

        @Override
        protected void applyValue() {
            NetworkHandler.CHANNEL.sendToServer(
                    new UpdateWanderRadiusC2SPacket(target.uuid(), (int) (2 + value * 62)));
        }
    }

    public void refresh(List<SyncFamiliarsS2CPacket.FamiliarInfo> updated) {
        this.familiars = new ArrayList<>(updated);
        rebuildButtons();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (familiars.size() > VISIBLE_ROWS) {
            scrollOffset -= (int) Math.signum(delta);
            rebuildButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /** Panneau style vanilla : contour noir, fond gris, biseaux 3D. */
    private void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, COL_BORDER);
        g.fill(x, y, x + w, y + h, COL_BG);
        g.fill(x, y, x + w - 1, y + 2, COL_LIGHT);           // haut
        g.fill(x, y, x + 2, y + h - 1, COL_LIGHT);           // gauche
        g.fill(x + 1, y + h - 2, x + w, y + h, COL_DARK);    // bas
        g.fill(x + w - 2, y + 1, x + w, y + h, COL_DARK);    // droite
    }

    /** Zone "en creux" (comme les slots d'inventaire) : biseaux inverses. */
    private void drawInset(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, COL_INSET_BG);
        g.fill(x, y, x + w - 1, y + 1, COL_DARK);
        g.fill(x, y, x + 1, y + h - 1, COL_DARK);
        g.fill(x + 1, y + h - 1, x + w, y + h, COL_LIGHT);
        g.fill(x + w - 1, y + 1, x + w, y + h, COL_LIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        drawPanel(graphics, leftPos, topPos, PANEL_W, PANEL_H);
        // zone de liste en creux (gauche)
        drawInset(graphics, leftPos + 8, topPos + 22, 152, VISIBLE_ROWS * ROW_HEIGHT + 6);
        // zone d'actions en creux leger (droite)
        drawInset(graphics, leftPos + 164, topPos + 22, 140, PANEL_H - 30);

        // titre dans le panneau (sans ombre, comme les titres d'inventaire vanilla)
        int titleW = this.font.width(this.title);
        graphics.drawString(this.font, this.title, leftPos + (PANEL_W - titleW) / 2, topPos + 8, 0x404040, false);

        // statut du familier selectionne, en tete de la colonne d'actions
        if (!familiars.isEmpty() && selectedIndex < familiars.size()) {
            SyncFamiliarsS2CPacket.FamiliarInfo selected = familiars.get(selectedIndex);
            graphics.drawString(this.font,
                    Component.translatable("gui.ultimatezootaming.whistle.current_status")
                            .append(" ").append(statusLabel(selected)),
                    leftPos + 168, topPos + 27, 0x2E7D32, false);

            // Bien-etre : barre de satisfaction coloree + enclos + maladie
            int satY = topPos + 214 - 40;
            int sat = selected.satisfaction();
            int barColor = selected.sick() ? 0xFF9C27B0
                    : sat > 75 ? 0xFF4CAF50
                    : sat < 25 ? 0xFFF44336
                    : 0xFFFFC107;
            String moodKey = selected.sick() ? "mood_sick"
                    : sat > 75 ? "mood_happy"
                    : sat < 25 ? "mood_miserable"
                    : "mood_neutral";
            graphics.drawString(this.font,
                    Component.translatable("gui.ultimatezootaming.whistle.welfare")
                            .append(" ").append(Component.translatable("gui.ultimatezootaming.whistle." + moodKey)),
                    leftPos + 12, satY - 12, 0x404040, false);
            // fond de barre + remplissage
            graphics.fill(leftPos + 12, satY, leftPos + 152, satY + 6, 0xFF555555);
            int fillW = (int) (140 * (sat / 100.0));
            graphics.fill(leftPos + 12, satY, leftPos + 12 + fillW, satY + 6, barColor);
            // nom de l'enclos si assigne
            if (!selected.zoneName().isEmpty()) {
                graphics.drawString(this.font,
                        Component.translatable("gui.ultimatezootaming.whistle.in_enclosure",
                                selected.zoneName()),
                        leftPos + 12, satY + 10, 0x2E5C8A, false);
            }
        }

        if (familiars.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.ultimatezootaming.whistle.empty"),
                    leftPos + 84, topPos + 90, 0xFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
