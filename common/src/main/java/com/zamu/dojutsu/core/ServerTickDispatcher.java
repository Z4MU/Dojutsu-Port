package com.zamu.dojutsu.core;

import com.zamu.dojutsu.core.scheduler.ModScheduler;
import dev.architectury.event.events.common.TickEvent;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Orquestador del tick del servidor. En cada {@code TickEvent.SERVER_POST} ejecuta tres fases:
 * <ol>
 *   <li><b>Jugador:</b> corre el {@link PlayerTickPipeline} por cada jugador conectado, aislando el fallo
 *       de un jugador para que no aborte el tick de los demas.</li>
 *   <li><b>Entidad/global:</b> placeholder (bestias, story, arbol divino, trader llegan en fases posteriores).</li>
 *   <li><b>Scheduler:</b> drena las tareas diferidas vencidas ({@link ModScheduler}).</li>
 * </ol>
 *
 * <p>Corre en el hilo del servidor y no itera entidades globales (rendimiento). El engranaje (pipeline y
 * scheduler) es accesible via {@link #get()} para que los modulos registren sistemas y programen tareas.
 */
public final class ServerTickDispatcher {

    private static final System.Logger LOGGER = System.getLogger("dojutsu.core.tick");

    private static ServerTickDispatcher instance;

    private final PlayerTickPipeline pipeline;
    private final ModScheduler scheduler;
    private long tickCount;

    public ServerTickDispatcher(PlayerTickPipeline pipeline, ModScheduler scheduler) {
        this.pipeline = Objects.requireNonNull(pipeline, "pipeline");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /** Engancha el dispatcher al tick del servidor y lo publica como instancia accesible. */
    public void register() {
        instance = this;
        TickEvent.SERVER_POST.register(this::onServerTick);
    }

    /** Instancia registrada (disponible tras {@link #register()} durante la inicializacion del mod). */
    public static ServerTickDispatcher get() {
        return instance;
    }

    public PlayerTickPipeline pipeline() {
        return pipeline;
    }

    public ModScheduler scheduler() {
        return scheduler;
    }

    /** Numero de ticks de servidor procesados (observabilidad). */
    public long tickCount() {
        return tickCount;
    }

    private void onServerTick(MinecraftServer server) {
        tickCount++;

        // Fase 1: jugadores (aislando el fallo de cada uno).
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                pipeline.run(player);
            } catch (RuntimeException ex) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "Fallo el tick del jugador " + player.getGameProfile().getName(), ex);
            }
        }

        // Fase 2: entidad/global (placeholder; bestias/story/godtree/trader en fases posteriores).

        // Fase 3: tareas diferidas.
        scheduler.tick();
    }
}
