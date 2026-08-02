package com.zamu.dojutsu.domain.nature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests del registro de naturalezas (DJT-050): composicion de los sets y afinidad derivada de Yin-Yang. */
class NatureTypeTest {

    @Test
    void hayOchoNaturalezas() {
        // [S03]: 5 basicas + 3 avanzadas (yin, yang, yin-yang).
        assertEquals(8, NatureType.values().length);
    }

    @Test
    void lasBasicasSonLasCincoElementales() {
        // predicates/has_all_basic_natures: fire, wind, ligh, earth, water.
        assertEquals(Set.of(NatureType.FIRE, NatureType.WIND, NatureType.LIGHTNING, NatureType.EARTH,
                NatureType.WATER), NatureType.basics());
    }

    @Test
    void lasSieteConAfinidadCoincidenConHasAllNatures() {
        // predicates/has_all_natures: las 5 basicas + yin + yang. Yin-Yang NO cuenta (es derivada).
        assertEquals(Set.of(NatureType.FIRE, NatureType.WIND, NatureType.LIGHTNING, NatureType.EARTH,
                NatureType.WATER, NatureType.YIN, NatureType.YANG), NatureType.withAffinity());
        assertFalse(NatureType.withAffinity().contains(NatureType.YIN_YANG));
    }

    @Test
    void soloYinYangNoAlmacenaAfinidad() {
        for (NatureType nature : NatureType.values()) {
            assertEquals(nature != NatureType.YIN_YANG, nature.storesAffinity(),
                    "storesAffinity incorrecto en " + nature);
        }
    }

    @Test
    void lasSieteQueAlmacenanAfinidadSonExactamenteLasNoCombinadas() {
        // Invariante que sostiene NatureProfile.normalizeAffinities.
        assertEquals(NatureType.withAffinity().size(),
                (int) Arrays.stream(NatureType.values()).filter(NatureType::storesAffinity).count());
    }

    @Test
    void losSetsSonInmutables() {
        assertThrows(UnsupportedOperationException.class, () -> NatureType.basics().add(NatureType.YIN));
        assertThrows(UnsupportedOperationException.class, () -> NatureType.withAffinity().add(NatureType.YIN_YANG));
    }

    @Test
    void categoriasCorrectas() {
        assertEquals(NatureType.Category.BASIC, NatureType.FIRE.category());
        assertEquals(NatureType.Category.ADVANCED, NatureType.YIN.category());
        assertEquals(NatureType.Category.COMBINED, NatureType.YIN_YANG.category());
        assertTrue(NatureType.basics().stream().allMatch(n -> n.category() == NatureType.Category.BASIC));
    }
}
