package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.dojutsu.tenseigan.TenseiganChakraModeService;

/**
 * Tenseigan Chakra Mode como entrada del catalogo de habilidades {@code [S05]} (cierre de flecos de F5): igual
 * que el Susanoo, <b>no</b> produce un efecto de un disparo — enciende o apaga un
 * {@link com.zamu.dojutsu.domain.mode.ToggleableMode} cuyo coste se paga por pulsos. Es el puente entre el menu
 * ocular (DJT-117) y {@link TenseiganChakraModeService}, y cierra la ultima habilidad declarada del catalogo.
 *
 * <p><b>Coste 0 y fatiga 0 aqui a proposito:</b> encender no cobra — el chakra se paga <b>por pulso</b> mientras
 * dura (10 cada 200t) y la puerta de sostenimiento (chakra {@code >= 19}) la aplica el modo al activar. El
 * Tenseigan no anade fatiga.
 */
public final class TenseiganChakraModeAbility implements EyeAbility {

    @Override
    public int chakraCost() {
        return 0; // el coste real es por pulso; la puerta de 19 la aplica el modo al activar
    }

    @Override
    public int strainCost() {
        return 0; // el Tenseigan no anade eye_strain
    }

    @Override
    public void execute(EyeAbilityContext context) {
        TenseiganChakraModeService.toggle(context.caster());
    }
}
