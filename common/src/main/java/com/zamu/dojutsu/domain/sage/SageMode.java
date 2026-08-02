package com.zamu.dojutsu.domain.sage;

import com.zamu.dojutsu.domain.mode.ToggleableMode;

/**
 * Modo sabio {@code [S08]} (F6/DJT-136) como {@link ToggleableMode}: el aura de energia natural que potencia al
 * sabio mientras le quede carga. <b>Tercer consumidor</b> del framework, y el primero que corre sobre el arnes
 * compartido {@code ToggleableModeService} (F6/DJT-130).
 *
 * <p>Traduce {@code sage/toggle_check} + {@code sage/main} al ciclo del framework:
 * <ul>
 *   <li>{@code onActivate}: veta si no es sabio, si lleva un <b>sello maldito activo</b> (exclusion del
 *       datapack) o si no llega al minimo de energia de su nivel ({@link SageModeTable#activationMinimum}).</li>
 *   <li>{@code onTick}: aplica el aura (Fuerza II, Velocidad II, Salto IV, Resistencia II).</li>
 *   <li>{@code onCost}: cae si se queda sin chakra ({@code force_deactivate_1}); si no, gasta <b>1 de energia
 *       natural</b> cada {@link SageModeTable#drainInterval} ticks — el nivel <b>alarga</b> el modo, no lo
 *       abarata. Sin energia, cae ({@code force_deactivate}).</li>
 * </ul>
 *
 * <p>Dominio puro: todo lo del jugador pasa por {@link SageModeContext}, asi que el ciclo se testea sin MC.
 */
public final class SageMode extends ToggleableMode<SageModeContext> {

    /** Por que no se pudo encender el modo (para avisar al jugador con el motivo real). */
    public enum BlockReason {
        /** Aun no es sabio: hay que reunir energia natural hasta el maximo primero. */
        NOT_SAGE,
        /** Lleva un sello maldito activo; el datapack los hace excluyentes. */
        CURSED_SEAL,
        /** No llega al minimo de energia natural que exige su nivel. */
        NOT_ENOUGH_ENERGY
    }

    @Override
    protected boolean onActivate(SageModeContext ctx) {
        if (!ctx.isSage()) {
            ctx.notifyBlocked(BlockReason.NOT_SAGE);
            return false;
        }
        if (ctx.cursedSealActive()) {
            ctx.notifyBlocked(BlockReason.CURSED_SEAL);
            return false;
        }
        if (ctx.naturalEnergy() < SageModeTable.activationMinimum(ctx.level())) {
            ctx.notifyBlocked(BlockReason.NOT_ENOUGH_ENERGY);
            return false;
        }
        ctx.setDrainTimer(SageModeTable.drainInterval(ctx.level()));
        ctx.notifyToggled(true);
        return true;
    }

    @Override
    protected void onTick(SageModeContext ctx) {
        ctx.applyAura();
    }

    @Override
    protected boolean onCost(SageModeContext ctx) {
        if (!ctx.hasChakra()) {
            return false; // force_deactivate_1: sin chakra normal el modo no se sostiene
        }
        int remaining = ctx.drainTimer();
        if (remaining > 0) {
            ctx.setDrainTimer(remaining - 1);
            return true; // entre pulsos no se gasta energia
        }
        if (!ctx.consumeNaturalEnergy(SageModeTable.DRAIN_PER_PULSE)) {
            return false; // force_deactivate: se agoto la energia natural
        }
        ctx.setDrainTimer(SageModeTable.drainInterval(ctx.level()));
        return true;
    }

    @Override
    protected void onDeactivate(SageModeContext ctx) {
        ctx.notifyToggled(false);
    }
}
