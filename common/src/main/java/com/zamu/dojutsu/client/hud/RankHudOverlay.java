package com.zamu.dojutsu.client.hud;

import com.zamu.dojutsu.client.ClientNinjaData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Overlay del rango ninja ({@code [S20]}): muestra el titulo del rango actual. <b>Solo cliente.</b> Lee de
 * {@link ClientNinjaData}; el rango se <b>deriva</b> de la XP ninja observada (no se transmite).
 */
public final class RankHudOverlay {

    private static final int X = 10;
    private static final int Y = 22;
    private static final int COLOR_RANK = 0xFFD700; // dorado

    private RankHudOverlay() {
    }

    /** Dibuja el rango. Los guards (espectador, F1, sin sync) los aplica {@link HudOverlays}. */
    public static void render(GuiGraphics graphics, Font font) {
        Component rank = Component.literal(ClientNinjaData.progression().rank().title());
        graphics.drawString(font, rank, X, Y, COLOR_RANK);
    }
}
