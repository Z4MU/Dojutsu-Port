package com.zamu.dojutsu.client.hud;

import com.zamu.dojutsu.client.ClientNinjaData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Overlay del rango ninja ({@code [S20]}): muestra el titulo del rango actual. <b>Solo cliente.</b> Lee de
 * {@link ClientNinjaData}; el rango se <b>deriva</b> de la XP ninja observada (no se transmite).
 *
 * <p>Su posicion la elige el jugador (F11/DJT-233): sale de {@link HudLayout}, no de constantes.
 */
public final class RankHudOverlay {

    private static final int COLOR_RANK = 0xFFD700; // dorado

    private RankHudOverlay() {
    }

    /** Dibuja el rango. Los guards (espectador, F1, sin sync) los aplica {@link HudOverlays}. */
    public static void render(GuiGraphics graphics, Font font) {
        HudPlacement placement = HudLayout.get(HudElement.RANK);
        if (!placement.visible()) {
            return;
        }
        render(graphics, font,
                placement.resolveX(graphics.guiWidth(), HudElement.RANK.width()),
                placement.resolveY(graphics.guiHeight(), HudElement.RANK.height()));
    }

    /** Dibuja el rango en una posicion dada. Lo usa tambien el editor de HUD para la vista previa. */
    public static void render(GuiGraphics graphics, Font font, int x, int y) {
        Component rank = Component.literal(ClientNinjaData.progression().rank().title());
        graphics.drawString(font, rank, x, y, COLOR_RANK);
    }
}
