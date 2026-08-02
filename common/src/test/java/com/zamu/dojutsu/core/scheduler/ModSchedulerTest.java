package com.zamu.dojutsu.core.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Tests del planificador de tareas diferidas (DJT-022). */
class ModSchedulerTest {

    private static void tickN(ModScheduler scheduler, int n) {
        for (int i = 0; i < n; i++) {
            scheduler.tick();
        }
    }

    @Test
    void ejecutaLaTareaTrasElRetardoExacto() {
        ModScheduler scheduler = new ModScheduler();
        AtomicInteger ran = new AtomicInteger();
        scheduler.schedule(20, ran::incrementAndGet);

        tickN(scheduler, 19);
        assertEquals(0, ran.get(), "no debe correr antes de los 20 ticks");
        assertEquals(1, scheduler.pending());

        scheduler.tick(); // tick 20
        assertEquals(1, ran.get(), "corre exactamente en el tick 20");
        assertEquals(0, scheduler.pending());
    }

    @Test
    void retardoCeroCorreEnElProximoTick() {
        ModScheduler scheduler = new ModScheduler();
        AtomicInteger ran = new AtomicInteger();
        scheduler.schedule(0, ran::incrementAndGet);
        scheduler.tick();
        assertEquals(1, ran.get());
    }

    @Test
    void tareaEnElPasadoCorreEnElProximoTickNoAntes() {
        ModScheduler scheduler = new ModScheduler();
        AtomicInteger ran = new AtomicInteger();
        scheduler.schedule(-100, ran::incrementAndGet); // "pasado" -> proximo tick
        assertEquals(0, ran.get());
        scheduler.tick();
        assertEquals(1, ran.get());
    }

    @Test
    void ejecutaEnOrdenPorVencimientoYLuegoPorRegistro() {
        ModScheduler scheduler = new ModScheduler();
        List<String> log = new ArrayList<>();
        scheduler.schedule(2, () -> log.add("b-tarde"));
        scheduler.schedule(1, () -> log.add("a-pronto"));
        scheduler.schedule(1, () -> log.add("a-pronto-2")); // mismo vencimiento -> orden de registro
        tickN(scheduler, 2);
        assertEquals(List.of("a-pronto", "a-pronto-2", "b-tarde"), log);
    }

    @Test
    void elFalloDeUnaTareaNoImpideLasDemas() {
        ModScheduler scheduler = new ModScheduler();
        List<String> log = new ArrayList<>();
        scheduler.schedule(1, () -> {
            throw new RuntimeException("boom");
        });
        scheduler.schedule(1, () -> log.add("ok"));
        scheduler.tick();
        assertEquals(List.of("ok"), log);
    }

    @Test
    void avanzaElContadorDeTicks() {
        ModScheduler scheduler = new ModScheduler();
        tickN(scheduler, 5);
        assertEquals(5, scheduler.currentTick());
    }

    @Test
    void tareaNulaEsRechazada() {
        ModScheduler scheduler = new ModScheduler();
        assertThrows(NullPointerException.class, () -> scheduler.schedule(1, null));
    }
}
