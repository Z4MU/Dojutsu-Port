package com.zamu.dojutsu.domain.rng;

import java.util.Objects;

/**
 * Entrada de una {@link RollTable}: un valor con su peso relativo.
 *
 * <p>Un peso 0 es valido (la entrada existe pero nunca se elige). Los pesos no necesitan sumar ningun
 * total concreto: son relativos. Dominio puro.
 *
 * @param value  valor de la entrada (no nulo)
 * @param weight peso relativo (&ge; 0)
 */
public record WeightedEntry<T>(T value, int weight) {

    public WeightedEntry {
        Objects.requireNonNull(value, "value");
        if (weight < 0) {
            throw new IllegalArgumentException("weight no puede ser negativo: " + weight);
        }
    }
}
