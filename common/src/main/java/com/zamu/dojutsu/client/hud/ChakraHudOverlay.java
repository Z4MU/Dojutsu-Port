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
 */
public final class ChakraHudOverlay {

    private static final int X = 10;
    private static final int Y = 10;
    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 6;

    private static final int COLOR_BORDER = 0xFF303030;
    private static final int COLOR_BACKGROUND = 0xAA000000;
    private static final int COLOR_CHAKRA = 0xFF29B6F6; // azul chakra

    private ChakraHudOverlay() {
    }

    /** Dibuja la barra de chakra. Los guards (espectador, F1, sin sync) los aplica {@link HudOverlays}. */
    public static void render(GuiGraphics graphics, Font font) {
        int chakra = ClientNinjaData.chakra();
        int maxChakra = ClientNinjaData.maxChakra();
        int filled = maxChakra > 0
                ? Math.round(BAR_WIDTH * Math.min(1.0F, (float) chakra / maxChakra))
                : 0;

        graphics.fill(X - 1, Y - 1, X + BAR_WIDTH + 1, Y + BAR_HEIGHT + 1, COLOR_BORDER);
        graphics.fill(X, Y, X + BAR_WIDTH, Y + BAR_HEIGHT, COLOR_BACKGROUND);
        graphics.fill(X, Y, X + filled, Y + BAR_HEIGHT, COLOR_CHAKRA);

        String text = HudFormat.compact(chakra) + " / " + HudFormat.compact(maxChakra);
        graphics.drawString(font, text, X + BAR_WIDTH + 6, Y - 1, 0xFFFFFF);
    }
}
