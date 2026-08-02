package com.zamu.dojutsu.domain.jutsu;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Datos inmutables de una tecnica (patron 1 del ADD): el contrato que ~200 tecnicas data-driven rellenan
 * sin cambiar la estructura (RA-1).
 *
 * <p>El <b>efecto</b> se guarda como <b>referencia</b> ({@code effectId}), no como el efecto en si: las
 * familias de efecto y el motor son F4. La categoria y la afinidad tambien se referencian por id
 * data-driven para no acoplar el dominio a enums que crecen.
 *
 * <p><b>Afinidad multiple + Kekkei Genkai (F4/DJT-091):</b> un jutsu puede exigir <b>varias</b> afinidades
 * (todas obligatorias) — los jutsus KG del datapack gatean por las afinidades de sus naturalezas
 * <b>componentes</b> (Madera = {@code earth} + {@code water}) — y ademas <b>poseer un KG</b>
 * ({@code check_kekkei_genkai}). Las tecnicas de una sola afinidad se construyen con el constructor de
 * conveniencia (retrocompatible); {@link #affinityRequirement()} devuelve la primera.
 *
 * @param id                     id data-driven de la tecnica (no vacio; ej. "rasengan")
 * @param chakraCost             coste de chakra (&ge; 0)
 * @param cooldownTicks          cooldown en ticks (&ge; 0)
 * @param affinityRequirements   requisitos de afinidad (todos obligatorios); lista vacia si es universal
 * @param kekkeiGenkaiRequirement requisito de posesion de KG, o vacio (F4/DJT-091)
 * @param skillRequirement       requisito de nivel de habilidad (kenjutsu/medical), o vacio (F4/DJT-083)
 * @param levelRequirement       nivel requerido para aprenderla (&ge; 0); puerta de aprendizaje (S06
 *                               learner), no del cast — no forma parte de {@link Jutsu#evaluatePreconditions}
 * @param category               id data-driven de la categoria (no vacio; ej. "fire", "taijutsu")
 * @param effectId               referencia al efecto que ejecuta F4 (no vacio)
 */
public record JutsuDefinition(
        String id,
        int chakraCost,
        int cooldownTicks,
        List<AffinityRequirement> affinityRequirements,
        Optional<KekkeiGenkaiRequirement> kekkeiGenkaiRequirement,
        Optional<SkillRequirement> skillRequirement,
        int levelRequirement,
        String category,
        String effectId) {

    public JutsuDefinition {
        requireNonBlank(id, "id");
        requireNonNegative(chakraCost, "chakraCost");
        requireNonNegative(cooldownTicks, "cooldownTicks");
        Objects.requireNonNull(affinityRequirements, "affinityRequirements");
        affinityRequirements = List.copyOf(affinityRequirements); // inmutable + null-check de elementos
        Objects.requireNonNull(kekkeiGenkaiRequirement, "kekkeiGenkaiRequirement");
        Objects.requireNonNull(skillRequirement, "skillRequirement");
        requireNonNegative(levelRequirement, "levelRequirement");
        requireNonBlank(category, "category");
        requireNonBlank(effectId, "effectId");
    }

    /**
     * Constructor de conveniencia retrocompatible: una sola afinidad (o ninguna), sin KG. Mantiene los
     * call sites previos a DJT-091 (una afinidad {@link Optional}) sin cambios.
     */
    public JutsuDefinition(String id, int chakraCost, int cooldownTicks,
            Optional<AffinityRequirement> affinityRequirement, Optional<SkillRequirement> skillRequirement,
            int levelRequirement, String category, String effectId) {
        this(id, chakraCost, cooldownTicks,
                affinityRequirement.map(List::of).orElseGet(List::of), Optional.empty(),
                skillRequirement, levelRequirement, category, effectId);
    }

    /**
     * Primer requisito de afinidad (o vacio). Atajo para los jutsus de una sola afinidad y para los
     * consumidores que solo necesitan una naturaleza representativa (mensaje de feedback, XP de afinidad).
     */
    public Optional<AffinityRequirement> affinityRequirement() {
        return affinityRequirements.isEmpty() ? Optional.empty() : Optional.of(affinityRequirements.get(0));
    }

    private static void requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " no puede estar vacio");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " no puede ser negativo: " + value);
        }
    }
}
