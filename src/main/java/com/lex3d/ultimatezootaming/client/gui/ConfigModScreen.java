package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.config.ZooClientConfig;
import com.lex3d.ultimatezootaming.core.network.ConfigSyncC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * "Interface visuelle listant tous les mods charges avec des Checkboxes pour les
 *  ajouter a la liste Forced Mods. Synchronisation automatique Client -> Serveur."
 *
 * Accessible via le bouton "Config" dans l'ecran des mods Forge natif (voir
 * UltimateZooTame#registerConfigScreen).
 *
 * NOTE : la classe Checkbox a change de forme (constructeur vs builder) selon les
 * versions mineures de Forge/MC 1.20.x. On utilise ici le constructeur direct
 * (le plus stable dans le temps) et on relit l'etat de chaque case via .selected()
 * au moment d'enregistrer, plutot qu'un callback onValueChange dont la signature
 * exacte varie. Si ton IDE indique un constructeur different, adapte juste la
 * ligne "new Checkbox(...)" ci-dessous.
 */
public class ConfigModScreen extends Screen {

    private final Set<String> initiallyChecked = new HashSet<>();
    private final List<String> allModIds = new ArrayList<>();
    private final Map<String, Checkbox> checkboxByModId = new HashMap<>();
    @Nullable
    private final Screen parent;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 20;
    private static final int VISIBLE_ROWS = 10;

    public ConfigModScreen(@Nullable Screen parent) {
        super(Component.translatable("gui.ultimatezootaming.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        allModIds.clear();
        ModList.get().getMods().forEach(info -> allModIds.add(info.getModId()));
        initiallyChecked.clear();
        initiallyChecked.addAll(ZooClientConfig.FORCED_MOD_IDS.get());

        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        checkboxByModId.clear();

        int visibleCount = Math.min(VISIBLE_ROWS, allModIds.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            String modId = allModIds.get(scrollOffset + i);
            int y = 30 + i * ROW_HEIGHT;

            Checkbox checkbox = new Checkbox(20, y, 200, 20,
                    Component.literal(modId), initiallyChecked.contains(modId));
            checkboxByModId.put(modId, checkbox);
            addRenderableWidget(checkbox);
        }

        if (scrollOffset > 0) {
            addRenderableWidget(Button.builder(Component.literal("^"), b -> {
                saveScrollState();
                scrollOffset = Math.max(0, scrollOffset - VISIBLE_ROWS);
                rebuildButtons();
            }).bounds(this.width - 60, 30, 20, 20).build());
        }
        if (scrollOffset + VISIBLE_ROWS < allModIds.size()) {
            addRenderableWidget(Button.builder(Component.literal("v"), b -> {
                saveScrollState();
                scrollOffset = Math.min(allModIds.size() - 1, scrollOffset + VISIBLE_ROWS);
                rebuildButtons();
            }).bounds(this.width - 60, 260, 20, 20).build());
        }

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimatezootaming.config.save"),
                        b -> saveAndSync())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20)
                .build());
    }

    /** Persiste l'etat des cases visibles avant de changer de page (scroll). */
    private void saveScrollState() {
        checkboxByModId.forEach((modId, checkbox) -> {
            if (checkbox.selected()) initiallyChecked.add(modId);
            else initiallyChecked.remove(modId);
        });
    }

    private void saveAndSync() {
        saveScrollState();

        ZooClientConfig.FORCED_MOD_IDS.set(new ArrayList<>(initiallyChecked));
        ZooClientConfig.FORCED_MOD_IDS.save();

        // On ne peut envoyer un packet au serveur que si on est effectivement connecte
        // a un monde (ce GUI est aussi accessible depuis l'ecran-titre, hors connexion).
        // Si pas connecte, la sync se fera automatiquement a la prochaine connexion
        // (voir ClientForgeBusEvents#onPlayerLoggingIn).
        if (Minecraft.getInstance().getConnection() != null) {
            NetworkHandler.CHANNEL.sendToServer(new ConfigSyncC2SPacket(new ArrayList<>(initiallyChecked)));
        }

        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}


