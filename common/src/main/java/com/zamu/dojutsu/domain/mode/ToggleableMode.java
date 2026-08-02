package com.zamu.dojutsu.domain.mode;

/**
 * Framework abstracto de "modo activable con coste/castigo" (patron 2 del ADD).
 *
 * <p>Base compartida por sage {@code [S08]}, sellos malditos {@code [S10]}, ocho puertas {@code [S11]},
 * SOAH {@code [S12]}, susanoo {@code [S24]} y formas de jinchuriki {@code [S09]}. Define solo el
 * <b>ciclo</b> y los <b>hooks</b>; cada modo concreto implementa unicamente su diferencia (coste,
 * efectos, castigo), sin que el framework conozca su logica.
 *
 * <h2>Ciclo</h2>
 * <pre>
 *   activate -> onActivate (puede vetar)
 *   tick     -> onTick -> onCost -> (si no paga: onPunish + deactivate)
 *   deactivate -> onDeactivate
 * </pre>
 *
 * <p>El framework solo automatiza el caso "no se puede sostener el modo" (coste impagable ->
 * castigo + apagado). Otros matices (letal en la 8a puerta, inyeccion probabilistica de sellos,
 * autocuracion por tiers de SOAH, castigos por umbral de strain) los dispara el propio modo desde sus
 * hooks (p. ej. en {@link #onTick} consultando su {@code StrainMeter}), llamando si procede a
 * {@link #deactivate}. Dominio puro: sin imports de Minecraft.
 *
 * @param <C> tipo de contexto que necesita este modo (evita casts y fugas entre modos)
 */
public abstract class ToggleableMode<C extends ModeContext> {

    private boolean active;

    /** Indica si el modo esta activo. */
    public final boolean isActive() {
        return active;
    }

    /**
     * Intenta activar el modo. No hace nada si ya esta activo o si {@link #onActivate} lo veta.
     *
     * @return {@code true} si se activo en esta llamada
     */
    public final boolean activate(C ctx) {
        if (active) {
            return false;
        }
        if (!onActivate(ctx)) {
            return false;
        }
        active = true;
        return true;
    }

    /** Desactiva el modo (limpieza via {@link #onDeactivate}). No hace nada si ya estaba inactivo. */
    public final void deactivate(C ctx) {
        if (!active) {
            return;
        }
        active = false;
        onDeactivate(ctx);
    }

    /**
     * Alterna el estado: desactiva si esta activo, o intenta activar si no.
     *
     * @return el estado resultante ({@code true} = activo)
     */
    public final boolean toggle(C ctx) {
        if (active) {
            deactivate(ctx);
            return false;
        }
        return activate(ctx);
    }

    /**
     * Ejecuta un tick del modo si esta activo: efectos, luego cobro del coste; si el coste no puede
     * pagarse, castiga y desactiva. No hace nada si el modo esta inactivo.
     */
    public final void tick(C ctx) {
        if (!active) {
            return;
        }
        onTick(ctx);
        if (!onCost(ctx)) {
            onPunish(ctx);
            deactivate(ctx);
        }
    }

    // --- Hooks (los matices de cada modo; por defecto, no-op / gratis / sin veto) ---

    /**
     * Preparacion al activar. Devuelve {@code false} para vetar la activacion (p. ej. sin recurso
     * suficiente o sin desbloqueo). Por defecto permite activar.
     */
    protected boolean onActivate(C ctx) {
        return true;
    }

    /** Efectos del modo por tick mientras esta activo. Por defecto no hace nada. */
    protected void onTick(C ctx) {
    }

    /**
     * Cobra el coste de sostener el modo este tick. Devuelve {@code false} si no puede pagarse (lo que
     * provoca {@link #onPunish} y el apagado). Por defecto el modo es gratuito ({@code true}).
     */
    protected boolean onCost(C ctx) {
        return true;
    }

    /** Castigo cuando el modo no puede sostenerse. Por defecto no hace nada (solo se desactivara). */
    protected void onPunish(C ctx) {
    }

    /** Limpieza al desactivar. Por defecto no hace nada. */
    protected void onDeactivate(C ctx) {
    }
}
