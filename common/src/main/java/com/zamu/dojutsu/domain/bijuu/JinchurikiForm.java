package com.zamu.dojutsu.domain.bijuu;

import com.zamu.dojutsu.domain.mode.ToggleableMode;

/**
 * Forma de jinchuriki {@code [S09]} (F7/DJT-166) como {@link ToggleableMode}: <b>septimo</b> consumidor del
 * framework de F1 y el primero fuera de F5/F6.
 *
 * <p>Traduce {@code jinchuriki/form/toggle} + {@code activate} + {@code main} + {@code deactivate}:
 * <ul>
 *   <li>{@code onActivate}: veta sin bestia, con el veto de entrada corriendo, o —solo el Diez Colas— sin
 *       Rinnegan activo. Presta {@value JinchurikiFormTable#FORM_MAX_CHAKRA_BONUS} de chakra maximo.</li>
 *   <li>{@code onTick}: aplica el aura del nivel (o la del Baryon, que la <b>reemplaza</b>) y sostiene el vuelo
 *       del Diez Colas. Y vuelve a comprobar el Rinnegan: perderlo a media forma la apaga.</li>
 *   <li>{@code onCost}: cae por debajo de {@value JinchurikiFormTable#FORM_FALL_CHAKRA} de chakra. La forma no
 *       cobra por pulsos como los modos de F6 — cobra <b>una vez</b> al entrar y te sostiene mientras te quede
 *       algo que gastar.</li>
 *   <li>{@code onDeactivate}: devuelve el chakra maximo prestado y deja el veto de
 *       {@value JinchurikiFormTable#FORM_COOLDOWN_TICKS} ticks.</li>
 * </ul>
 *
 * <p><b>El invariante de los ojos no vive aqui.</b> {@code [ADD/04 §8]} dice "la forma desactiva los ojos", pero
 * el datapack no los <b>apaga</b>: los <b>suprime</b> ({@code dojutsu/main} ejecuta cada ojo
 * {@code unless is_in_jinchuriki_form}, con el ojo aun equipado). La diferencia no es cosmetica: la forma del
 * Diez Colas <b>exige el Rinnegan activo</b>, asi que apagarlo de verdad haria que esa forma se apagase a si
 * misma en el mismo tick. Se implementa como supresion en el servicio, y aqui queda anotado por que.
 *
 * <p>Dominio puro: todo lo del jugador pasa por {@link JinchurikiFormContext}.
 */
public final class JinchurikiForm extends ToggleableMode<JinchurikiFormContext> {

    /** Por que no se pudo entrar en forma. */
    public enum BlockReason {
        /** No lleva ninguna bestia sellada. */
        NOT_JINCHURIKI,
        /** Aun corre el veto de entrada ({@code d.jin_formcd}). */
        COOLDOWN,
        /** La forma del Diez Colas exige el Rinnegan (o el Rinne Sharingan) activo. */
        NEEDS_RINNEGAN
    }

    @Override
    protected boolean onActivate(JinchurikiFormContext ctx) {
        TailedBeast beast = ctx.beast().orElse(null);
        if (beast == null) {
            ctx.notifyBlocked(BlockReason.NOT_JINCHURIKI);
            return false;
        }
        if (ctx.formCooldown() > 0) {
            ctx.notifyBlocked(BlockReason.COOLDOWN);
            return false;
        }
        if (beast.isTenTails() && !ctx.hasRinneganActive()) {
            ctx.notifyBlocked(BlockReason.NEEDS_RINNEGAN);
            return false;
        }
        ctx.addMaxChakra(JinchurikiFormTable.FORM_MAX_CHAKRA_BONUS);
        ctx.setFlight(beast.isTenTails()); // form/activate: solo la del Diez Colas da vuelo por si misma
        ctx.notifyToggled(true);
        return true;
    }

    @Override
    protected void onTick(JinchurikiFormContext ctx) {
        TailedBeast beast = ctx.beast().orElse(null);
        if (beast == null) {
            deactivate(ctx); // te extrajeron la bestia estando en forma
            return;
        }
        if (beast.isTenTails() && !ctx.hasRinneganActive()) {
            deactivate(ctx); // form/main: perder el Rinnegan a media forma la apaga
            return;
        }
        ctx.applyAura(JinchurikiFormTable.auraFor(ctx.level(), ctx.baryonActive()));
    }

    @Override
    protected boolean onCost(JinchurikiFormContext ctx) {
        return !JinchurikiFormTable.fallsForChakra(ctx.chakra());
    }

    @Override
    protected void onDeactivate(JinchurikiFormContext ctx) {
        ctx.addMaxChakra(-JinchurikiFormTable.FORM_MAX_CHAKRA_BONUS);
        ctx.setFormCooldown(JinchurikiFormTable.FORM_COOLDOWN_TICKS);
        ctx.setFlight(false);
        ctx.notifyToggled(false);
    }
}
