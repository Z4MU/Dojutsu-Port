package com.zamu.dojutsu.domain.bijuu;

import com.zamu.dojutsu.domain.mode.ModeContext;
import java.util.Optional;

/**
 * Contexto de la {@link JinchurikiForm}: lo que sus hooks necesitan del portador. Costura que mantiene el modo en
 * dominio puro — el glue lo implementa sobre un {@code ServerPlayer}.
 */
public interface JinchurikiFormContext extends ModeContext {

    /** Bestia sellada, o vacio si no eres jinchuriki. */
    Optional<TailedBeast> beast();

    /** Nivel de vinculo 0..4; fija el aura y si tienes tecnicas. */
    int level();

    /** Chakra actual del portador. */
    int chakra();

    /** Ticks que faltan para poder volver a entrar en forma ({@code d.jin_formcd}). */
    int formCooldown();

    /** Fija el veto de entrada. */
    void setFormCooldown(int ticks);

    /**
     * Indica si el portador lleva el Rinnegan o el Rinne Sharingan <b>activo</b>. Solo importa al Diez Colas, que
     * exige uno de los dos para entrar y para <b>sostener</b> la forma ({@code form/main} la apaga si lo pierdes).
     */
    boolean hasRinneganActive();

    /** Suma o resta chakra maximo (la forma presta {@value JinchurikiFormTable#FORM_MAX_CHAKRA_BONUS}). */
    void addMaxChakra(int amount);

    /** Aplica el aura de este tick. */
    void applyAura(JinchurikiFormTable.FormAura aura);

    /** Indica si el Modo Baryon esta encendido; cambia el aura y el vuelo. */
    boolean baryonActive();

    /** Da o quita el vuelo (la forma del Diez Colas y el Baryon lo conceden). */
    void setFlight(boolean flying);

    /** Anuncia el encendido o apagado de la forma. */
    void notifyToggled(boolean active);

    /** Explica por que no se pudo entrar en forma. */
    void notifyBlocked(JinchurikiForm.BlockReason reason);
}
