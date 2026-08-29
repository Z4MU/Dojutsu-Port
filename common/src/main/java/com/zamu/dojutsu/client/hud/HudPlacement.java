package com.zamu.dojutsu.client.hud;

import java.util.Objects;

/**
 * Donde va un elemento del HUD (F11/DJT-233): su anclaje, el desplazamiento desde el, y si se dibuja.
 *
 * <p><b>El desplazamiento va en fraccion de pantalla, no en pixeles</b> (0.0 a 1.0). Es lo que permite que la
 * colocacion del jugador sobreviva a un cambio de resolucion o de escala de GUI. Ver {@link HudAnchor}.
 *
 * <p>Value object inmutable, como el resto del estado del mod: para cambiar algo se reemplaza.
 *
 * @param anchor  esquina desde la que se mide
 * @param offsetX desplazamiento horizontal en fraccion de la anchura de pantalla
 * @param offsetY desplazamiento vertical en fraccion de la altura de pantalla
 * @param visible si el elemento se dibuja (hay quien solo quiere la barra de chakra)
 */
public record HudPlacement(HudAnchor anchor, float offsetX, float offsetY, boolean visible) {

    public HudPlacement {
        Objects.requireNonNull(anchor, "anchor");
        offsetX = clampOffset(offsetX);
        offsetY = clampOffset(offsetY);
    }

    /** Copia con otro anclaje. */
    public HudPlacement withAnchor(HudAnchor value) {
        return new HudPlacement(value, offsetX, offsetY, visible);
    }

    /** Copia con otro desplazamiento. */
    public HudPlacement withOffset(float x, float y) {
        return new HudPlacement(anchor, x, y, visible);
    }

    /** Copia mostrando u ocultando el elemento. */
    public HudPlacement withVisible(boolean value) {
        return new HudPlacement(anchor, offsetX, offsetY, value);
    }

    /**
     * Resuelve la coordenada X en pixeles, dejando el elemento <b>dentro</b> de la pantalla.
     *
     * @param screenWidth  anchura de la pantalla en pixeles de GUI
     * @param elementWidth anchura del elemento en pixeles de GUI
     */
    public int resolveX(int screenWidth, int elementWidth) {
        float raw = anchor.originX() * screenWidth
                + anchor.widthBias() * elementWidth
                + offsetX * screenWidth;
        return clampToScreen(Math.round(raw), screenWidth, elementWidth);
    }

    /**
     * Resuelve la coordenada Y en pixeles, dejando el elemento <b>dentro</b> de la pantalla.
     *
     * @param screenHeight  altura de la pantalla en pixeles de GUI
     * @param elementHeight altura del elemento en pixeles de GUI
     */
    public int resolveY(int screenHeight, int elementHeight) {
        float raw = anchor.originY() * screenHeight
                + anchor.heightBias() * elementHeight
                + offsetY * screenHeight;
        return clampToScreen(Math.round(raw), screenHeight, elementHeight);
    }

    /**
     * Desplazamiento (en fraccion) que deja el elemento en una posicion de pantalla concreta: es la operacion
     * inversa de {@code resolve}, la que necesita el editor cuando el jugador suelta el raton.
     */
    public HudPlacement movedTo(int pixelX, int pixelY, int screenWidth, int screenHeight,
            int elementWidth, int elementHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return this;
        }
        float x = (pixelX - anchor.originX() * screenWidth - anchor.widthBias() * elementWidth) / screenWidth;
        float y = (pixelY - anchor.originY() * screenHeight - anchor.heightBias() * elementHeight) / screenHeight;
        return withOffset(x, y);
    }

    /** Mantiene el desplazamiento en un rango con sentido: fuera de [-1, 1] el elemento no estaria en pantalla. */
    private static float clampOffset(float value) {
        if (Float.isNaN(value)) {
            return 0.0F;
        }
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    /**
     * Impide que un elemento quede fuera de la pantalla. Es la red de seguridad de un config editado a mano o
     * de una resolucion mucho menor que aquella en la que se coloco.
     */
    private static int clampToScreen(int value, int screenSize, int elementSize) {
        int max = Math.max(0, screenSize - elementSize);
        return Math.max(0, Math.min(value, max));
    }
}
