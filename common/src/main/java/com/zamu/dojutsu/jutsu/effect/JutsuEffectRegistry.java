package com.zamu.dojutsu.jutsu.effect;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro <b>en codigo</b> de las familias de efecto (F4/DJT-074): a diferencia de los registros
 * data-driven (jutsus, clanes...), las familias son logica, no datos — se registran una vez en el
 * arranque ({@link JutsuEffects}) y no se recargan con {@code /reload}.
 *
 * <p>El pipeline resuelve {@code JutsuDefinition.effectId} contra este registro <b>antes</b> de
 * consumir chakra: un jutsu cuyo efecto no esta implementado falla sin coste. Extensible: F5+
 * puede registrar familias nuevas para casos especiales sin tocar el pipeline (OCP).
 */
public final class JutsuEffectRegistry {

    private static final Map<String, JutsuEffect> EFFECTS = new ConcurrentHashMap<>();

    private JutsuEffectRegistry() {
    }

    /** Registra una familia de efecto. Un id repetido reemplaza (avisar es del llamador). */
    public static void register(String effectId, JutsuEffect effect) {
        Objects.requireNonNull(effectId, "effectId");
        Objects.requireNonNull(effect, "effect");
        EFFECTS.put(effectId, effect);
    }

    /** La familia de un id, o vacio si no esta implementada. */
    public static Optional<JutsuEffect> get(String effectId) {
        return Optional.ofNullable(EFFECTS.get(effectId));
    }

    /** Indica si el id tiene familia implementada (lo consulta el pipeline antes de cobrar). */
    public static boolean isAvailable(String effectId) {
        return EFFECTS.containsKey(effectId);
    }
}
