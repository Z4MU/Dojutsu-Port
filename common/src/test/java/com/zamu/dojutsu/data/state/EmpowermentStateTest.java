package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests del sub-estado de potenciaciones (F6/DJT-130): normalizacion de rangos y withers inmutables de los tres
 * subsistemas (sello maldito, Ocho Puertas, SOAH).
 */
class EmpowermentStateTest {

    @Test
    void elNinjaNuevoNoTienePotenciaciones() {
        assertTrue(EmpowermentState.DEFAULT.cursedSeal().isEmpty());
        assertFalse(EmpowermentState.DEFAULT.hasCursedSeal());
        assertEquals(0, EmpowermentState.DEFAULT.gateStrain());
        assertEquals(0, EmpowermentState.DEFAULT.soahCharge());
        assertFalse(EmpowermentState.DEFAULT.soahGranted());
    }

    @Test
    void recibirUnSelloLoDejaEnNivelUno() {
        EmpowermentState sealed = EmpowermentState.DEFAULT.withCursedSeal("heaven");
        assertEquals("heaven", sealed.cursedSeal().orElseThrow());
        assertEquals(1, sealed.cursedSealLevel());
        assertTrue(sealed.hasCursedSeal());
    }

    @Test
    void quitarElSelloDejaElNivelEnCero() {
        EmpowermentState clean = EmpowermentState.DEFAULT.withCursedSeal("tayuya")
                .withCursedSealLevel(2)
                .withoutCursedSeal();
        assertFalse(clean.hasCursedSeal());
        assertEquals(0, clean.cursedSealLevel());
    }

    @Test
    void elNivelDelSelloSeClampaADos() {
        assertEquals(EmpowermentState.MAX_SEAL_LEVEL,
                EmpowermentState.DEFAULT.withCursedSeal("earth").withCursedSealLevel(9).cursedSealLevel());
        assertEquals(0, EmpowermentState.DEFAULT.withCursedSeal("earth").withCursedSealLevel(-3).cursedSealLevel());
    }

    @Test
    void laFatigaCorporalSeClampaACien() {
        assertEquals(EmpowermentState.MAX_GATE_STRAIN, EmpowermentState.DEFAULT.withGateStrain(999).gateStrain());
        assertEquals(0, EmpowermentState.DEFAULT.withGateStrain(-1).gateStrain());
    }

    @Test
    void losContadoresNuncaSonNegativos() {
        assertEquals(0, EmpowermentState.DEFAULT.withGateStrain(-50).gateStrain());
        assertEquals(0, EmpowermentState.DEFAULT.withSoahCharge(-10).soahCharge());
    }

    @Test
    void lasMuertesPorInyeccionSeAcumulan() {
        EmpowermentState state = EmpowermentState.DEFAULT.withCursedSealDeath().withCursedSealDeath();
        assertEquals(2, state.cursedSealDeaths());
    }

    @Test
    void losWithersNoContaminanOtrosSubsistemas() {
        EmpowermentState state = EmpowermentState.DEFAULT
                .withCursedSeal("kidomaru")
                .withGateStrain(3)
                .withSoahCharge(500)
                .withSoahGranted(true);
        assertEquals("kidomaru", state.cursedSeal().orElseThrow());
        assertEquals(3, state.gateStrain());
        assertEquals(500, state.soahCharge());
        assertTrue(state.soahGranted());
        // cambiar la fatiga de puertas no toca el sello ni el SOAH
        EmpowermentState strained = state.withGateStrain(40);
        assertEquals("kidomaru", strained.cursedSeal().orElseThrow());
        assertEquals(500, strained.soahCharge());
        assertEquals(40, strained.gateStrain());
    }
}
