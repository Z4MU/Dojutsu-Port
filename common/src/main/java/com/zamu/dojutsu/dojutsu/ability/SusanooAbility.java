package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.domain.susanoo.SusanooCostTable;
import com.zamu.dojutsu.dojutsu.susanoo.SusanooService;

/**
 * Susanoo (Mangekyō) como entrada del catalogo de habilidades {@code [S24]} (DJT-120): a diferencia del resto,
 * <b>no produce un efecto de un disparo</b> — enciende o apaga un {@link com.zamu.dojutsu.domain.mode.ToggleableMode}
 * cuyo coste se paga por pulsos mientras dure. Es el puente entre el menu ocular (DJT-117) y
 * {@link SusanooService}, y evita tener que anadir una tecla nueva: {@code susanoo} ya es una habilidad
 * declarada de {@code mangekyo_itachi.json}.
 *
 * <p><b>Coste 0 y fatiga 0 aqui a proposito:</b> manifestarlo no cobra nada — el datapack solo <b>exige</b>
 * {@code d.chakra matches 5..} como puerta ({@link SusanooCostTable#ACTIVATION_CHAKRA_GATE}, que veta desde
 * {@code SusanooMode.onActivate}), y el chakra y la fatiga reales se cobran <b>por pulso</b> mientras esta
 * activo. Cobrar tambien en el cast seria un doble cobro que el datapack no hace.
 *
 * <p>El cooldown si es suyo: {@code activate} deja {@code d.susanoo_cd 14}.
 */
public final class SusanooAbility implements EyeAbility {

    @Override
    public int chakraCost() {
        return 0; // la puerta de 5 la aplica el modo al activar; el coste real es por pulso
    }

    @Override
    public int strainCost() {
        return 0; // la fatiga tambien se paga por pulso (3/2/1 segun nivel; 1 si el ojo es eterno)
    }

    @Override
    public int cooldownTicks() {
        return SusanooCostTable.ACTIVATION_COOLDOWN_TICKS;
    }

    @Override
    public void execute(EyeAbilityContext context) {
        SusanooService.toggle(context.caster());
    }
}
