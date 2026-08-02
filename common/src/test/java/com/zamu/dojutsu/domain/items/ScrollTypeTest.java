package com.zamu.dojutsu.domain.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de los tres pergaminos (F10/DJT-225). Los ids son la bisagra entre el enum, el item registrado, la
 * receta y la clave de traduccion: si divergen, el pergamino existe pero no se puede craftear.
 */
class ScrollTypeTest {

    @Test
    void sonExactamenteTres() {
        assertEquals(3, ScrollType.values().length);
    }

    @Test
    void elPathDelItemCoincideConElNombreDeLaReceta() {
        // data/dojutsu/recipe/<path>.json y dojutsu:<path> tienen que ser el mismo nombre
        assertEquals("medical_scroll", ScrollType.MEDICAL.itemPath());
        assertEquals("removal_scroll", ScrollType.REMOVAL.itemPath());
        assertEquals("sage_scroll", ScrollType.SAGE.itemPath());
    }

    @Test
    void seResuelvenPorId() {
        for (ScrollType type : ScrollType.values()) {
            assertEquals(Optional.of(type), ScrollType.byId(type.id()));
        }
    }

    @Test
    void unIdDesconocidoNoRompeNada() {
        assertTrue(ScrollType.byId("experience").isEmpty());
        assertTrue(ScrollType.byId("").isEmpty());
    }
}
