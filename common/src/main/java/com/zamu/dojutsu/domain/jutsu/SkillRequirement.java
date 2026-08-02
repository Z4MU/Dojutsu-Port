package com.zamu.dojutsu.domain.jutsu;

import java.util.Objects;

/**
 * Requisito de <b>nivel de habilidad</b> de una tecnica: una habilidad (por id data-driven) y el nivel minimo.
 *
 * <p>Distinto de {@link AffinityRequirement} (afinidad de naturaleza): las tecnicas de kenjutsu exigen
 * {@code d.kenjutsu >= N} ("Requires higher Kenjutsu!") y las medicas {@code d.medical >= N} ("Requires higher
 * Medical Ninjutsu!"), sin afinidad. F4/DJT-083.
 *
 * @param skillId id data-driven de la habilidad ({@code "kenjutsu"}, {@code "medical"}...)
 * @param amount  nivel de habilidad minimo (&ge; 0)
 */
public record SkillRequirement(String skillId, int amount) {

    public SkillRequirement {
        Objects.requireNonNull(skillId, "skillId");
        if (skillId.isBlank()) {
            throw new IllegalArgumentException("skillId no puede estar vacio");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount no puede ser negativo: " + amount);
        }
    }
}
