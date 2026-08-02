package com.zamu.dojutsu.domain.soah;

import com.zamu.dojutsu.domain.mode.ModeContext;

/**
 * Contexto del {@link SoahMode}: lo que sus hooks necesitan del portador. Costura que mantiene el modo en
 * dominio puro — el glue lo implementa sobre un {@code ServerPlayer}.
 */
public interface SoahModeContext extends ModeContext {

    /** Ya recibio el sello ({@code d.soah_gave}); sin el no hay nada que activar. */
    boolean hasSeal();

    /** Chakra guardado en el sello ({@code d.soah_chakra}). */
    int storedChakra();

    /** Chakra actual del ninja. */
    int chakra();

    /** Chakra maximo del ninja; fija tanto un requisito de uso como el techo al que cura el sello. */
    int maxChakra();

    /** Nivel de Ninjutsu Medico en escala del mod. */
    int medicalLevel();

    /** Nivel de afinidad Yin (escala 0-500, sin reescalar). */
    int yinAffinity();

    /** El sello maldito esta activo: son <b>excluyentes</b> ({@code toggle}). */
    boolean cursedSealActive();

    /** Ticks que faltan para poder volver a alternar el sello ({@code d.soah_cd}). */
    int cooldown();

    /** Fija el veto de alternancia. */
    void setCooldown(int ticks);

    /** Mueve {@code amount} de la reserva del sello al chakra del ninja ({@code heal_*}). */
    void transferFromSeal(int amount);

    /** Aura del sello mientras esta activo. */
    void emitAura();

    /** Anuncia el encendido, informando de la reserva disponible. */
    void notifyActivated(int storedChakra);

    /** Anuncia el apagado. */
    void notifyDeactivated();

    /** Informa de la reserva restante al terminar de curar ({@code stop_heals}). */
    void notifyStored(int storedChakra);

    /** Avisa de que la reserva se ha agotado y explica como recargarla ({@code notify}). */
    void notifyOutOfStoredChakra();

    /** Explica por que no se pudo encender el sello. */
    void notifyBlocked(SoahMode.BlockReason reason);
}
