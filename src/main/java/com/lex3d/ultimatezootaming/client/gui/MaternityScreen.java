package com.lex3d.ultimatezootaming.client.gui;

import com.lex3d.ultimatezootaming.client.ClientSetup;
import com.lex3d.ultimatezootaming.core.network.AdoptBabyC2SPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Une naissance en attente = un choix : donner un nom + adopter (rejoint le Sifflet),
 * ou laisser sauvage (il restera pour l'elevage de viande, comme precise dans le doc).
 */
public class MaternityScreen extends Screen {

    private final List<ClientSetup.PendingBaby> pendingBabies;
    private int currentIndex = 0;
    private EditBox nameBox;

    public MaternityScreen(List<ClientSetup.PendingBaby> pendingBabies) {
        super(Component.translatable("gui.ultimatezootaming.maternity.title"));
        this.pendingBabies = pendingBabies;
    }

    @Override
    protected void init() {
        super.init();
        if (pendingBabies.isEmpty()) {
            this.minecraft.setScreen(null);
            return;
        }

        nameBox = new EditBox(font, this.width / 2 - 100, 70, 200, 20,
                Component.translatable("gui.ultimatezootaming.maternity.name"));
        nameBox.setMaxLength(32);
        addRenderableWidget(nameBox);

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimatezootaming.maternity.adopt"),
                        b -> resolveCurrent(true))
                .bounds(this.width / 2 - 105, 110, 100, 20)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.ultimatezootaming.maternity.leave_wild"),
                        b -> resolveCurrent(false))
                .bounds(this.width / 2 + 5, 110, 100, 20)
                .build());
    }

    private void resolveCurrent(boolean adopt) {
        ClientSetup.PendingBaby baby = pendingBabies.get(currentIndex);
        NetworkHandler.CHANNEL.sendToServer(
                new AdoptBabyC2SPacket(baby.uuid(), nameBox.getValue(), adopt));
        ClientSetup.clearPendingBaby(baby.uuid());

        pendingBabies.remove(currentIndex);
        if (pendingBabies.isEmpty()) {
            this.minecraft.setScreen(null);
        } else {
            currentIndex = 0;
            nameBox.setValue("");
            init();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        if (!pendingBabies.isEmpty()) {
            ClientSetup.PendingBaby baby = pendingBabies.get(currentIndex);
            graphics.drawCenteredString(this.font,
                    Component.translatable(baby.descriptionId()),
                    this.width / 2, 40, 0xFFFF55);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
