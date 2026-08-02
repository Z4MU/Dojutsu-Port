package com.zamu.dojutsu.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ejecuta los {@link PlayerTickSystem} registrados, por jugador y en el orden de {@link TickPhase}
 * ({@code [S01 §4]}). Es el punto de acoplamiento central del motor: los modulos se enganchan por
 * inyeccion ({@link #register}) sin que {@code core} conozca su logica (OCP).
 *
 * <p>Dentro de una misma fase se conserva el orden de registro (orden estable). No hace reset de input.
 */
public final class PlayerTickPipeline {

    private static final Comparator<PlayerTickSystem> BY_PHASE =
            Comparator.comparingInt(system -> system.phase().ordinal());

    private final List<PlayerTickSystem> systems = new ArrayList<>();

    /** Registra un sistema, manteniendo el orden por fase (estable dentro de la misma fase). */
    public void register(PlayerTickSystem system) {
        systems.add(Objects.requireNonNull(system, "system"));
        systems.sort(BY_PHASE); // List.sort es estable: conserva el orden de registro en empates
    }

    /** Ejecuta todos los sistemas para el jugador, en orden de fase. */
    public void run(ServerPlayer player) {
        for (PlayerTickSystem system : systems) {
            system.tick(player);
        }
    }

    /** Vista de solo lectura de los sistemas, en orden de ejecucion (para inspeccion/tests). */
    public List<PlayerTickSystem> systems() {
        return List.copyOf(systems);
    }
}
