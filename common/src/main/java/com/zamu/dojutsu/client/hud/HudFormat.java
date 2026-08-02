package com.zamu.dojutsu.client.hud;

import java.util.Locale;

/**
 * Formato de numeros del HUD. Los valores del mod pueden crecer mucho ({@code [ADD/08 §4]}: nivel hasta 500,
 * chakra de Jinchuriki +8000), asi que se compactan para no romper el layout.
 *
 * <p>Clase <b>pura</b> (sin Minecraft): testeable.
 */
public final class HudFormat {

    private HudFormat() {
    }

    /**
     * Formato compacto: {@code 999 -> "999"}, {@code 8000 -> "8.0k"}, {@code 1500000 -> "1.5M"}. Los valores
     * por debajo de 1000 (y los negativos, que no deberian darse) se muestran tal cual.
     */
    public static String compact(int value) {
        if (value < 1_000) {
            return Integer.toString(value);
        }
        if (value < 1_000_000) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0);
        }
        return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
    }
}
