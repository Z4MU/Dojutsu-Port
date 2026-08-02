package com.zamu.dojutsu.domain.soah;

import com.zamu.dojutsu.domain.mode.ToggleableMode;

/**
 * Sello de la Fuerza de un Centenar {@code [S12]} (F6/DJT-150) como {@link ToggleableMode}: el <b>unico modo que
 * te devuelve recurso en vez de cobrartelo</b>. Sexto y ultimo consumidor del framework en F6.
 *
 * <p>Los otros cinco gastan mientras estan encendidos —chakra el sello maldito, energia natural el sabio, fatiga
 * las puertas—. Este ya cobro por adelantado: la reserva se llena a mano ({@code recharge}) y el modo se limita a
 * devolverla cuando hace falta. Por eso {@code onCost} aqui no descuenta nada; solo comprueba que <b>quede</b>
 * algo que devolver.
 *
 * <p>Traduce {@code soah_seal/main} + {@code run} + {@code heal}:
 * <ul>
 *   <li>{@code onActivate}: veta por sello maldito activo, requisitos (medicina, Yin, chakra maximo), veto de
 *       alternancia o reserva vacia.</li>
 *   <li>{@code onTick}: si el chakra baja de {@value SoahTable#HEAL_TRIGGER_CHAKRA} entra a curar, y no para
 *       hasta dejarte al maximo, devolviendo por tramos ({@link SoahTable#healTier}).</li>
 *   <li>{@code onCost}: cae si te quedas a cero de chakra ({@code no_chakra}) o si la reserva se agota mientras
 *       estaba curando.</li>
 *   <li>{@code onDeactivate}: veta {@value SoahTable#DEACTIVATE_COOLDOWN_TICKS} ticks, quince veces mas que
 *       encenderlo.</li>
 * </ul>
 *
 * <p>Dominio puro: todo lo del jugador pasa por {@link SoahModeContext}.
 */
public final class SoahMode extends ToggleableMode<SoahModeContext> {

    /** Por que no se pudo encender el sello. */
    public enum BlockReason {
        /** Aun no ha recibido el sello ({@code d.soah_gave = 0}). */
        NO_SEAL,
        /** El sello maldito esta activo: son excluyentes. */
        CURSED_SEAL,
        /** Ninjutsu Medico insuficiente. */
        LOW_MEDICAL,
        /** Afinidad Yin insuficiente. */
        LOW_YIN,
        /** Chakra maximo insuficiente. */
        LOW_MAX_CHAKRA,
        /** Aun corre el veto de alternancia ({@code d.soah_cd}). */
        COOLDOWN,
        /** La reserva del sello esta vacia: hay que recargarla agachandose. */
        EMPTY_SEAL
    }

    /** Esta devolviendo reserva ahora mismo ({@code d.soah_healing}). */
    private boolean healing;

    /** Indica si el sello esta devolviendo chakra en este momento. */
    public boolean healing() {
        return healing;
    }

    @Override
    protected boolean onActivate(SoahModeContext ctx) {
        BlockReason veto = veto(ctx);
        if (veto != null) {
            ctx.notifyBlocked(veto);
            return false;
        }
        healing = false;
        ctx.setCooldown(SoahTable.ACTIVATE_COOLDOWN_TICKS);
        ctx.notifyActivated(ctx.storedChakra());
        return true;
    }

    @Override
    protected void onTick(SoahModeContext ctx) {
        if (!healing && SoahTable.needsHealing(ctx.chakra())) {
            healing = true; // run: por debajo del umbral, el sello entra a devolver
        }
        if (healing) {
            if (ctx.chakra() >= ctx.maxChakra()) {
                healing = false; // stop_heals: no para hasta dejarte al maximo
                ctx.notifyStored(ctx.storedChakra());
            } else {
                int tier = SoahTable.healTier(ctx.storedChakra());
                if (tier > 0) {
                    ctx.transferFromSeal(tier); // heal: un solo tramo por tick
                }
            }
        }
        ctx.emitAura();
    }

    @Override
    protected boolean onCost(SoahModeContext ctx) {
        if (ctx.chakra() <= 0) {
            return false; // no_chakra: sin chakra el sello no se sostiene
        }
        return !healing || ctx.storedChakra() > 0; // la reserva agotada a mitad de cura lo apaga
    }

    @Override
    protected void onPunish(SoahModeContext ctx) {
        if (ctx.storedChakra() <= 0) {
            ctx.notifyOutOfStoredChakra(); // explica ademas como recargarlo
        }
    }

    @Override
    protected void onDeactivate(SoahModeContext ctx) {
        healing = false;
        ctx.setCooldown(SoahTable.DEACTIVATE_COOLDOWN_TICKS);
        ctx.notifyDeactivated();
    }

    /** Primer motivo que impide encender el sello, o {@code null} si puede. Decision pura. */
    private static BlockReason veto(SoahModeContext ctx) {
        if (!ctx.hasSeal()) {
            return BlockReason.NO_SEAL;
        }
        if (ctx.cursedSealActive()) {
            return BlockReason.CURSED_SEAL;
        }
        if (ctx.medicalLevel() < SoahTable.UNLOCK_MEDICAL) {
            return BlockReason.LOW_MEDICAL;
        }
        if (ctx.yinAffinity() < SoahTable.REQUIRED_YIN_AFFINITY) {
            return BlockReason.LOW_YIN;
        }
        if (ctx.maxChakra() < SoahTable.REQUIRED_MAX_CHAKRA) {
            return BlockReason.LOW_MAX_CHAKRA;
        }
        if (ctx.cooldown() > 0) {
            return BlockReason.COOLDOWN;
        }
        if (ctx.storedChakra() <= 0) {
            return BlockReason.EMPTY_SEAL;
        }
        return null;
    }
}
