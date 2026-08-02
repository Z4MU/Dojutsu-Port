package com.zamu.dojutsu.domain.jutsu;

/**
 * Razon por la que una tecnica no puede lanzarse, en el orden de chequeo del datapack {@code [S06]}
 * (la maquina {@code <jutsu>/cast}: chakra -&gt; cooldown -&gt; posesion de la naturaleza -&gt; afinidad).
 *
 * <p>{@code MISSING_NATURE} se anadio en F4/DJT-074 (auditoria de {@code check_affinity}, regla 16):
 * el datapack valida <b>poseer</b> la naturaleza ("Lack required Chakra Nature.") ademas del nivel de
 * afinidad — y en el mod importa: revocar una naturaleza conserva el nivel de afinidad (DJT-051).
 */
public enum CastFailureReason {

    /** El caster no tiene chakra suficiente para el coste. */
    INSUFFICIENT_CHAKRA,

    /** La tecnica esta en cooldown para el caster. */
    ON_COOLDOWN,

    /** El caster no posee el Kekkei Genkai requerido ({@code check_kekkei_genkai}, F4/DJT-091). */
    MISSING_KEKKEI_GENKAI,
    /** El caster no posee la naturaleza requerida ({@code check_affinity}: posesion, no nivel). */
    MISSING_NATURE,

    /** El caster no alcanza el nivel de afinidad requerido. */
    INSUFFICIENT_AFFINITY,

    /** El caster no alcanza el nivel de habilidad requerido (kenjutsu/medical, F4/DJT-083). */
    INSUFFICIENT_SKILL
}
