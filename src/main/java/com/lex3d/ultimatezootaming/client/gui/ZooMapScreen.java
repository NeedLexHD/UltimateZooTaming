package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.MapDataS2CPacket;
import com.lex3d.ultimatezootaming.core.network.MapEditC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.RequestMapC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.MapColor;

/**
 * La Carte du Zoo, propre : relief ombre comme une carte vanilla, deplacement a
 * la souris (glisser en mode Voir), PEINTURE des allees en glissant (mode
 * Chemins, avec le bloc du 1er slot de ta hotbar), territoire par chunks.
 */
public class ZooMapScreen extends Screen {

    private static final int S = MapDataS2CPacket.SIZE;
    private static final int PX = 4;                 // 4 px par bloc
    private static final int MAP = S * PX;           // 256
    private static final int SIDE = 118;             // panneau droit
    private static final int BG = 0xF01A1B22, PANEL = 0xFF23242E, PANEL_LIGHT = 0xFF2C2E3A;
    private static final int ACCENT = 0xFF4FD08A, TEXT = 0xFFE8E8F0, TEXT_DIM = 0xFF9A9CB0;
    private static final int RED = 0xFFE05555, GOLD = 0xFFE0B94F, BLUE = 0xFF6FA8DC;

    private MapDataS2CPacket data;
    private int mode = 0; // 0 voir, 1 territoire, 2 chemins
    private boolean erase = false; // mode chemins : effacer
    private int leftPos, topPos, mapX, mapY;
    private final Button[] modeButtons = new Button[3];
    private Button eraseButton;
    private Button addPathBlockButton;
    // interactions souris
    private boolean dragging = false;
    private double dragStartX, dragStartY;
    private int lastPaintX = Integer.MIN_VALUE, lastPaintZ = Integer.MIN_VALUE;

    public ZooMapScreen(MapDataS2CPacket data) {
        super(Component.translatable("gui.ultimatezootaming.map.title"));
        this.data = data;
    }

    public void updateData(MapDataS2CPacket newData) { this.data = newData; }

