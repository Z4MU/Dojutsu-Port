package com.zamu.dojutsu.domain.rng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/** Tests de la tabla de pesos con RNG inyectado (DJT-015). */
class RollTableTest {

    /** Generador de prueba que devuelve un valor fijo en nextInt(bound): fija el "dado" para el borde exacto. */
    private static final class FixedRandom implements RandomGenerator {
        private final int fixed;

        FixedRandom(int fixed) {
            this.fixed = fixed;
        }

        @Override
        public long nextLong() {
            return 0; // no usado por RollTable
        }

        @Override
        public int nextInt(int bound) {
            return fixed;
        }
    }

    private static RollTable<String> abc() {
        // Pesos no normalizados: A=2, B=3, C=5 (total 10).
        return RollTable.of(
                new WeightedEntry<>("A", 2),
                new WeightedEntry<>("B", 3),
                new WeightedEntry<>("C", 5));
    }

    @Test
    void seleccionPorBordesAcumulados() {
        RollTable<String> t = abc();
        assertEquals("A", t.roll(new FixedRandom(0)));
        assertEquals("A", t.roll(new FixedRandom(1)));
        assertEquals("B", t.roll(new FixedRandom(2)));
        assertEquals("B", t.roll(new FixedRandom(4)));
        assertEquals("C", t.roll(new FixedRandom(5)));
        assertEquals("C", t.roll(new FixedRandom(9)));
    }

    @Test
    void cadaValorCubreExactamenteSuPesoDelRango() {
        // Recorriendo r = 0..total-1 se prueba la proporcion de forma determinista.
        RollTable<String> t = abc();
        int a = 0;
        int b = 0;
        int c = 0;
        for (int r = 0; r < t.totalWeight(); r++) {
            switch (t.roll(new FixedRandom(r))) {
                case "A" -> a++;
                case "B" -> b++;
                case "C" -> c++;
                default -> throw new AssertionError();
            }
        }
        assertEquals(2, a);
        assertEquals(3, b);
        assertEquals(5, c);
    }

    @Test
    void mismaSemillaProduceLaMismaSecuencia() {
        RollTable<String> t = abc();
        List<String> first = rollN(t, new Random(1234L), 50);
        List<String> second = rollN(t, new Random(1234L), 50);
        assertEquals(first, second, "determinismo con semilla fija");
    }

    private static List<String> rollN(RollTable<String> t, RandomGenerator random, int n) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            out.add(t.roll(random));
        }
        return out;
    }

    @Test
    void unSoloElementoSiempreSale() {
        RollTable<String> t = RollTable.of(new WeightedEntry<>("solo", 7));
        assertEquals("solo", t.roll(new Random(0L)));
        assertEquals("solo", t.roll(new FixedRandom(6)));
    }

    @Test
    void pesoCeroNuncaSeElige() {
        RollTable<String> t = RollTable.of(
                new WeightedEntry<>("nunca", 0),
                new WeightedEntry<>("siempre", 1));
        for (int r = 0; r < 100; r++) {
            assertEquals("siempre", t.roll(new Random(r)));
        }
    }

    @Test
    void tablaVaciaLanzaAlTirar() {
        RollTable<String> vacia = new RollTable<>(List.of());
        assertTrue(vacia.isEmpty());
        assertEquals(0, vacia.totalWeight());
        assertThrows(IllegalStateException.class, () -> vacia.roll(new Random(0L)));
    }

    @Test
    void tablaConTodoPesoCeroLanzaAlTirar() {
        RollTable<String> t = RollTable.of(
                new WeightedEntry<>("x", 0),
                new WeightedEntry<>("y", 0));
        assertEquals(0, t.totalWeight());
        assertThrows(IllegalStateException.class, () -> t.roll(new Random(0L)));
    }

    @Test
    void entradaConPesoNegativoEsRechazada() {
        assertThrows(IllegalArgumentException.class, () -> new WeightedEntry<>("z", -1));
    }
}
