package com.lex3d.ultimatezootaming.client.toasts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class BirthToast implements Toast {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/toasts.png");

    private final Component title;
    private final Component subtitle;
    private long firstDrawTime;
    private boolean justUpdated = true;

    public BirthToast(String babyDescriptionId) {
        this.title = Component.translatable("toast.ultimatezootaming.birth.title");
        this.subtitle = Component.translatable(babyDescriptionId);
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible) {
        if (justUpdated) {
            this.firstDrawTime = timeSinceLastVisible;
            this.justUpdated = false;
        }

        // Reutilise le sprite "recipe toast" vanilla (0,32 dans toasts.png) en attendant
        // un visuel dedie au mod.
        graphics.blit(TEXTURE, 0, 0, 0, 32, this.width(), this.height());

        graphics.drawString(toastComponent.getMinecraft().font, title, 18, 7, 0xFF500050, false);
        graphics.drawString(toastComponent.getMinecraft().font, subtitle, 18, 18, 0xFF000000, false);

        return timeSinceLastVisible - firstDrawTime < 5000L ? Visibility.SHOW : Visibility.HIDE;
    }
}

