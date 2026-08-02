package com.zamu.dojutsu.domain.cursedseal;

import com.zamu.dojutsu.domain.mode.ToggleableMode;

/**
 * Sello maldito {@code [S10]} (F6/DJT-140) como {@link ToggleableMode}: el poder prestado que <b>te consume si
 * no puedes pagarlo</b>. Cuarto consumidor del framework y <b>el primero que usa {@code onPunish}</b> — el hook
 * que existia desde F1 sin ningun cliente.
 *
 * <p>Traduce {@code cursed_seal/activate} + {@code main} + {@code punish} al ciclo del framework:
 * <ul>
 *   <li>{@code onActivate}: veta si no porta sello.</li>
 *   <li>{@code onTick}: aplica el aura del sello segun su tipo y nivel.</li>
 *   <li>{@code onCost}: cobra {@link CursedSealTable#chakraCost} cada
 *       {@value CursedSealTable#DRAIN_INTERVAL_TICKS} ticks. <b>Madurar lo abarata</b> (12 → 8).</li>
 *   <li>{@code onPunish}: <b>el sello mata a su portador</b> ({@code punish}) — y en nivel 2 la muerte cuenta
 *       para degradarlo. Es la diferencia con los demas modos: los otros solo se apagan.</li>
 * </ul>
 *
 * <p>Dominio puro: todo lo del jugador pasa por {@link CursedSealModeContext}, asi que el ciclo —incluida la
 * muerte— se testea sin arrancar Minecraft.
 */
public final class CursedSealMode extends ToggleableMode<CursedSealModeContext> {

    @Override
    protected boolean onActivate(CursedSealModeContext ctx) {
        if (ctx.seal().isEmpty()) {
            return false; // sin sello no hay nada que activar
        }
        ctx.setDrainTimer(CursedSealTable.DRAIN_INTERVAL_TICKS);
        ctx.notifyToggled(true);
        return true;
    }

    @Override
    protected void onTick(CursedSealModeContext ctx) {
        ctx.seal().ifPresent(type -> ctx.applyAura(type, ctx.level()));
    }

    @Override
    protected boolean onCost(CursedSealModeContext ctx) {
        int remaining = ctx.drainTimer();
        if (remaining > 0) {
            ctx.setDrainTimer(remaining - 1);
            return true; // entre pulsos no se cobra
        }
        if (!ctx.consumeChakra(CursedSealTable.chakraCost(ctx.level()))) {
            return false; // sin chakra: onPunish lo mata
        }
        ctx.setDrainTimer(CursedSealTable.DRAIN_INTERVAL_TICKS);
        return true;
    }

    @Override
    protected void onPunish(CursedSealModeContext ctx) {
        ctx.consumeBearer(); // "You didn't have enough Chakra and your Cursed Seal consumed you!"
    }

    @Override
    protected void onDeactivate(CursedSealModeContext ctx) {
        ctx.notifyToggled(false);
    }
}
