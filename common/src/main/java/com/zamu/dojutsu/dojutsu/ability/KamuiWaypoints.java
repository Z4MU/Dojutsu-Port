package com.zamu.dojutsu.dojutsu.ability;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Estado compartido de Kamui (Mangekyō de Obito): el <b>punto marcado</b> y el <b>cooldown</b> que
 * {@link KamuiAbility} (teletransporte propio) y {@link KamuiOthersAbility} (mandar a otros al punto) usan en
 * comun, porque en el datapack ambos leen la misma marca ({@code d.kamui_tp}) y comparten el mismo enfriamiento
 * ({@code d.mangekyo_time}). Estado de ejecucion (no se guarda): se olvida al desconectar via {@link #forget}.
 */
final class KamuiWaypoints {

    /** Un punto marcado: dimension + posicion + orientacion (soporta volver desde otra dimension). */
    record Waypoint(ResourceKey<Level> dimension, Vec3 pos, float yaw, float pitch) {
    }

    private static final Map<UUID, Waypoint> WAYPOINTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> READY_AT = new ConcurrentHashMap<>();

    private KamuiWaypoints() {
    }

    /** Punto marcado del jugador, o {@code null} si no ha marcado ninguno. */
    static Waypoint get(UUID playerId) {
        return WAYPOINTS.get(playerId);
    }

    /** Marca (o reemplaza) el punto del jugador. */
    static void set(UUID playerId, Waypoint waypoint) {
        WAYPOINTS.put(playerId, waypoint);
    }

    /** Borra el punto marcado (se consume al volver). */
    static void clear(UUID playerId) {
        WAYPOINTS.remove(playerId);
    }

    /** {@code true} si el Kamui sigue en enfriamiento (compartido entre teletransporte propio y a otros). */
    static boolean onCooldown(UUID playerId, long nowTick) {
        return nowTick < READY_AT.getOrDefault(playerId, Long.MIN_VALUE);
    }

    /** Arma el cooldown compartido de Kamui hasta el tick dado. */
    static void setReadyAt(UUID playerId, long readyAtTick) {
        READY_AT.put(playerId, readyAtTick);
    }

    /** Olvida punto y cooldown de un jugador (al desconectar). */
    static void forget(UUID playerId) {
        WAYPOINTS.remove(playerId);
        READY_AT.remove(playerId);
    }
}
