package com.zamu.dojutsu.domain.cursedseal;

import com.zamu.dojutsu.domain.mode.ModeContext;
import java.util.Optional;

/**
 * Contexto del {@link CursedSealMode}: lo que sus hooks necesitan del portador. Costura que mantiene el modo en
 * dominio puro — el glue lo implementa sobre un {@code ServerPlayer}.
 */
public interface CursedSealModeContext extends ModeContext {

    /** Sello que porta, o vacio si no tiene ninguno (sin sello no hay modo que activar). */
    Optional<CursedSealType> seal();

    /** Nivel del sello 1..2; fija el coste y la potencia del aura. */
    int level();

    /** Cobra chakra todo-o-nada; {@code false} si no hay suficiente (el modo castigara). */
    boolean consumeChakra(int amount);

    /** Ticks que faltan para el proximo cobro ({@code d.cseal_timer}); 0 = toca cobrar. */
    int drainTimer();

    /** Fija los ticks que faltan para el proximo cobro. */
    void setDrainTimer(int ticks);

    /** Aplica el aura del sello este tick (fuerza/velocidad/salto/resistencia segun tipo y nivel). */
    void applyAura(CursedSealType type, int level);

    /**
     * <b>Castigo:</b> el sello consume a su portador — lo <b>mata</b> ({@code punish}) y, si procede, degrada el
     * sello. Es el unico automatismo del framework: coste impagable → castigo + apagado.
     */
    void consumeBearer();

    /** Avisa al portador de un cambio del modo ({@code true} = se activo). */
    void notifyToggled(boolean active);
}
