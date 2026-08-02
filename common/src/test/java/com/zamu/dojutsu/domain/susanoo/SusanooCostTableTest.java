package com.zamu.dojutsu.domain.susanoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la tabla del Susanoo (DJT-120): fija los valores <b>verificados</b> contra
 * {@code susanoo/main.mcfunction} (coste), {@code effects.mcfunction} (aura) y {@code level/set_N} (nombre/XP),
 * para que un retoque de balance sea una decision explicita y no un descuido.
 */
class SusanooCostTableTest {

    @Test
    void laTablaTieneLosSeisNivelesDelDatapack() {
        assertEquals(6, SusanooCostTable.tiers().size());
        assertEquals("Ribcage", SusanooCostTable.tier(1).name());
        assertEquals("Skeletal", SusanooCostTable.tier(2).name());
        assertEquals("Armored", SusanooCostTable.tier(3).name());
        assertEquals("Humanoid", SusanooCostTable.tier(4).name());
        assertEquals("Full-Body", SusanooCostTable.tier(5).name());
        assertEquals("Perfect", SusanooCostTable.tier(6).name());
    }

    @Test
    void elCosteDeChakraBajaConElNivel() {
        // main.mcfunction: 6, 4, 2, 1, 1, 1 — la maestria abarata el modo
        assertEquals(6, SusanooCostTable.tier(1).chakraPerPulse());
        assertEquals(4, SusanooCostTable.tier(2).chakraPerPulse());
        assertEquals(2, SusanooCostTable.tier(3).chakraPerPulse());
        assertEquals(1, SusanooCostTable.tier(4).chakraPerPulse());
        assertEquals(1, SusanooCostTable.tier(5).chakraPerPulse());
        assertEquals(1, SusanooCostTable.tier(6).chakraPerPulse());
    }

    @Test
    void elIntervaloDePulsoSeEspaciaConElNivel() {
        assertEquals(20, SusanooCostTable.tier(1).pulseTicks());
        assertEquals(20, SusanooCostTable.tier(2).pulseTicks());
        assertEquals(40, SusanooCostTable.tier(3).pulseTicks());
        assertEquals(50, SusanooCostTable.tier(4).pulseTicks());
        assertEquals(60, SusanooCostTable.tier(5).pulseTicks());
        assertEquals(80, SusanooCostTable.tier(6).pulseTicks());
    }

    @Test
    void laFatigaPorPulsoBajaConElNivel() {
        assertEquals(3, SusanooCostTable.tier(1).strainPerPulse());
        assertEquals(2, SusanooCostTable.tier(2).strainPerPulse());
        assertEquals(1, SusanooCostTable.tier(3).strainPerPulse());
        assertEquals(1, SusanooCostTable.tier(6).strainPerPulse());
    }

    @Test
    void laProteccionSubeConElNivel() {
        // effects.mcfunction: resistance y fire_resistance con amplificador nivel-1
        for (SusanooTier tier : SusanooCostTable.tiers()) {
            assertEquals(tier.level() - 1, tier.resistanceAmplifier());
            assertEquals(tier.level() - 1, tier.fireResistanceAmplifier());
        }
    }

    @Test
    void soloLosNivelesTresYCuatroLastran() {
        // effects.mcfunction da slowness solo a susanoo_lvl_3 y susanoo_lvl_4
        assertFalse(SusanooCostTable.tier(1).slowness());
        assertFalse(SusanooCostTable.tier(2).slowness());
        assertTrue(SusanooCostTable.tier(3).slowness());
        assertTrue(SusanooCostTable.tier(4).slowness());
        assertFalse(SusanooCostTable.tier(5).slowness());
        assertFalse(SusanooCostTable.tier(6).slowness());
    }

    @Test
    void elOjoEternoPagaLaTarifaPlanaSeaCualSeaElNivel() {
        for (int level = 1; level <= SusanooCostTable.MAX_LEVEL; level++) {
            assertEquals(0, SusanooCostTable.chakraPerPulse(level, true));
            assertEquals(1, SusanooCostTable.strainPerPulse(level, true));
            assertEquals(160, SusanooCostTable.pulseTicks(level, true));
        }
    }

    @Test
    void unNivelFueraDeRangoSeClampa() {
        assertEquals(1, SusanooCostTable.tier(0).level());
        assertEquals(1, SusanooCostTable.tier(-5).level());
        assertEquals(SusanooCostTable.MAX_LEVEL, SusanooCostTable.tier(99).level());
    }

    @Test
    void seSubeDeNivelAlAlcanzarElUmbralDeUso() {
        assertEquals(1, SusanooCostTable.levelAfterUse(1, SusanooCostTable.USE_PER_LEVEL - 1));
        assertEquals(2, SusanooCostTable.levelAfterUse(1, SusanooCostTable.USE_PER_LEVEL));
        assertEquals(6, SusanooCostTable.levelAfterUse(5, SusanooCostTable.USE_PER_LEVEL));
    }

    @Test
    void elNivelMaximoYaNoSube() {
        assertEquals(SusanooCostTable.MAX_LEVEL,
                SusanooCostTable.levelAfterUse(SusanooCostTable.MAX_LEVEL, SusanooCostTable.USE_PER_LEVEL * 10));
    }
}
