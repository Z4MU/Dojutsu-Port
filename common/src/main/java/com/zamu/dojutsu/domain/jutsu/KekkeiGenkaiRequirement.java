package com.zamu.dojutsu.domain.jutsu;

import java.util.Objects;

/**
 * Requisito de <b>posesion de un Kekkei Genkai</b> de una tecnica: el id data-driven del KG que el caster
 * debe poseer (puerta {@code check_kekkei_genkai} del datapack: "Lack required Kekkei Genkai.").
 *
 * <p>A diferencia de {@link AffinityRequirement}, no lleva nivel: es una puerta de <b>posesion</b>, no de
 * grado. Los jutsus KG del datapack la combinan con los requisitos de afinidad de sus naturalezas
 * <b>componentes</b> (ej. Madera: poseer {@code wood} + {@code aff_earth} y {@code aff_water}) — F4/DJT-091.
 *
 * @param kekkeiGenkaiId id data-driven del Kekkei Genkai (no vacio; ej. "wood", "ice")
 */
public record KekkeiGenkaiRequirement(String kekkeiGenkaiId) {

    public KekkeiGenkaiRequirement {
        Objects.requireNonNull(kekkeiGenkaiId, "kekkeiGenkaiId");
        if (kekkeiGenkaiId.isBlank()) {
            throw new IllegalArgumentException("kekkeiGenkaiId no puede estar vacio");
        }
    }
}
