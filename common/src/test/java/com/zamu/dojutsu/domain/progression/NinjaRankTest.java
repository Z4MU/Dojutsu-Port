package com.zamu.dojutsu.domain.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests de la tabla de rango ninja de [S20], con las fronteras exactas (RA-4). */
class NinjaRankTest {

    @ParameterizedTest(name = "xp {0} -> {1}")
    @CsvSource({
            "0,ACADEMY_STUDENT", "999,ACADEMY_STUDENT",
            "1000,GENIN", "1999,GENIN",
            "2000,CHUNIN", "4999,CHUNIN",
            "5000,TOKUBETSU_JONIN", "8999,TOKUBETSU_JONIN",
            "9000,JONIN", "11999,JONIN",
            "12000,SANNIN", "14999,SANNIN",
            "15000,KAGE", "17999,KAGE",
            "18000,LEGENDARY", "23999,LEGENDARY",
            "24000,GOD_OF_SHINOBI", "29999,GOD_OF_SHINOBI",
            "30000,SAGE_OF_THE_SIX_PATHS", "34999,SAGE_OF_THE_SIX_PATHS",
            "35000,DEMIGOD", "39999,DEMIGOD",
            "40000,GOD", "1000000,GOD"
    })
    void fromXpReproduceLosUmbrales(int xp, NinjaRank expected) {
        assertEquals(expected, NinjaRank.fromXp(xp));
    }

    @Test
    void elOrdinalCoincideConNinjaRank() {
        assertEquals(0, NinjaRank.ACADEMY_STUDENT.level());
        assertEquals(11, NinjaRank.GOD.level());
        assertEquals(12, NinjaRank.values().length, "hay 12 rangos (0..11)");
    }

    @Test
    void xpNegativaEsRechazada() {
        assertThrows(IllegalArgumentException.class, () -> NinjaRank.fromXp(-1));
    }
}
