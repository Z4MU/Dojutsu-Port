package com.zamu.dojutsu.bijuu.ai;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de {@link BeastMove} por id. Se puebla <b>en codigo</b> en el arranque, como
 * {@code EyeAbilityRegistry}: cada tecnica es una clase con logica propia.
 *
 * <p><b>Punto de encuentro de la dualidad</b> {@code [S09-R2]}: tanto la IA de la bestia (F7/DJT-161) como el
 * jinchuriki transformado (F7/DJT-166) resuelven aqui la misma instancia. Que exista un unico registro es lo que
 * hace <b>imposible</b> que las dos versiones vuelvan a divergir.
 *
 * <p>Un id sin clase registrada devuelve vacio: una bestia cuya tecnica insignia aun no este implementada
 * simplemente no la usa, en vez de romper el tick (estado tolerante, como el resto del proyecto).
 */
public final class BeastMoveRegistry {

    private static final Map<String, BeastMove> MOVES = new ConcurrentHashMap<>();

    private BeastMoveRegistry() {
    }

    /** Registra (o reemplaza) una tecnica por su id. Llamar en el arranque. */
    public static void register(BeastMove move) {
        MOVES.put(move.id(), move);
    }

    /** Tecnica por id, o vacio si aun no esta implementada. */
    public static Optional<BeastMove> get(String id) {
        return Optional.ofNullable(MOVES.get(id));
    }

    /** Numero de tecnicas registradas (para el log de arranque y el comando de QA). */
    public static int size() {
        return MOVES.size();
    }
}
