package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import com.lex3d.ultimatezootaming.core.network.SaveHabitatC2SPacket;
import com.lex3d.ultimatezootaming.welfare.HabitatManager;
import com.lex3d.ultimatezootaming.welfare.HabitatProfile;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI admin (/zootame habitats) : TOUS les types de creatures (vanilla + mods),
 * avec pour chacun un bouton Habitat (cycle des profils) et un bouton Regime
 * (Auto/Herbivore/Carnivore/Piscivore/Omnivore). Filtre de recherche en haut.
 * Chaque clic sauvegarde immediatement cote serveur.
 */
public class HabitatConfigScreen extends Screen {

    private static final int PANEL_W = 380, PANEL_H = 230, ROW_H = 22, VISIBLE = 7;
    private static final String[] DIETS = {"auto", "herbivore", "carnivore", "piscivore", "omnivore"};

    private final Map<String, HabitatManager.Entry> overrides;
    private final List<EntityType<?>> allTypes = new ArrayList<>();
    private List<EntityType<?>> filtered = new ArrayList<>();
    private int scroll = 0;
    private int leftPos, topPos;
    private EditBox search;

    public HabitatConfigScreen(Map<String, HabitatManager.Entry> overrides) {
        super(Component.translatable("gui.ultimatezootaming.habitats.title"));
        this.overrides = overrides;
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            if (type.getCategory() == MobCategory.CREATURE
                    || type.getCategory() == MobCategory.WATER_CREATURE
                    || type.getCategory() == MobCategory.WATER_AMBIENT
                    || type.getCategory() == MobCategory.AMBIENT) {
                allTypes.add(type);
            }
        }
        allTypes.sort((a, b) -> HabitatManager.key(a).compareTo(HabitatManager.key(b)));
        filtered = allTypes;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = (width - PANEL_W) / 2;
        topPos = (height - PANEL_H) / 2;
        search = new EditBox(font, leftPos + 12, topPos + 22, 200, 16, Component.empty());
        search.setResponder(txt -> {
            filtered = allTypes.stream().filter(t ->
                    HabitatManager.key(t).contains(txt.toLowerCase())).toList();
            scroll = 0;
            rebuild();
        });
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        addRenderableWidget(search);

        int maxScroll = Math.max(0, filtered.size() - VISIBLE);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int y0 = topPos + 44;

        for (int row = 0; row < VISIBLE; row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            EntityType<?> type = filtered.get(idx);
            String id = HabitatManager.key(type);
            HabitatManager.Entry e = overrides.getOrDefault(id, new HabitatManager.Entry(0, 0));
            int y = y0 + row * ROW_H;

            // Bouton habitat (cycle)
            addRenderableWidget(Button.builder(
                    Component.translatable("habitat.ultimatezootaming."
                            + HabitatProfile.values()[e.habitat()].name().toLowerCase()),
                    b -> {
                        int next = (e.habitat() + 1) % HabitatProfile.values().length;
                        set(id, next, e.diet());
                    }).bounds(leftPos + 200, y, 84, 18).build());

            // Bouton regime (cycle)
            addRenderableWidget(Button.builder(
                    Component.translatable("diet.ultimatezootaming." + DIETS[e.diet()]),
                    b -> {
                        int next = (e.diet() + 1) % DIETS.length;
                        set(id, e.habitat(), next);
                    }).bounds(leftPos + 288, y, 80, 18).build());
        }
        if (filtered.size() > VISIBLE) {
            Button up = Button.builder(Component.literal("\u25B2"), b -> { scroll--; rebuild(); })
                    .bounds(leftPos + PANEL_W - 20, y0, 14, 18).build();
            up.active = scroll > 0;
            addRenderableWidget(up);
            Button down = Button.builder(Component.literal("\u25BC"), b -> { scroll++; rebuild(); })
                    .bounds(leftPos + PANEL_W - 20, y0 + (VISIBLE - 1) * ROW_H, 14, 18).build();
            down.active = scroll < maxScroll;
            addRenderableWidget(down);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.ultimatezootaming.zones.close"),
                b -> onClose()).bounds(leftPos + PANEL_W / 2 - 50, topPos + PANEL_H - 24, 100, 20).build());
    }

    private void set(String id, int habitat, int diet) {
        overrides.put(id, new HabitatManager.Entry(habitat, diet));
        NetworkHandler.CHANNEL.sendToServer(new SaveHabitatC2SPacket(id, habitat, diet));
        rebuild();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(leftPos - 1, topPos - 1, leftPos + PANEL_W + 1, topPos + PANEL_H + 1, 0xFF000000);
        g.fill(leftPos, topPos, leftPos + PANEL_W, topPos + PANEL_H, 0xFFC6C6C6);
        int tw = font.width(title);
        g.drawString(font, title, leftPos + (PANEL_W - tw) / 2, topPos + 8, 0x404040, false);

        int y0 = topPos + 44;
        for (int row = 0; row < VISIBLE; row++) {
            int idx = scroll + row;
            if (idx >= filtered.size()) break;
            EntityType<?> type = filtered.get(idx);
            g.drawString(font, type.getDescription().getString(),
                    leftPos + 12, y0 + row * ROW_H + 5, 0x404040, false);
        }
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
