package com.zamu.dojutsu.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.progression.NinjaRank;
import org.junit.jupiter.api.Test;

/** Tests de la deteccion reactiva de cambio de rango (DJT-037). Puro (derivado de ninja_xp). */
class RankServiceTest {

    @Test
    void cruzarUnUmbralDevuelveElNuevoRango() {
        // 999 (Academy) -> 1000 (Genin).
        assertEquals(NinjaRank.GENIN, RankService.rankChangeFrom(999, 1_000).orElseThrow());
    }

    @Test
    void umbralExacto() {
        assertEquals(NinjaRank.CHUNIN, RankService.rankChangeFrom(1_999, 2_000).orElseThrow());
    }

    @Test
    void sinCambioDeRangoDevuelveVacio() {
        // 500 -> 900: sigue en Academy.
        assertTrue(RankService.rankChangeFrom(500, 900).isEmpty());
    }

    @Test
    void saltarVariosRangosDeUnaVez() {
        // 0 (Academy) -> 6000 (Tokubetsu Jonin): cambio (rango 0 -> 3).
        assertEquals(NinjaRank.TOKUBETSU_JONIN, RankService.rankChangeFrom(0, 6_000).orElseThrow());
    }

    @Test
    void enElRangoMaximoNoHayCambio() {
        // God (40000+) -> mas xp: sigue God.
        assertTrue(RankService.rankChangeFrom(40_000, 50_000).isEmpty());
    }
}
