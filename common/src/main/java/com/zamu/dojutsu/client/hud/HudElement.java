package com.zamu.dojutsu.client.hud;

/**
 * Los elementos del HUD que el jugador puede recolocar (F11/DJT-233), con su sitio de fabrica y su tamano.
 *
 * <p>Los defaults reproducen exactamente donde estaban antes de que el HUD fuera movible (esquina superior
 * izquierda, apilados): quien no toque nada no debe notar el cambio.
 *
 * <p>El tamano se declara aqui, y no en cada overlay, porque lo necesitan dos sitios: el clamp que impide que
 * un elemento se salga de la pantalla y el editor, que dibuja su caja para poder arrastrarla.
 */
public enum HudElement {

    /** Barra de chakra + texto {@code actual / maximo}. */
    CHAKRA("chakra", 0.0F, 128, 8),

    /** Titulo del rango ninja. */
    RANK("rank", 0.0208F, 96, 10),

    /** Barra de enfriamiento del jutsu equipado. */
    ABILITY_BAR("ability_bar", 0.0417F, 128, 8);

    /**
     * Array cacheado de los valores. {@code values()} clona el array en cada llamada, y esto se recorre por
     * frame en el editor de HUD.
     */
    private static final HudElement[] VALUES = values();

    private final String key;
    private final int width;
    private final int height;
    private final HudPlacement defaultPlacement;

    HudElement(String key, float defaultOffsetY, int width, int height) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.defaultPlacement = new HudPlacement(HudAnchor.TOP_LEFT, 0.0078F, defaultOffsetY, true);
    }

    /** Los elementos sin clonar el array. Usar esto en cualquier bucle por frame. */
    public static HudElement[] all() {
        return VALUES;
    }

    /** Clave del elemento en el archivo de configuracion (estable: renombrarla resetea al jugador). */
    public String key() {
        return key;
    }

    /** Anchura aproximada en pixeles de GUI, incluida la etiqueta de texto que va al lado. */
    public int width() {
        return width;
    }

    /** Altura aproximada en pixeles de GUI. */
    public int height() {
        return height;
    }

    /**
     * Colocacion de fabrica: arriba a la izquierda y apilados, que es donde estaban con las coordenadas fijas
     * (chakra en y=10, rango en y=22, cd de jutsu en y=34 sobre una pantalla de 480 de alto tipica).
     *
     * <p>Se construye una sola vez en el constructor: {@code HudLayout.get} cae aqui en cada frame y para cada
     * elemento mientras el jugador no haya movido nada —que es el caso por defecto—, asi que devolver una
     * instancia nueva serian cientos de objetos por segundo para dar siempre la misma respuesta.
     * {@link HudPlacement} es inmutable, asi que compartir la instancia es seguro.
     */
    public HudPlacement defaultPlacement() {
        return defaultPlacement;
    }
}
