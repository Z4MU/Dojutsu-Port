package com.zamu.dojutsu.domain.bijuu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * El catalogo de cuerpos modelados (F10/DJT-228).
 *
 * <p>Lo que protege es el <b>numero dos</b>. La suposicion natural —«las nueve bestias tendran su modelo»— es
 * justo la que costo un ticket entero de verificacion: el resource pack original declara diez y solo trae dos.
 */
class BeastArtTest {

    @Test
    void soloGyukiYKuramaTienenCuerpo() {
        assertTrue(BeastArt.hasBody(TailedBeast.EIGHT_TAILS));
        assertTrue(BeastArt.hasBody(TailedBeast.NINE_TAILS));
        assertEquals(2, BeastArt.modelledCount(), "el resource pack original solo trae dos cuerpos");
    }

    @Test
    void lasOtrasSieteYElDiezColasSeDibujanConElRavager() {
        // No es deuda: el datapack invoca un ravager de verdad y no lo hace invisible.
        for (TailedBeast beast : TailedBeast.values()) {
            if (beast == TailedBeast.EIGHT_TAILS || beast == TailedBeast.NINE_TAILS) {
                continue;
            }
            assertFalse(BeastArt.hasBody(beast), beast.id());
            assertTrue(BeastArt.customModelData(beast).isEmpty(), beast.id());
        }
    }

    @Test
    void elCustomModelDataEsElNumeroDeColas() {
        assertEquals(8, BeastArt.customModelData(TailedBeast.EIGHT_TAILS).orElseThrow());
        assertEquals(9, BeastArt.customModelData(TailedBeast.NINE_TAILS).orElseThrow());
    }
}
