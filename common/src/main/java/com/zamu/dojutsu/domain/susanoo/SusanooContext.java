package com.zamu.dojutsu.domain.susanoo;

import com.zamu.dojutsu.domain.mode.ModeContext;

/**
 * Contexto del {@link SusanooMode}: lo que sus hooks necesitan del portador, y nada mas. Es la <b>costura</b>
 * que mantiene el modo en dominio puro — el glue (servicio) implementa esto sobre un {@code ServerPlayer} y sus
 * servicios de chakra/fatiga, y el modo nunca ve Minecraft.
 *
 * <p>Deliberadamente estrecho: el modo no sabe subir de nivel ni entregar XP (eso es progresion, del servicio);
 * solo sabe <b>sostenerse o caer</b>.
 */
public interface SusanooContext extends ModeContext {

    /** Nivel del Susanoo del portador (1..6); fija coste y proteccion. */
    int level();

    /** El ojo activo esta en su forma eterna (EMS): casi exime del coste. */
    boolean eternal();

    /** Ticks que faltan para el proximo cobro ({@code d.susanoo_time}); 0 = toca cobrar ya. */
    int pulseTimer();

    /** Fija los ticks que faltan para el proximo cobro. */
    void setPulseTimer(int ticks);

    /** Chakra disponible del portador (para la puerta de activacion, que no cobra). */
    int chakra();

    /** Cobra chakra todo-o-nada. {@code false} = no habia suficiente (el modo caera). */
    boolean consumeChakra(int amount);

    /** Anade fatiga ocular. */
    void addStrain(int amount);

    /** Aplica los efectos del nivel al portador este tick (proteccion, ignifugo, lastre). */
    void applyAura(SusanooTier tier);

    /** Avisa al portador de un cambio del modo ({@code true} = se encendio, {@code false} = se apago). */
    void notifyToggled(boolean active);
}
