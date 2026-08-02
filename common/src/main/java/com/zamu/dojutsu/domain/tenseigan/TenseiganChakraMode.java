package com.zamu.dojutsu.domain.tenseigan;

import com.zamu.dojutsu.domain.mode.ToggleableMode;

/**
 * Tenseigan Chakra Mode {@code [S05]} como <b>modo activable</b> (DJT-125b, cierre de flecos de F5): el aura de
 * chakra del Tenseigan que envuelve al portador con buffs y le da <b>vuelo</b> mientras lo sostiene. Segundo
 * consumidor del framework {@link ToggleableMode} tras el Susanoo; misma mecanica de "se paga o cae".
 *
 * <p>Traduce {@code tenseigan/chakra_mode/main.mcfunction} + {@code toggle.mcfunction} al ciclo del framework:
 * <ul>
 *   <li>{@code onActivate}: veta si no hay chakra para sostenerlo ({@link #SUSTAIN_THRESHOLD}); el datapack lo
 *       apaga en cuanto {@code chakra < 19}.</li>
 *   <li>{@code onTick}: aplica el aura (Fuerza II, Velocidad II, Salto IV, Resistencia II) y el vuelo.</li>
 *   <li>{@code onCost}: cae si el chakra baja del umbral; si no, cobra <b>por pulsos</b> — 10 de chakra cada
 *       {@link #PULSE_TICKS} ticks ({@code tenseicm_cc}).</li>
 *   <li>{@code onDeactivate}: retira el vuelo y avisa.</li>
 * </ul>
 *
 * <p>Sin fatiga: el Tenseigan no anade {@code eye_strain} (consistente con sus habilidades). Dominio puro: todo
 * lo del jugador pasa por {@link TenseiganChakraModeContext}, asi que el ciclo se testea sin arrancar Minecraft.
 */
public final class TenseiganChakraMode extends ToggleableMode<TenseiganChakraModeContext> {

    /** Chakra minimo para sostener el modo; por debajo, cae ({@code unless chakra matches 19..}). */
    public static final int SUSTAIN_THRESHOLD = 19;

    /** Chakra que cuesta cada pulso de mantenimiento. */
    public static final int CHAKRA_PER_PULSE = 10;

    /** Cada cuantos ticks se cobra el pulso ({@code d.tenseicm_cc = 200}). */
    public static final int PULSE_TICKS = 200;

    @Override
    protected boolean onActivate(TenseiganChakraModeContext ctx) {
        if (ctx.chakra() < SUSTAIN_THRESHOLD) {
            return false; // no hay chakra ni para sostener el primer instante
        }
        ctx.setPulseTimer(0); // el primer pulso se cobra en el tick siguiente
        ctx.setFlight(true);
        ctx.notifyToggled(true);
        return true;
    }

    @Override
    protected void onTick(TenseiganChakraModeContext ctx) {
        ctx.applyAura();
    }

    @Override
    protected boolean onCost(TenseiganChakraModeContext ctx) {
        if (ctx.chakra() < SUSTAIN_THRESHOLD) {
            return false; // cae: el datapack lo apaga en cuanto baja del umbral
        }
        int remaining = ctx.pulseTimer();
        if (remaining > 0) {
            ctx.setPulseTimer(remaining - 1);
            return true; // entre pulsos no se cobra
        }
        if (!ctx.consumeChakra(CHAKRA_PER_PULSE)) {
            return false; // sin chakra para el pulso: cae
        }
        ctx.setPulseTimer(PULSE_TICKS);
        return true;
    }

    @Override
    protected void onDeactivate(TenseiganChakraModeContext ctx) {
        ctx.setFlight(false);
        ctx.notifyToggled(false);
    }
}
