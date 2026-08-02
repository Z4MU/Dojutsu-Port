package com.zamu.dojutsu.domain.sixpaths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

/**
 * Tests de las reglas del chakra de los Seis Caminos (F9/DJT-210). Verificado contra
 * {@code sage/six_paths/obtain_six_paths_chakra}.
 *
 * <p>Lo que se blinda: las <b>escalas</b> —que es donde se cuela el error al portar, porque conviven dos— y que
 * Yin/Yang valen mas XP que las elementales.
 */
class SixPathsTableTest {

    @Test
    void elChakraYLaXpVanUnoAUno() {
        assertEquals(6000, SixPathsTable.CHAKRA_BONUS);
        assertEquals(200, SixPathsTable.NINJA_XP);
    }

    @Test
    void laAfinidadNoSeReescalaPeroElSenjutsuSi() {
        // aff_* += 100 es escala nativa; senjutsu += 300 es una skill -> escala del mod (÷ 4)
        assertEquals(100, SixPathsTable.AFFINITY_BONUS);
        assertEquals(75, SixPathsTable.SENJUTSU_LEVELS);
        assertEquals(300 / 4, SixPathsTable.SENJUTSU_LEVELS);
    }

    @Test
    void concedeLasSieteNaturalezasSinRepetir() {
        assertEquals(7, SixPathsTable.GRANTED_NATURES.size());
        assertEquals(7, new HashSet<>(SixPathsTable.GRANTED_NATURES).size());
        assertTrue(SixPathsTable.GRANTED_NATURES.contains(NatureType.YIN));
        assertTrue(SixPathsTable.GRANTED_NATURES.contains(NatureType.YANG));
        // yin_yang NO esta: obtain_six_paths_chakra da yin y yang por separado
        assertTrue(SixPathsTable.GRANTED_NATURES.stream().noneMatch(n -> n == NatureType.YIN_YANG));
    }

    @Test
    void yinYYangValenMasXpQueLasElementales() {
        assertEquals(30, SixPathsTable.natureXp(NatureType.YIN));
        assertEquals(30, SixPathsTable.natureXp(NatureType.YANG));
        for (NatureType nature : SixPathsTable.GRANTED_NATURES) {
            if (nature != NatureType.YIN && nature != NatureType.YANG) {
                assertEquals(20, SixPathsTable.natureXp(nature), nature + " deberia dar 20");
            }
        }
    }

    @Test
    void laViaDelRinneganSigueDesactivada() {
        // still_check tiene sus tres lineas comentadas: portarla seria inventar contenido
        assertTrue(SixPathsTable.RINNEGAN_ROUTE_DISABLED);
    }
}
