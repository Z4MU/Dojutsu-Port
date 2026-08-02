package com.zamu.dojutsu.dojutsu.ability;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de habilidades oculares por id ({@link EyeAbility}). Se puebla <b>en codigo</b> en el arranque
 * ({@code EyeAbilityService.register}), no data-driven: cada habilidad es una clase con logica propia (a
 * diferencia de los jutsus, que son datos + familia). Mismo patron que {@code JutsuEffectRegistry}.
 *
 * <p>Los ids coinciden con los del campo {@code abilities} de las {@code EyeDefinition} y con la
 * {@code selectedAbility} del menu (DJT-117): asi el pipeline resuelve la habilidad seleccionada.
 */
public final class EyeAbilityRegistry {

    private static final Map<String, EyeAbility> ABILITIES = new ConcurrentHashMap<>();

    private EyeAbilityRegistry() {
    }

    /** Registra (o reemplaza) una habilidad por id. Llamar en el arranque. */
    public static void register(String id, EyeAbility ability) {
        ABILITIES.put(id, ability);
    }

    /** Habilidad por id, o vacio si no esta implementada (habilidad data-only del menu sin clase aun). */
    public static Optional<EyeAbility> get(String id) {
        return Optional.ofNullable(ABILITIES.get(id));
    }
}
