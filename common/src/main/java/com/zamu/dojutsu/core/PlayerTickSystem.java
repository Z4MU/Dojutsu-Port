package com.zamu.dojutsu.core;

import net.minecraft.server.level.ServerPlayer;

/**
 * Un sistema que corre por jugador cada tick del servidor. Cada modulo de gameplay implementa esta
 * interfaz y se registra en el {@link PlayerTickPipeline}; {@code core} no conoce su logica (inyeccion).
 *
 * <p>El sistema declara su {@link TickPhase}, que fija su posicion en el orden invariante del tick
 * ({@code [S01 §4]}). No hay reset de input (el datapack usaba polling; aqui no).
 */
public interface PlayerTickSystem {

    /** Fase que determina el orden de ejecucion de este sistema. */
    TickPhase phase();

    /** Ejecuta la logica de este sistema para un jugador en este tick (lado servidor). */
    void tick(ServerPlayer player);
}
