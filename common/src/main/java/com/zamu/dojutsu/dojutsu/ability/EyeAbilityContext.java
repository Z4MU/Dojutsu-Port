package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Contexto de ejecucion de una {@link EyeAbility}: el lanzador y su mundo. Los caster no-jugador (IA de F8) se
 * modelaran ampliando este contexto, igual que {@code CastContext} en el motor de jutsus.
 *
 * @param caster jugador que lanza la habilidad
 */
public record EyeAbilityContext(ServerPlayer caster) {

    /** Mundo (server) del lanzador. */
    public ServerLevel level() {
        return (ServerLevel) caster.level();
    }
}
