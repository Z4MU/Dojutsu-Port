package com.zamu.dojutsu.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Una pestaña del menú ninja (F11/DJT-236). <b>Solo cliente.</b>
 *
 * <p>Existe para que las tres pantallas de <b>preparación</b> —estadísticas, jutsus y ojo— vivan bajo una sola
 * tecla sin convertirse en una clase gigante: cada una sigue siendo su propio fichero, con su estado y su
 * lógica, y {@link NinjaMenuScreen} solo las contiene.
 *
 * <p>Una pestaña <b>no</b> es una {@code Screen}: no gestiona su fondo, ni el cierre, ni el desenfoque. Añade
 * sus widgets al contenedor en {@link #init} y dibuja lo suyo en {@link #render}. La regla del proyecto sigue
 * en pie: una pestaña solo lee cachés de cliente y envía intenciones; nunca decide gameplay.
 */
public interface MenuTab {

    /** Rótulo de la pestaña en la barra superior. */
    Component title();

    /**
     * Versión de la caché que observa esta pestaña. Cuando cambia, el contenedor la reconstruye.
     *
     * <p>Por defecto {@code 0}: una pestaña que solo lee {@code ClientNinjaData} no necesita reconstruirse
     * (repinta sus números en cada frame). Las que se dibujan con <b>widgets</b> a partir de una vista del
     * servidor sí, porque los botones ya creados no se enteran de que llegó otra vista.
     */
    default int observedVersion() {
        return 0;
    }

    /** Pide al servidor lo que necesite. Se llama <b>una vez</b>, al abrirse el menú en esta pestaña. */
    default void request() {
    }

    /** Crea los widgets de la pestaña. Se llama en cada reconstrucción (cambio de tamaño, de pestaña, o vista nueva). */
    void init(NinjaMenuScreen screen);

    /** Dibuja el contenido propio (lo que no son widgets). */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /** Pulso de cliente, para lo que dependa del tiempo y no de un repintado. */
    default void tick() {
    }

    /** Clic no consumido por ningún widget; {@code true} si la pestaña lo aprovecha. */
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /** Rueda del ratón; {@code true} si la pestaña la aprovecha. */
    default boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return false;
    }
}
