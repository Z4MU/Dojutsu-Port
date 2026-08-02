package com.zamu.dojutsu.perf;

import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.dojutsu.EyeDefinition;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * <b>Banco de pruebas manual</b> de los caminos calientes del tick (F10/DJT-222). <b>Deshabilitado a
 * proposito</b>: mide tiempo, y un test que mide tiempo falla en CI el dia que la maquina va cargada. Se ejecuta
 * a mano cuando se quiere volver a comprobar una decision:
 *
 * <pre>./gradlew :common:test --tests "*TickHotPathBenchmark*" -Djunit.jupiter.conditions.deactivate=*</pre>
 *
 * <p>Existe porque DJT-222 no quiso afirmar mejoras sin números. Lo que mide no es "el mod va mas rapido" —eso
 * solo se ve en partida— sino el <b>coste unitario</b> de lo que se cambio, que es lo unico que se puede medir
 * sin un servidor con jugadores dentro.
 *
 * <h2>Escala de referencia</h2>
 * Un tick de servidor son <b>50 ms</b>. Con 20 jugadores, un sistema por tick que cueste 1 µs por jugador
 * consume 20 µs = el <b>0,04 %</b> del presupuesto. Por eso este banco sirve para descartar micro-optimizaciones
 * tanto como para justificar las que se hicieron: casi todo lo que se mide aqui es irrelevante, y decirlo es
 * parte del resultado.
 */
@Disabled("banco manual: mide tiempo, no comportamiento")
class TickHotPathBenchmark {

    private static final int WARMUP = 200_000;
    private static final int ITERATIONS = 5_000_000;

    /** Azar compartido, como el que usan ahora AwakeningService y EvolutionService. */
    private static final RandomGenerator SHARED = ThreadLocalRandom.current();

    @Test
    void azarPorLlamadaFrenteAAzarCompartido() {
        // Lo que hacian AwakeningService y EvolutionService: un Random nuevo por jugador y tick.
        long nuevo = time(() -> {
            RandomGenerator r = new Random();
            return r.nextInt(2);
        });
        // Lo que hacen ahora.
        long compartido = time(() -> SHARED.nextInt(2));

        report("new Random() por llamada", nuevo);
        report("ThreadLocalRandom compartido", compartido);
        System.out.printf("  -> factor: %.1fx%n", (double) nuevo / compartido);
    }

    @Test
    void jugadorSinOjoConGuardaYSinElla() {
        EyeState sinOjo = EyeState.DEFAULT;
        Function<String, Optional<EyeDefinition>> lookup = id -> Optional.empty();

        // Antes: se construia el predicado de flags y un Random, y se entraba en evaluate para nada.
        long sinGuarda = time(() -> {
            Predicate<String> flags = flag -> "hashirama_dna".equals(flag) && sinOjo.eternal();
            RandomGenerator r = new Random();
            return sinOjo.activeEye().flatMap(lookup).isPresent() || flags.test("x") ? r.nextInt(2) : 0;
        });
        // Ahora: una comprobacion sobre el estado y fuera.
        long conGuarda = time(() -> sinOjo.activeEye().isEmpty() ? 1 : 0);

        report("tick de jugador sin ojo (antes)", sinGuarda);
        report("tick de jugador sin ojo (ahora)", conGuarda);
        System.out.printf("  -> factor: %.1fx%n", (double) sinGuarda / conGuarda);
    }

    // ------------------------------------------------------------------

    private interface Work {
        int run();
    }

    /** Nanosegundos por operacion, con calentamiento y consumo del resultado (que no lo borre el JIT). */
    private static long time(Work work) {
        int sink = 0;
        for (int i = 0; i < WARMUP; i++) {
            sink += work.run();
        }
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            sink += work.run();
        }
        long elapsed = System.nanoTime() - start;
        if (sink == Integer.MIN_VALUE) {
            System.out.print(""); // consume el resultado sin ensuciar la salida
        }
        return elapsed / ITERATIONS;
    }

    private static void report(String label, long nanosPerOp) {
        // 20 jugadores x 20 ticks/s = 400 llamadas/s; el presupuesto de un tick son 50 ms
        double microsPorTickCon20Jugadores = nanosPerOp * 20 / 1000.0;
        System.out.printf("%-36s %6d ns/op   (%.2f us por tick con 20 jugadores, %.4f%% del tick)%n",
                label, nanosPerOp, microsPorTickCon20Jugadores, microsPorTickCon20Jugadores / 50_000.0 * 100.0);
    }
}
