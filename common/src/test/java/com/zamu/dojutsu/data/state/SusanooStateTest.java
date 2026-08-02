package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.zamu.dojutsu.domain.susanoo.SusanooCostTable;
import org.junit.jupiter.api.Test;

/** Tests del sub-estado del Susanoo (DJT-120): normalizacion y withers inmutables. */
class SusanooStateTest {

    @Test
    void elPortadorNuevoEmpiezaEnNivelUnoSinUso() {
        assertEquals(1, SusanooState.DEFAULT.level());
        assertEquals(0, SusanooState.DEFAULT.use());
    }

    @Test
    void elNivelSeClampaAlRangoValido() {
        assertEquals(1, new SusanooState(0, 0).level());
        assertEquals(1, new SusanooState(-3, 0).level());
        assertEquals(SusanooCostTable.MAX_LEVEL, new SusanooState(99, 0).level());
    }

    @Test
    void elUsoNuncaEsNegativo() {
        assertEquals(0, new SusanooState(1, -50).use());
    }

    @Test
    void subirDeNivelReiniciaElUso() {
        SusanooState leveled = new SusanooState(2, SusanooCostTable.USE_PER_LEVEL).leveledUp();
        assertEquals(3, leveled.level());
        assertEquals(0, leveled.use());
    }

    @Test
    void enElNivelMaximoSubirNoHaceNada() {
        SusanooState top = new SusanooState(SusanooCostTable.MAX_LEVEL, 500);
        assertSame(top, top.leveledUp());
    }

    @Test
    void elWitherDeUsoNoCreaCopiaSiNoCambia() {
        SusanooState state = new SusanooState(2, 10);
        assertSame(state, state.withUse(10));
        assertEquals(11, state.withUse(11).use());
        assertEquals(2, state.withUse(11).level(), "cambiar el uso no toca el nivel");
    }

    @Test
    void plusUseAplicaLasSubidasDeNivelQueProvoca() {
        // el bono +78000 del EMS: 78000 = 2 niveles (32000 c/u) + 14000 de resto
        SusanooState boosted = SusanooState.DEFAULT.plusUse(78000);
        assertEquals(3, boosted.level());
        assertEquals(78000 - 2 * SusanooCostTable.USE_PER_LEVEL, boosted.use());
    }

    @Test
    void plusUseNoPasaDelNivelMaximoYDescartaElSobrante() {
        SusanooState top = new SusanooState(SusanooCostTable.MAX_LEVEL - 1, 0).plusUse(1_000_000);
        assertEquals(SusanooCostTable.MAX_LEVEL, top.level());
        assertEquals(0, top.use(), "en el tope no se guarda uso sobrante");
    }

    @Test
    void plusUseIgnoraCantidadesNoPositivas() {
        SusanooState state = new SusanooState(2, 10);
        assertSame(state, state.plusUse(0));
        assertSame(state, state.plusUse(-5));
    }
}
