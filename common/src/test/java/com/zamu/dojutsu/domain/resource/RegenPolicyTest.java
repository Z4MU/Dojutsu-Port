package com.zamu.dojutsu.domain.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests de la politica de regen timer->pulso (DJT-010). Los valores replican la tabla de {@code [S02]}
 * (umbrales 120/60/30/15 e incrementos por clan/DNA/modo).
 */
class RegenPolicyTest {

    private static void assertPulse(int umbral, int incremento, Set<RegenFactor> factores) {
        RegenPolicy p = RegenPolicy.resolve(factores);
        assertEquals(umbral, p.thresholdTicks(), "umbral para " + factores);
        assertEquals(incremento, p.increment(), "incremento para " + factores);
    }

    @Test
    void tablaSinModo() {
        assertPulse(120, 3, EnumSet.noneOf(RegenFactor.class));            // estandar
        assertPulse(60, 4, EnumSet.of(RegenFactor.CLAN_OTSUTSUKI));
        assertPulse(60, 6, EnumSet.of(RegenFactor.CLAN_UZUMAKI));
        assertPulse(60, 8, EnumSet.of(RegenFactor.HASHIRAMA_DNA));
    }

    @Test
    void tablaSage1() {
        assertPulse(60, 4, EnumSet.of(RegenFactor.SAGE_MODE_1));           // estandar
        assertPulse(30, 5, EnumSet.of(RegenFactor.SAGE_MODE_1, RegenFactor.CLAN_OTSUTSUKI));
        assertPulse(30, 7, EnumSet.of(RegenFactor.SAGE_MODE_1, RegenFactor.CLAN_UZUMAKI));
        assertPulse(30, 9, EnumSet.of(RegenFactor.SAGE_MODE_1, RegenFactor.HASHIRAMA_DNA));
    }

    @Test
    void tablaSage2() {
        assertPulse(30, 4, EnumSet.of(RegenFactor.SAGE_MODE_2));           // estandar
        assertPulse(15, 5, EnumSet.of(RegenFactor.SAGE_MODE_2, RegenFactor.CLAN_OTSUTSUKI));
        assertPulse(15, 7, EnumSet.of(RegenFactor.SAGE_MODE_2, RegenFactor.CLAN_UZUMAKI));
        assertPulse(15, 9, EnumSet.of(RegenFactor.SAGE_MODE_2, RegenFactor.HASHIRAMA_DNA));
    }

    @Test
    void combinacionResuelveLaClaseMasFuerteYElModoMasAlto() {
        // Uzumaki + Sage + DNA de Hashirama: gana Hashirama (mas fuerte) y Sage 2 (mas alto) -> 15/+9.
        Set<RegenFactor> combo = EnumSet.of(
                RegenFactor.CLAN_UZUMAKI, RegenFactor.HASHIRAMA_DNA,
                RegenFactor.SAGE_MODE_1, RegenFactor.SAGE_MODE_2);
        assertPulse(15, 9, combo);
    }

    @Test
    void tasaPorSegundoEstandar() {
        // 3 x (20/120) = 0.5 chakra/seg.
        assertEquals(0.5, RegenPolicy.resolve(EnumSet.noneOf(RegenFactor.class)).chakraPerSecond(), 1e-9);
        // Uzumaki base: 6 x (20/60) = 2.0.
        assertEquals(2.0, RegenPolicy.resolve(EnumSet.of(RegenFactor.CLAN_UZUMAKI)).chakraPerSecond(), 1e-9);
    }

    @Test
    void tickDisparaElPulsoSoloEnElUmbral() {
        RegenPolicy p = new RegenPolicy(120, 3);
        for (int i = 1; i < 120; i++) {
            assertEquals(0, p.tick(), "sin pulso en el tick " + i);
        }
        assertEquals(3, p.tick(), "pulso en el tick 120");
        // Tras el pulso, el timer se reinicia y vuelve a disparar a los 120 ticks.
        for (int i = 1; i < 120; i++) {
            assertEquals(0, p.tick());
        }
        assertEquals(3, p.tick(), "segundo pulso en el tick 240");
    }

    @Test
    void losPulsosAplicadosAUnRecursoSeClampan() {
        Resource chakra = new Resource(0, 100);
        RegenPolicy p = new RegenPolicy(60, 8); // Hashirama base
        int pulsos = 0;
        for (int tick = 0; tick < 60 * 20; tick++) { // 20 pulsos de +8 = 160 > 100
            int ganado = p.tick();
            if (ganado > 0) {
                chakra.add(ganado);
                pulsos++;
            }
        }
        assertEquals(20, pulsos, "60 ticks/pulso durante 1200 ticks = 20 pulsos");
        assertEquals(100, chakra.current(), "el chakra nunca supera el max");
    }

    @Test
    void constructorRechazaValoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new RegenPolicy(0, 3));
        assertThrows(IllegalArgumentException.class, () -> new RegenPolicy(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> new RegenPolicy(120, -1));
    }
}
