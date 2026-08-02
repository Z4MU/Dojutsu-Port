package com.zamu.dojutsu.bijuu.ai;

import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Contexto de ejecucion de una {@link BeastMove}. La pieza que <b>resuelve la dualidad</b> de {@code [S09-R2]}:
 * el lanzador es un {@link LivingEntity}, no un jugador, asi que la misma tecnica sirve a la bestia y al
 * jinchuriki transformado.
 *
 * <p>A diferencia del {@code EyeAbilityContext} (que exige {@code ServerPlayer} porque el dojutsu es siempre de
 * jugador), aqui el tipo mas amplio <b>es</b> el diseño: si una tecnica necesitara algo exclusivo del jugador,
 * es señal de que no pertenece a este catalogo.
 *
 * @param caster entidad que lanza la tecnica: la bestia (IA) o el jinchuriki en forma (F7/DJT-166)
 * @param beast  bestia de la que procede la tecnica; fija el sabor y a quien pertenece
 */
public record BeastMoveContext(LivingEntity caster, TailedBeast beast) {

    /** Mundo (server) del lanzador. */
    public ServerLevel level() {
        return (ServerLevel) caster.level();
    }

    /** Indica si quien lanza es la propia bestia (IA) y no un jinchuriki transformado. */
    public boolean castByBeast() {
        return caster.getType() == com.zamu.dojutsu.registry.ModEntities.TAILED_BEAST.get();
    }
}
