package com.zamu.dojutsu.domain.jutsu;

/**
 * Comportamiento de una tecnica. Cada tecnica se define por sus datos ({@link #definition()}) y su
 * ejecucion ({@link #cast(CastContext)}); las precondiciones estandar del datapack se comparten en
 * {@link #evaluatePreconditions(CastContext)}.
 *
 * <p>En F1 solo existe el contrato: el motor y las familias de efecto (lo que hace {@code cast}) son F4.
 * Dominio puro: el caster es generico ({@link CastContext}), no un {@code Player}.
 */
public interface Jutsu {

    /** Datos inmutables de esta tecnica. */
    JutsuDefinition definition();

    /**
     * Ejecuta la tecnica (efecto real, consumo de chakra, cooldown, XP de afinidad). Debe comprobar antes
     * {@link #evaluatePreconditions(CastContext)} y devolver su fallo si no se cumplen. F4 lo implementa.
     */
    CastResult cast(CastContext ctx);

    /**
     * Chequeo estandar de precondiciones en el orden del datapack {@code [S06]}:
     * <b>chakra -&gt; cooldown -&gt; posesion de KG -&gt; posesion de la naturaleza -&gt; nivel de afinidad
     * -&gt; nivel de habilidad</b>. Devuelve el primer fallo encontrado, o exito si todo pasa. El requisito de
     * habilidad (kenjutsu/medical) gatea las tecnicas sin afinidad (F4/DJT-083).
     *
     * <p>La posesion ({@code check_affinity}: "Lack required Chakra Nature.") se anadio en F4/DJT-074
     * (regla 16): es una puerta distinta del nivel, porque revocar una naturaleza conserva la afinidad. La
     * posesion de KG ({@code check_kekkei_genkai}) y la <b>afinidad multiple</b> (los jutsus KG exigen las
     * afinidades de sus naturalezas componentes, <b>todas</b> obligatorias) se anadieron en F4/DJT-091.
     *
     * <p>El nivel requerido ({@link JutsuDefinition#levelRequirement()}) es puerta de aprendizaje, no de
     * cast, por lo que no se comprueba aqui (fiel a la maquina {@code <jutsu>/cast}).
     */
    default CastResult evaluatePreconditions(CastContext ctx) {
        JutsuDefinition def = definition();
        if (ctx.availableChakra() < def.chakraCost()) {
            return CastResult.failure(CastFailureReason.INSUFFICIENT_CHAKRA);
        }
        if (ctx.isOnCooldown(def.id())) {
            return CastResult.failure(CastFailureReason.ON_COOLDOWN);
        }
        if (def.kekkeiGenkaiRequirement().isPresent()
                && !ctx.hasKekkeiGenkai(def.kekkeiGenkaiRequirement().get().kekkeiGenkaiId())) {
            return CastResult.failure(CastFailureReason.MISSING_KEKKEI_GENKAI);
        }
        // Afinidad multiple: todas las naturalezas componentes deben poseerse y alcanzar su nivel.
        for (AffinityRequirement req : def.affinityRequirements()) {
            if (!ctx.hasNature(req.natureId())) {
                return CastResult.failure(CastFailureReason.MISSING_NATURE);
            }
            if (ctx.affinity(req.natureId()) < req.amount()) {
                return CastResult.failure(CastFailureReason.INSUFFICIENT_AFFINITY);
            }
        }
        if (def.skillRequirement().isPresent()) {
            SkillRequirement req = def.skillRequirement().get();
            if (ctx.skillLevel(req.skillId()) < req.amount()) {
                return CastResult.failure(CastFailureReason.INSUFFICIENT_SKILL);
            }
        }
        return CastResult.success();
    }
}
