package com.zamu.dojutsu.domain.nature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Tests del registro de Kekkei Genkai (DJT-050): son 18 y hashirama_cells no es uno de ellos. */
class KekkeiGenkaiTest {

    @Test
    void hayDieciochoKekkeiGenkai() {
        // [S03]: 18 KG. El datapack tiene 19 ficheros give/kg/* (sin all/random), pero hashirama_cells no
        // escribe ningun d.kg_* (escribe d.has_hsdna) -> no es un KG, es un flag de NatureProfile.
        assertEquals(18, KekkeiGenkai.values().length);
    }

    @Test
    void incluyeLosCompuestosYLosEspeciales() {
        assertTrue(contains("WOOD"));
        assertTrue(contains("ICE"));
        assertTrue(contains("SHIKOTSUMYAKU"));
        assertTrue(contains("JUGO_CLAN"));
    }

    @Test
    void noIncluyeHashiramaCells() {
        assertFalse(contains("HASHIRAMA_CELLS"));
    }

    private static boolean contains(String name) {
        return Arrays.stream(KekkeiGenkai.values()).anyMatch(kg -> kg.name().equals(name));
    }
}
