package com.zamu.dojutsu.domain.jutsu;

import java.util.Objects;

/**
 * Requisito de afinidad de una tecnica: una naturaleza (por id data-driven) y el nivel minimo exigido.
 *
 * <p>Ej. Rasengan requiere Afinidad Viento 150 -&gt; {@code new AffinityRequirement("wind", 150)}. Una
 * tecnica universal (sin requisito) se modela con {@code Optional.empty()} en la definicion, no con una
 * instancia de amount 0.
 *
 * @param natureId id data-driven de la naturaleza (no vacio)
 * @param amount   nivel de afinidad minimo (&ge; 0)
 */
public record AffinityRequirement(String natureId, int amount) {

    public AffinityRequirement {
        Objects.requireNonNull(natureId, "natureId");
        if (natureId.isBlank()) {
            throw new IllegalArgumentException("natureId no puede estar vacio");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount no puede ser negativo: " + amount);
        }
    }
}
