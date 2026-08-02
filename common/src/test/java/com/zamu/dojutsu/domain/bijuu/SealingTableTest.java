package com.zamu.dojutsu.domain.bijuu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de las recompensas de sellar una bestia (F7/DJT-165). Verificado contra {@code jinchuriki/sealing/*}.
 */
class SealingTableTest {

    @Test
    void elChakraPrestadoEsMilPorCola() {
        // la ficha decia "+8000 fijo"; el datapack escala con la bestia
        assertEquals(1_000, SealingTable.maxChakraBonus(TailedBeast.ONE_TAIL));
        assertEquals(9_000, SealingTable.maxChakraBonus(TailedBeast.NINE_TAILS));
        assertEquals(10_000, SealingTable.maxChakraBonus(TailedBeast.TEN_TAILS));
    }

    @Test
    void laXpSigueUnPatronQueElNueveYElDiezRompen() {
        // 100 + 10*colas de la 1 a la 8...
        assertEquals(110, SealingTable.ninjaXp(TailedBeast.ONE_TAIL));
        assertEquals(180, SealingTable.ninjaXp(TailedBeast.EIGHT_TAILS));
        // ...y luego el patron se rompe dos veces
        assertEquals(230, SealingTable.ninjaXp(TailedBeast.NINE_TAILS)); // seria 190
        assertEquals(1_000, SealingTable.ninjaXp(TailedBeast.TEN_TAILS));
    }

    @Test
    void cadaBestiaPrestaLosElementosDeSuKekkeiGenkai() {
        // Shukaku presta viento + tierra, que son justo los componentes del Magnetismo
        assertEquals(java.util.List.of(NatureType.WIND, NatureType.EARTH),
                SealingTable.naturesOf(TailedBeast.ONE_TAIL));
        assertEquals(Optional.of(KekkeiGenkai.MAGNET), SealingTable.kekkeiGenkaiOf(TailedBeast.ONE_TAIL));
        // el 4 presta fuego + tierra y da Lava; el 5, fuego + agua y da Ebullicion
        assertEquals(Optional.of(KekkeiGenkai.LAVA), SealingTable.kekkeiGenkaiOf(TailedBeast.FOUR_TAILS));
        assertEquals(Optional.of(KekkeiGenkai.BOIL), SealingTable.kekkeiGenkaiOf(TailedBeast.FIVE_TAILS));
    }

    @Test
    void tresBestiasNoPrestanNingunElemento() {
        assertTrue(SealingTable.naturesOf(TailedBeast.SEVEN_TAILS).isEmpty());
        assertTrue(SealingTable.naturesOf(TailedBeast.EIGHT_TAILS).isEmpty());
        assertTrue(SealingTable.naturesOf(TailedBeast.NINE_TAILS).isEmpty());
        assertTrue(SealingTable.kekkeiGenkaiOf(TailedBeast.NINE_TAILS).isEmpty());
    }

    @Test
    void soloCuatroBestiasPrestanKekkeiGenkai() {
        long withKg = java.util.Arrays.stream(TailedBeast.values())
                .filter(beast -> SealingTable.kekkeiGenkaiOf(beast).isPresent())
                .count();
        assertEquals(4, withKg);
    }

    @Test
    void elDiezColasLoPrestaTodo() {
        assertEquals(7, SealingTable.naturesOf(TailedBeast.TEN_TAILS).size()); // 5 basicas + yin + yang
        assertTrue(SealingTable.naturesOf(TailedBeast.TEN_TAILS).contains(NatureType.YIN));
        assertTrue(SealingTable.naturesOf(TailedBeast.TEN_TAILS).contains(NatureType.YANG));
        assertEquals(Optional.of(KekkeiGenkai.WOOD), SealingTable.kekkeiGenkaiOf(TailedBeast.TEN_TAILS));
    }

    @Test
    void ningunaBestiaPrestaElYinYangDerivado() {
        // YIN_YANG se deriva de yin+yang en NatureProfile: prestarlo aparte lo duplicaria
        for (TailedBeast beast : TailedBeast.values()) {
            assertFalse(SealingTable.naturesOf(beast).contains(NatureType.YIN_YANG),
                    beast.id() + " no deberia prestar el yin-yang derivado");
        }
    }
}
