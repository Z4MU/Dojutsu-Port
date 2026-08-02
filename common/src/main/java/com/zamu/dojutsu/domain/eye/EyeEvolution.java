package com.zamu.dojutsu.domain.eye;

import java.util.List;
import java.util.Objects;

/**
 * Una arista del grafo de evolucion de ojos {@code [S05]}: "este ojo puede convertirse en {@link #to} si se
 * cumplen todos los {@link #requirements}". El grafo es data-driven (RA-3): las rutas divergentes
 * (Uchiha/Otsutsuki vs. implante) no se hardcodean, se expresan como distintas aristas con distintos
 * requisitos. Dominio puro; el codec vive en {@link com.zamu.dojutsu.dojutsu.EyeDefinition}.
 *
 * @param to           id del {@link com.zamu.dojutsu.dojutsu.EyeDefinition} destino (p. ej. {@code "mangekyo_itachi"})
 * @param requirements requisitos que deben cumplirse <b>todos</b> (AND) para que la evolucion sea elegible
 * @param variants     pool de destinos entre los que se elige <b>al azar</b> (el {@code mangekyo_rng} del datapack:
 *                    el Sharingan despierta un Mangekyo de personaje aleatorio). Vacio = destino fijo {@link #to}
 */
public record EyeEvolution(String to, List<EyeRequirement> requirements, List<String> variants) {

    public EyeEvolution {
        Objects.requireNonNull(to, "to");
        if (to.isBlank()) {
            throw new IllegalArgumentException("el destino de la evolucion no puede estar vacio");
        }
        requirements = List.copyOf(requirements);
        variants = List.copyOf(variants);
    }

    /** Arista de destino fijo (sin pool aleatorio): azucar para el codigo/tests que no usan variantes. */
    public EyeEvolution(String to, List<EyeRequirement> requirements) {
        this(to, requirements, List.of());
    }

    /** {@code true} si la evolucion elige el destino al azar de {@link #variants} (Mangekyo por personaje). */
    public boolean isRandom() {
        return !variants.isEmpty();
    }
}
