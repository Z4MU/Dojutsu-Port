package com.zamu.dojutsu.domain.eightgates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la tabla de las Ocho Puertas (F6/DJT-145): la derivacion desde el taijutsu, las dos curvas que
 * premian progresar y la recuperacion de fatiga. Valores verificados del datapack, en escala del mod (÷4).
 */
class EightGatesTableTest {

    @Test
    void laPuertaMaximaSeDerivaDelTaijutsu() {
        assertEquals(0, EightGatesTable.maxGate(EightGatesTable.ACTIVATION_TAIJUTSU - 1));
        assertEquals(1, EightGatesTable.maxGate(Gate.OPENING.requiredTaijutsu()));
        assertEquals(4, EightGatesTable.maxGate(Gate.PAIN.requiredTaijutsu()));
        assertEquals(8, EightGatesTable.maxGate(Gate.DEATH.requiredTaijutsu()));
        assertEquals(8, EightGatesTable.maxGate(99999)); // no hay novena puerta
    }

    @Test
    void laMaestriaLlegaMasTardeQueLaOctavaPuerta() {
        assertFalse(EightGatesTable.hasMastery(Gate.DEATH.requiredTaijutsu()));
        assertTrue(EightGatesTable.hasMastery(EightGatesTable.MASTERY_TAIJUTSU));
    }

    @Test
    void progresarDejaEncadenarPuertasMasRapido() {
        // la curva baja de 180 a 20 ticks, y dominarlas la deja en 10
        assertEquals(180, EightGatesTable.openCooldownTicks(1, false));
        assertEquals(120, EightGatesTable.openCooldownTicks(4, false));
        assertEquals(20, EightGatesTable.openCooldownTicks(8, false));
        assertEquals(EightGatesTable.OPEN_COOLDOWN_MASTERED, EightGatesTable.openCooldownTicks(1, true));
    }

    @Test
    void progresarRetrasaElPrimerMordiscoDeFatiga() {
        // la curva opuesta: sube de 20 a 65, y dominarlas la deja en 75
        assertEquals(20, EightGatesTable.firstStrainTickTicks(1, false));
        assertEquals(45, EightGatesTable.firstStrainTickTicks(4, false));
        assertEquals(65, EightGatesTable.firstStrainTickTicks(8, false));
        assertEquals(EightGatesTable.FIRST_STRAIN_TICK_MASTERED, EightGatesTable.firstStrainTickTicks(8, true));
    }

    @Test
    void lasCurvasSeIndexanPorLaPuertaMaximaAunSinNinguna() {
        // eg_max 0 no revienta la tabla: cae en el indice de la primera puerta
        assertEquals(180, EightGatesTable.openCooldownTicks(0, false));
        assertEquals(20, EightGatesTable.firstStrainTickTicks(0, false));
    }

    @Test
    void elClanLeeRecuperaLaMitadDeRapido() {
        // Tal cual el datapack: 400 ticks por punto para Lee frente a 200 para el resto. Es lo CONTRARIO de lo
        // que dice la ficha de [S11]; se porta como esta (regla 16) y queda anotado como posible fallo de origen.
        assertEquals(200, EightGatesTable.decayPulseTicks(false));
        assertEquals(400, EightGatesTable.decayPulseTicks(true));
    }

    @Test
    void lasCelulasDeHashiramaTriplicanLaRecuperacion() {
        assertEquals(1, EightGatesTable.decayAmount(false));
        assertEquals(3, EightGatesTable.decayAmount(true));
    }

    @Test
    void soloAlCienSeFuerzaElCierre() {
        assertFalse(EightGatesTable.forcesClose(EightGatesTable.PUNISH_HARD_AT));
        assertFalse(EightGatesTable.forcesClose(99));
        assertTrue(EightGatesTable.forcesClose(EightGatesTable.FORCE_CLOSE_AT));
    }

    @Test
    void laEscaladaDeEfectosEsMonotona() {
        int previousStrength = -1;
        for (Gate gate : Gate.values()) {
            assertTrue(gate.strength() > previousStrength,
                    "la fuerza deberia crecer en cada puerta: " + gate.id());
            previousStrength = gate.strength();
        }
        // las dos primeras no dan salto; a partir de la 3ª, si
        assertEquals(Gate.NONE, Gate.OPENING.jumpBoost());
        assertEquals(Gate.NONE, Gate.HEALING.jumpBoost());
        assertEquals(Gate.NONE, Gate.OPENING.speed()); // la 1ª es solo fuerza
        assertEquals(9, Gate.DEATH.jumpBoost());
        assertEquals(24, Gate.DEATH.speed());
    }

    @Test
    void lasPuertasSeResuelvenPorEtapa() {
        assertEquals(java.util.Optional.of(Gate.LIMIT), Gate.forStage(5));
        assertTrue(Gate.forStage(0).isEmpty()); // 0 = cerradas
        assertTrue(Gate.forStage(9).isEmpty());
    }
}