    @Override
    protected void init() {
        int W = MAP + SIDE + 26, H = MAP + 40;
        leftPos = (width - W) / 2;
        topPos = (height - H) / 2;
        mapX = leftPos + 8;
        mapY = topPos + 30;

        int bx = mapX + MAP + 10;
        String[] keys = {"view", "territory", "paths"};
        for (int i = 0; i < 3; i++) {
            final int m = i;
            modeButtons[i] = Button.builder(
                            Component.translatable("gui.ultimatezootaming.map.mode." + keys[i]),
                            b -> { mode = m; refreshButtons(); })
                    .bounds(bx, topPos + 30 + i * 21, SIDE, 18).build();
            addRenderableWidget(modeButtons[i]);
        }
        eraseButton = Button.builder(Component.translatable("gui.ultimatezootaming.map.erase_off"),
                        b -> {
                            erase = !erase;
                            b.setMessage(Component.translatable(erase
                                    ? "gui.ultimatezootaming.map.erase_on"
                                    : "gui.ultimatezootaming.map.erase_off"));
                        })
                .bounds(bx, topPos + 30 + 3 * 21, SIDE, 18).build();
        addRenderableWidget(eraseButton);
        addPathBlockButton = Button.builder(
                        Component.translatable("gui.ultimatezootaming.map.add_path_block"),
                        b -> NetworkHandler.CHANNEL.sendToServer(
                                new com.lex3d.ultimatezootaming.core.network.TogglePathBlockC2SPacket()))
                .bounds(bx, topPos + 30 + 4 * 21, SIDE, 18).build();
        addRenderableWidget(addPathBlockButton);
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.map.refresh"),
                        b -> requestAt(data.centerX, data.centerZ))
                .bounds(bx, topPos + MAP - 32, SIDE, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                        b -> onClose())
                .bounds(bx, topPos + MAP - 10, SIDE, 18).build());
        refreshButtons();
    }

    private void refreshButtons() {
        for (int i = 0; i < 3; i++) modeButtons[i].active = i != mode;
        eraseButton.visible = mode == 2;
        addPathBlockButton.visible = mode == 2;
    }

    private void requestAt(int cx, int cz) {
        NetworkHandler.CHANNEL.sendToServer(new RequestMapC2SPacket(cx, cz));
    }

    private boolean inMap(double mx, double my) {
        return mx >= mapX && my >= mapY && mx < mapX + MAP && my < mapY + MAP;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (inMap(mx, my)) {
            dragging = true;
            dragStartX = mx;
            dragStartY = my;
            if (mode != 0) paintAt(mx, my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging && mode == 2 && inMap(mx, my)) {
            paintAt(mx, my); // peinture continue des allees
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            if (mode == 0) {
                // Glisser = deplacer la carte
                int dxBlocks = (int) ((dragStartX - mx) / PX);
                int dzBlocks = (int) ((dragStartY - my) / PX);
                if (Math.abs(dxBlocks) > 1 || Math.abs(dzBlocks) > 1) {
                    requestAt(data.centerX + dxBlocks, data.centerZ + dzBlocks);
                }
            } else {
                requestAt(data.centerX, data.centerZ); // voir le resultat des edits
            }
            dragging = false;
            lastPaintX = Integer.MIN_VALUE;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private void paintAt(double mx, double my) {
        int bx = (int) ((mx - mapX) / PX);
        int bz = (int) ((my - mapY) / PX);
        int worldX = data.centerX - S / 2 + bx;
        int worldZ = data.centerZ - S / 2 + bz;
        if (worldX == lastPaintX && worldZ == lastPaintZ) return;
        lastPaintX = worldX;
        lastPaintZ = worldZ;
        int idx = bz * S + bx;
        if (mode == 1) {
            boolean claimed = (data.flags[idx] & 2) != 0;
            NetworkHandler.CHANNEL.sendToServer(new MapEditC2SPacket(claimed ? 1 : 0, worldX, worldZ));
            // Edition optimiste : tout le chunk a l'ecran change tout de suite
            int chunkBx = (bx + (data.centerX - S / 2)) >> 4, chunkBz = (bz + (data.centerZ - S / 2)) >> 4;
            for (int i = 0; i < S; i++) {
                for (int j = 0; j < S; j++) {
                    int wx = data.centerX - S / 2 + i, wz = data.centerZ - S / 2 + j;
                    if ((wx >> 4) == chunkBx && (wz >> 4) == chunkBz) {
                        int k = j * S + i;
                        data.flags[k] = (byte) (claimed ? data.flags[k] & ~2 : data.flags[k] | 2);
                    }
                }
            }
        } else if (mode == 2) {
            NetworkHandler.CHANNEL.sendToServer(new MapEditC2SPacket(erase ? 3 : 2, worldX, worldZ));
            // Affichage immediat de la case peinte/effacee
            data.flags[idx] = (byte) (erase ? data.flags[idx] & ~1 : data.flags[idx] | 1);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int W = MAP + SIDE + 26, H = MAP + 40;
        g.fill(leftPos - 2, topPos - 2, leftPos + W + 2, topPos + H + 2, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + W, topPos + H, BG);
        g.fill(leftPos, topPos, leftPos + W, topPos + 22, PANEL);
        g.fill(leftPos, topPos + 22, leftPos + W, topPos + 23, ACCENT);
        g.drawString(font, title, leftPos + 8, topPos + 7, TEXT, false);
        var claims = Component.translatable("gui.ultimatezootaming.map.claims", data.claimedCount);
        g.drawString(font, claims, leftPos + W - 8 - font.width(claims), topPos + 7, ACCENT, false);

        // ---- Terrain avec RELIEF (ombrage par pente, comme une carte vanilla) ----
        g.fill(mapX - 1, mapY - 1, mapX + MAP + 1, mapY + MAP + 1, 0xFF000000);
        for (int bz = 0; bz < S; bz++) {
            for (int bx = 0; bx < S; bx++) {
                int idx = bz * S + bx;
                MapColor mc = MapColor.byId(data.colors[idx] & 0xFF);
                int base = mc == MapColor.NONE ? 0x202020 : mc.col;
                // pente : compare a la case au nord (bz-1)
                int shade = 0;
                if (bz > 0) {
                    int hN = data.heights[(bz - 1) * S + bx] & 0xFF;
                    int h = data.heights[idx] & 0xFF;
                    shade = h > hN ? 24 : h < hN ? -28 : 0;
                }
                int r = clamp(((base >> 16) & 0xFF) + shade);
                int gr = clamp(((base >> 8) & 0xFF) + shade);
                int b = clamp((base & 0xFF) + shade);
                int col = 0xFF000000 | (r << 16) | (gr << 8) | b;
                int x = mapX + bx * PX, y = mapY + bz * PX;
                g.fill(x, y, x + PX, y + PX, col);
                if ((data.flags[idx] & 2) != 0 && mode != 2) {
                    g.fill(x, y, x + PX, y + PX, 0x2E48D08A); // voile territoire
                }
            }
        }
        // Grille + survol des chunks en mode territoire
        if (mode == 1) {
            int x0 = data.centerX - S / 2, z0 = data.centerZ - S / 2;
            for (int bx = Math.floorMod(-x0, 16); bx <= S; bx += 16) {
                g.fill(mapX + bx * PX, mapY, mapX + bx * PX + 1, mapY + MAP, 0x55FFFFFF);
            }
            for (int bz = Math.floorMod(-z0, 16); bz <= S; bz += 16) {
                g.fill(mapX, mapY + bz * PX, mapX + MAP, mapY + bz * PX + 1, 0x55FFFFFF);
            }
            if (inMap(mx, my)) {
                int hbx = ((int) (mx - mapX) / PX + x0) >> 4, hbz = ((int) (my - mapY) / PX + z0) >> 4;
                int sx = mapX + ((hbx << 4) - x0) * PX, sz = mapY + ((hbz << 4) - z0) * PX;
                g.fill(Math.max(mapX, sx), Math.max(mapY, sz),
                        Math.min(mapX + MAP, sx + 16 * PX), Math.min(mapY + MAP, sz + 16 * PX),
                        0x40FFFFFF);
            }
        }
        // ---- Marqueurs ----
        for (MapDataS2CPacket.Marker m : data.markers) {
            int x = mapX + m.x() * PX + PX / 2, y = mapY + m.z() * PX + PX / 2;
            int col = switch (m.type()) {
                case 0 -> ACCENT;
                case 1, 2 -> GOLD;
                case 3 -> BLUE;
                case 4 -> TEXT;
                case 5 -> RED;
                default -> 0xFFFF7DF2;
            };
            int r = m.type() >= 5 ? 3 : 2;
            g.fill(x - r, y - r, x + r, y + r, 0xFF000000);
            g.fill(x - r + 1, y - r + 1, x + r - 1, y + r - 1, col);
        }
        // ---- Panneau droit : legende ----
        int bx2 = mapX + MAP + 10, ly = topPos + 30 + 5 * 21 + 6;
        String[][] legend = {
                {"entrance", String.valueOf(ACCENT)}, {"register", String.valueOf(GOLD)},
                {"visitor", String.valueOf(BLUE)}, {"staff", String.valueOf(TEXT)},
                {"escaped", String.valueOf(RED)}, {"you", String.valueOf(0xFFFF7DF2)}};
        for (String[] item : legend) {
            int col = Integer.parseInt(item[1]);
            g.fill(bx2, ly + 1, bx2 + 6, ly + 7, 0xFF000000);
            g.fill(bx2 + 1, ly + 2, bx2 + 5, ly + 6, col);
            g.drawString(font, Component.translatable("gui.ultimatezootaming.map.legend." + item[0]),
                    bx2 + 10, ly, TEXT_DIM, false);
            ly += 11;
        }
        // Coordonnees survolees + aide du mode
        if (inMap(mx, my)) {
            int wx = data.centerX - S / 2 + (int) (mx - mapX) / PX;
            int wz = data.centerZ - S / 2 + (int) (my - mapY) / PX;
            g.drawString(font, wx + ", " + wz, bx2, ly + 4, TEXT_DIM, false);
        }
        g.drawString(font, Component.translatable("gui.ultimatezootaming.map.hint." + mode),
                leftPos + 8, topPos + H - 12, TEXT_DIM, false);
        super.render(g, mx, my, pt);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    @Override
    public boolean isPauseScreen() { return false; }
}
