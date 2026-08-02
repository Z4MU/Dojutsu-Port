package com.zamu.dojutsu.domain.tenseigan;

import com.zamu.dojutsu.domain.mode.ModeContext;

/**
 * Contexto del {@link TenseiganChakraMode}: lo que sus hooks necesitan del portador, y nada mas. La costura que
 * mantiene el modo en dominio puro — el glue lo implementa sobre un {@code ServerPlayer}.
 */
public interface TenseiganChakraModeContext extends ModeContext {

    /** Chakra disponible del portador (para la puerta de sostenimiento). */
    int chakra();

    /** Cobra chakra todo-o-nada. {@code false} = no habia suficiente. */
    boolean consumeChakra(int amount);

    /** Ticks que faltan para el proximo cobro del pulso ({@code d.tenseicm_cc}); 0 = toca cobrar ya. */
    int pulseTimer();

    /** Fija los ticks que faltan para el proximo cobro. */
    void setPulseTimer(int ticks);

    /** Aplica el aura del modo este tick: buffs (fuerza/velocidad/salto/resistencia) y vuelo. */
    void applyAura();

    /** Concede o retira el vuelo del portador (el modo lo da mientras esta activo). */
    void setFlight(boolean allowed);

    /** Avisa al portador de un cambio del modo ({@code true} = se encendio, {@code false} = se apago). */
    void notifyToggled(boolean active);
}
