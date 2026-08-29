package com.zamu.dojutsu.client.hud;

import com.zamu.dojutsu.client.ClientNinjaData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Overlay del chakra ({@code [S02]}/{@code [S13]}): barra que refleja {@code chakra/maxChakra} en tiempo real.
 * <b>Solo cliente.</b> Lee de {@link ClientNinjaData} (cache read-only alimentada por el sync); nunca consulta
 * al servidor ni decide gameplay.
 *
 * <p>Se dibuja con primitivas (sin texturas todavia): los assets reales llegan con {@code EP-ASSETS}.
 *
 * <p>Su posicion la elige el jugador (F11/DJT-233): sale de {@link HudLayout}, no de constantes.
 */
public final class ChakraHudOverlay {

    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 6;

    private static final int COLOR_BORDER = 0xFF303030;
    private static final int COLOR_BACKGROUND = 0xAA000000;
    private static final int COLOR_CHAKRA = 0xFF29B6F6; // azul chakra

    private ChakraHudOverlay() {
    }

    /** Dibuja la barra de chakra. Los guards (espectador, F1, sin sync) los aplica {@link HudOverlays}. */
    public static void render(GuiGraphics graphics, Font font) {
        HudPlacement placement = HudLayout.get(HudElement.CHAKRA);
        if (!placement.visible()) {
            return;
        }
        render(graphics, font,
                placement.resolveX(graphics.guiWidth(), HudElement.CHAKRA.width()),
                placement.resolveY(graphics.guiHeight(), HudElement.CHAKRA.height()));
    }

    /** Dibuja la barra en una posicion dada. Lo usa tambien el editor de HUD para la vista previa. */
    public static void render(GuiGraphics graphics, Font font, int x, int y) {
        int chakra = ClientNinjaData.chakra();
        int maxChakra = ClientNinjaData.maxChakra();
        int filled = maxChakra > 0
                ? Math.round(BAR_WIDTH * Math.min(1.0F, (float) chakra / maxChakra))
                : 0;

        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BORDER);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_BACKGROUND);
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, COLOR_CHAKRA);

        String text = HudFormat.compact(chakra) + " / " + HudFormat.compact(maxChakra);
        graphics.drawString(font, text, x + BAR_WIDTH + 6, y - 1, 0xFFFFFF);
    }
}
