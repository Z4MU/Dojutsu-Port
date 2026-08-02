package com.zamu.dojutsu.core.scheduler;

import java.util.Objects;
import java.util.PriorityQueue;

/**
 * Planificador de tareas diferidas por tick del servidor. Reemplaza el {@code schedule 1s/3600s} del
 * datapack por tareas puntuales con un retardo en ticks.
 *
 * <p>Mantiene un contador de ticks propio; cada {@link #tick()} lo avanza y ejecuta las tareas cuyo tick
 * de vencimiento ya llego, en orden (por vencimiento y, a igualdad, por orden de programacion). El fallo
 * de una tarea no impide ejecutar las demas. Es logica pura (server-side), unit-testeable sin Minecraft.
 */
public final class ModScheduler {

    private static final System.Logger LOGGER = System.getLogger("dojutsu.core.scheduler");

    private final PriorityQueue<Task> queue = new PriorityQueue<>();
    private long currentTick;
    private long sequence;

    /**
     * Programa una tarea para dentro de {@code delayTicks} ticks. Un retardo &le; 0 la ejecuta en el
     * proximo tick (no "en el pasado").
     *
     * @throws NullPointerException si {@code action} es nulo
     */
    public void schedule(long delayTicks, Runnable action) {
        Objects.requireNonNull(action, "action");
        long dueTick = currentTick + Math.max(0L, delayTicks);
        queue.add(new Task(dueTick, sequence++, action));
    }

    /** Avanza un tick y ejecuta todas las tareas vencidas, aislando el fallo de cada una. */
    public void tick() {
        currentTick++;
        while (!queue.isEmpty() && queue.peek().dueTick() <= currentTick) {
            Task task = queue.poll();
            try {
                task.action().run();
            } catch (RuntimeException ex) {
                LOGGER.log(System.Logger.Level.ERROR, "Tarea programada fallo", ex);
            }
        }
    }

    /** Tick actual del planificador. */
    public long currentTick() {
        return currentTick;
    }

    /** Numero de tareas pendientes. */
    public int pending() {
        return queue.size();
    }

    private record Task(long dueTick, long seq, Runnable action) implements Comparable<Task> {
        @Override
        public int compareTo(Task other) {
            int byDue = Long.compare(dueTick, other.dueTick);
            return byDue != 0 ? byDue : Long.compare(seq, other.seq);
        }
    }
}
