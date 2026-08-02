package com.zamu.dojutsu.domain.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests de la curva de XP por bandas de [S13], incluidas todas las fronteras (RA-4). */
class XpCurveTest {

    @Test
    void capEs500() {
        assertEquals(500, XpCurve.MAX_LEVEL);
    }

    @ParameterizedTest(name = "nivel {0} -> {1} XP")
    @CsvSource({
            // banda 0-10 = 10
            "0,10", "10,10",
            // banda 11-20 = 15
            "11,15", "20,15",
            // banda 21-40 = 20
            "21,20", "40,20",
            // banda 41-80 = 30
            "41,30", "80,30",
            // banda 81-90 = 50
            "81,50", "90,50",
            // banda 91-99 = 60
            "91,60", "99,60",
            // banda 100-299 = 80
            "100,80", "299,80",
            // banda 300-499 = 100
            "300,100", "499,100",
            // banda final 500 = 120
            "500,120"
    })
    void xpForLevelReproduceLasBandas(int level, int expected) {
        assertEquals(expected, XpCurve.xpForLevel(level));
    }

    @Test
    void nivelFueraDeRangoLanza() {
        assertThrows(IllegalArgumentException.class, () -> XpCurve.xpForLevel(-1));
        assertThrows(IllegalArgumentException.class, () -> XpCurve.xpForLevel(501));
    }
}
