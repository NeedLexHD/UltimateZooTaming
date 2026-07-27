package com.lex3d.ultimatezootaming.client;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Barre de bien-etre dessinee a l'ecran (au-dessus de la hotbar), facon barre de
 * vie : fond sombre + remplissage colore + icone humeur + nom. Ne s'affiche QUE
 * quand on vise un familier (ClientWelfareCache.shouldRender), disparait net sinon.
 */
public class WelfareOverlay implements IGuiOverlay {

    public static final WelfareOverlay INSTANCE = new WelfareOverlay();

    private static final int BAR_W = 120;
    private static final int BAR_H = 8;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float partialTick, int screenW, int screenH) {
        if (Minecraft.getInstance().options.hideGui) return;
        if (!ClientWelfareCache.shouldRender()) return;

        int sat = ClientWelfareCache.satisfaction();
        boolean sick = ClientWelfareCache.sick();
        boolean inZone = ClientWelfareCache.inZone();

        int x = (screenW - BAR_W) / 2;
        int y = screenH - 68; // juste au-dessus de la hotbar + barres de vie/faim

        Minecraft mc = Minecraft.getInstance();

        // Nom du familier (+ trait s'il en a un) centre au-dessus de la barre
        String name = ClientWelfareCache.currentName();
        String trait = ClientWelfareCache.trait();
        Component nameComp;
        if (trait != null && !trait.equals("NONE")) {
            nameComp = Component.literal(name + " ")
                    .append(Component.translatable("trait.ultimatezootaming." + trait.toLowerCase())
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
        } else {
            nameComp = Component.literal(name);
        }
        int nameW = mc.font.width(nameComp);
        g.drawString(mc.font, nameComp, (screenW - nameW) / 2, y - 11, 0xFFFFFF, true);

        if (!inZone) {
            // Pas dans un enclos : petit texte gris, pas de barre
            Component msg = Component.translatable("message.ultimatezootaming.welfare_no_zone");
            int mw = mc.font.width(msg);
            g.drawString(mc.font, msg, (screenW - mw) / 2, y, 0xFFAAAAAA, true);
            return;
        }

        // Cadre + fond de barre
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xFF000000);
        g.fill(x, y, x + BAR_W, y + BAR_H, 0xFF3A3A3A);

        // Remplissage colore selon la satisfaction / maladie
        int color = sick ? 0xFFB44FD0
                : sat > 75 ? 0xFF4CAF50
                : sat < 25 ? 0xFFF44336
                : 0xFFFFC107;
        int fillW = (int) (BAR_W * (sat / 100.0));
        g.fill(x, y, x + fillW, y + BAR_H, color);
        // liseré clair en haut du remplissage (effet volume)
        if (fillW > 0) g.fill(x, y, x + fillW, y + 1, 0x66FFFFFF);

        // Texte humeur + pourcentage, centre dans la barre
        String moodKey = sick ? "mood_sick" : sat > 75 ? "mood_happy" : sat < 25 ? "mood_miserable" : "mood_neutral";
        Component label = Component.translatable("gui.ultimatezootaming.whistle." + moodKey)
                .copy().append(Component.literal("  " + sat + "%"));
        int lw = mc.font.width(label);
        g.drawString(mc.font, label, x + (BAR_W - lw) / 2, y - 0, 0xFFFFFFFF, true);
    }
}
