package com.zamu.dojutsu.domain.susanoo;

import com.zamu.dojutsu.domain.mode.ToggleableMode;

/**
 * El Susanoo como <b>modo activable</b> {@code [S24]} (DJT-120): el <b>primer consumidor</b> del framework
 * {@link ToggleableMode} (patron 2 del ADD), que hasta ahora existia sin clientes. Sostenerlo cuesta chakra y
 * fatiga cada cierto numero de ticks; si el chakra no llega, el modo cae solo — exactamente el unico automatismo
 * que el framework promete ("coste impagable → castigo + apagado").
 *
 * <p>Traduce {@code susanoo/main.mcfunction} + {@code effects.mcfunction} al ciclo del framework:
 * <ul>
 *   <li>{@code onActivate}: veta si no hay {@link SusanooCostTable#ACTIVATION_CHAKRA_GATE} de chakra
 *       ({@code activate}: {@code d.chakra matches 5..}). Es <b>puerta, no cobro</b>: activar no consume nada.</li>
 *   <li>{@code onTick}: aplica la aura del nivel (proteccion/ignifugo/lastre), cada tick como el datapack
 *       (efectos de duracion 1 refrescados).</li>
 *   <li>{@code onCost}: cobra <b>por pulsos</b>, no por tick — mientras el temporizador corre, decrementa; al
 *       llegar a 0 cobra chakra + fatiga y lo rearma con el intervalo del nivel.</li>
 *   <li>{@code onDeactivate}: avisa al portador. El apagado por chakra agotado sale gratis de
 *       {@code onCost}=false, sin castigo extra: el datapack solo llama a {@code deactivate}.</li>
 * </ul>
 *
 * <p>Dominio puro: todo lo que toca al jugador pasa por {@link SusanooContext}, asi que el ciclo entero es
 * testeable sin arrancar Minecraft.
 */
public final class SusanooMode extends ToggleableMode<SusanooContext> {

    @Override
    protected boolean onActivate(SusanooContext ctx) {
        if (ctx.chakra() < SusanooCostTable.ACTIVATION_CHAKRA_GATE) {
            return false; // veta: el datapack exige d.chakra matches 5.. para siquiera manifestarlo
        }
        ctx.setPulseTimer(0); // el primer pulso se cobra en el tick siguiente, como al entrar en main
        ctx.notifyToggled(true);
        return true;
    }

    @Override
    protected void onTick(SusanooContext ctx) {
        ctx.applyAura(SusanooCostTable.tier(ctx.level()));
    }

    @Override
    protected boolean onCost(SusanooContext ctx) {
        int remaining = ctx.pulseTimer();
        if (remaining > 0) {
            ctx.setPulseTimer(remaining - 1);
            return true; // entre pulsos no se cobra (unless susanoo_time)
        }
        boolean eternal = ctx.eternal();
        int level = ctx.level();
        if (!ctx.consumeChakra(SusanooCostTable.chakraPerPulse(level, eternal))) {
            return false; // sin chakra: el framework castiga y apaga (if no_chakra run deactivate)
        }
        ctx.addStrain(SusanooCostTable.strainPerPulse(level, eternal));
        ctx.setPulseTimer(SusanooCostTable.pulseTicks(level, eternal));
        return true;
    }

    @Override
    protected void onDeactivate(SusanooContext ctx) {
        ctx.notifyToggled(false);
    }
}
