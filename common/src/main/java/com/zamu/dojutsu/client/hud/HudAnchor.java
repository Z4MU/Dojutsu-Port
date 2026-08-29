package com.zamu.dojutsu.client.hud;

/**
 * Esquina (o borde) de la pantalla desde la que se mide la posicion de un elemento del HUD (F11/DJT-233).
 *
 * <p><b>Por que anclaje y no coordenadas absolutas.</b> Un HUD colocado en pixeles se descoloca en cuanto
 * cambia la resolucion o la escala de GUI: lo que estaba pegado abajo a la derecha en 1080p aparece flotando
 * en medio en 1440p. Con un anclaje, el elemento se mide siempre desde su esquina y se queda donde el jugador
 * lo dejo.
 */
public enum HudAnchor {

    TOP_LEFT(0.0F, 0.0F),
    TOP_CENTER(0.5F, 0.0F),
    TOP_RIGHT(1.0F, 0.0F),
    BOTTOM_LEFT(0.0F, 1.0F),
    BOTTOM_CENTER(0.5F, 1.0F),
    BOTTOM_RIGHT(1.0F, 1.0F);

    private final float originX;
    private final float originY;

    HudAnchor(float originX, float originY) {
        this.originX = originX;
        this.originY = originY;
    }

    /** Fraccion horizontal de la pantalla donde nace el anclaje (0 = izquierda, 1 = derecha). */
    public float originX() {
        return originX;
    }

    /** Fraccion vertical de la pantalla donde nace el anclaje (0 = arriba, 1 = abajo). */
    public float originY() {
        return originY;
    }

    /**
     * El elemento se dibuja hacia dentro de la pantalla: en los anclajes de la derecha su ancho se resta, y en
     * los del centro se resta la mitad. Sin esto, un elemento anclado a la derecha se saldria por el borde.
     */
    public float widthBias() {
        return -originX;
    }

    /** Igual que {@link #widthBias()} para la vertical: abajo se resta la altura. */
    public float heightBias() {
        return -originY;
    }

    /** Anclaje por nombre, tolerante: si no existe (config a mano o de otra version) devuelve el de reserva. */
    public static HudAnchor byName(String name, HudAnchor fallback) {
        if (name != null) {
            for (HudAnchor anchor : values()) {
                if (anchor.name().equalsIgnoreCase(name.trim())) {
                    return anchor;
                }
            }
        }
        return fallback;
    }
}
