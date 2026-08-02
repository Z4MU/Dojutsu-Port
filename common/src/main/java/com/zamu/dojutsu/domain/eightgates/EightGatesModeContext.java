package com.zamu.dojutsu.domain.eightgates;

import com.zamu.dojutsu.domain.mode.ModeContext;

/**
 * Contexto del {@link EightGatesMode}: lo que sus hooks necesitan del ninja. Costura que mantiene el modo en
 * dominio puro — el glue lo implementa sobre un {@code ServerPlayer}.
 *
 * <p>La fatiga solo se <b>consulta</b> y se <b>suma</b> desde aqui: la recuperacion pasiva y el castigo por
 * banda ocurren tambien con las puertas cerradas, asi que viven en el servicio, no en el modo.
 */
public interface EightGatesModeContext extends ModeContext {

    /** Nivel de taijutsu en escala del mod; fija que puertas puede abrir y si las domina. */
    int taijutsuLevel();

    /** Fatiga corporal actual 0..100 ({@code d.eg_strain}). */
    int strain();

    /** Suma fatiga corporal (clampada por el estado). */
    void addStrain(int amount);

    /** Ticks que faltan para el proximo mordisco de fatiga ({@code d.eg_stimer}); 0 = toca. */
    int strainTimer();

    /** Fija los ticks que faltan para el proximo mordisco de fatiga. */
    void setStrainTimer(int ticks);

    /** Ticks que faltan para poder <b>encadenar</b> otra puerta ({@code d.eg_opencd}). */
    int openCooldown();

    /** Fija el veto entre aperturas. */
    void setOpenCooldown(int ticks);

    /** Ticks que faltan para poder volver a abrir tras haber cerrado ({@code d.eg_cd}). */
    int closeCooldown();

    /** Fija el veto tras cerrar. */
    void setCloseCooldown(int ticks);

    /** Aplica los efectos de la puerta abierta este tick (fuerza/velocidad/salto y particulas). */
    void applyGateEffects(Gate gate);

    /** Anuncia que se ha abierto una puerta. */
    void notifyOpened(Gate gate);

    /**
     * Anuncia el cierre y aplica el <b>coste de cerrar</b> ({@code deactivate}): con la fatiga por encima de 95
     * duele, y por encima de 98 duele el doble. Con poca fatiga, cerrar es gratis.
     *
     * @param strainAtClose fatiga en el momento de cerrar
     */
    void notifyClosed(int strainAtClose);

    /** Explica por que no se pudo abrir la siguiente puerta. */
    void notifyBlocked(EightGatesMode.BlockReason reason);
}
