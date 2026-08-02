package com.zamu.dojutsu.jutsu;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registro de las secuencias de aprendizaje ({@link LearnSequence}) cargadas desde datos. Lo
 * puebla {@link LearnSequenceReloadListener} en cada carga/{@code /reload}; lo consulta
 * {@code LearnerService.use} (DJT-075).
 *
 * <p>Un elemento <b>sin</b> secuencia no ensena nada (fiel al datapack: los learners de yin y
 * yang apuntaban a {@code learner/placeholder} — no hacian nada).
 */
public final class LearnSequenceRegistry {

    private static volatile Map<String, LearnSequence> sequences = Map.of();

    private LearnSequenceRegistry() {
    }

    /** Reemplaza las secuencias cargadas (lo llama el reload listener). */
    public static void load(Map<String, LearnSequence> loaded) {
        sequences = Collections.unmodifiableMap(new TreeMap<>(loaded));
    }

    /** Secuencia de un elemento, o vacio si su learner no ensena nada (aun). */
    public static Optional<LearnSequence> get(String element) {
        return Optional.ofNullable(sequences.get(element));
    }

    /** Vista de solo lectura de todas las secuencias, ordenadas por elemento. */
    public static Map<String, LearnSequence> all() {
        return sequences;
    }
}
