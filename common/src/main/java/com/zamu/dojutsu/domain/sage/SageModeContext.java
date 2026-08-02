package com.zamu.dojutsu.domain.sage;

import com.zamu.dojutsu.domain.mode.ModeContext;

/**
 * Contexto del {@link SageMode}: lo que sus hooks necesitan del portador. Costura que mantiene el modo en
 * dominio puro — el glue lo implementa sobre un {@code ServerPlayer}.
 */
public interface SageModeContext extends ModeContext {

    /** Ya es sabio: sin esto no se puede encender el modo ({@code toggle_check}). */
    boolean isSage();

    /** Nivel de sabio 1..5; fija el minimo de activacion y el ritmo de drenaje. */
    int level();

    /** Energia natural reunida 0..100 ({@code d.sagechakra}). */
    int naturalEnergy();

    /** Gasta energia natural; {@code false} si no queda (el modo caera). */
    boolean consumeNaturalEnergy(int amount);

    /** Tiene chakra normal: el datapack apaga el modo si se queda a cero ({@code force_deactivate_1}). */
    boolean hasChakra();

    /** Lleva un sello maldito activo: <b>excluye</b> el modo sabio ({@code toggle_check}). */
    boolean cursedSealActive();

    /** Ticks que faltan para el proximo cobro ({@code d.sage_timer}); 0 = toca cobrar. */
    int drainTimer();

    /** Fija los ticks que faltan para el proximo cobro. */
    void setDrainTimer(int ticks);

    /** Aplica el aura del modo este tick (fuerza, velocidad, salto, resistencia). */
    void applyAura();

    /** Avisa al portador de un cambio del modo, o del motivo por el que no puede encenderlo. */
    void notifyToggled(boolean active);

    /** Avisa de que no puede encenderlo, con el motivo. */
    void notifyBlocked(SageMode.BlockReason reason);
}
