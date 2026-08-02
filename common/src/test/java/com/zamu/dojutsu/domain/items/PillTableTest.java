package com.zamu.dojutsu.domain.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.ItemFlags;
import org.junit.jupiter.api.Test;

/**
 * Tests de las pildoras y de la mascara (F8/DJT-170). Verificado contra {@code items/ration_pills/*} y
 * {@code items/masks/*}.
 */
class PillTableTest {

    @Test
    void cadaPildoraDevuelveUnQuintoDelChakraMaximo() {
        assertEquals(200, PillTable.chakraRestored(1_000));
        assertEquals(0, PillTable.chakraRestored(4)); // division entera, como el datapack (/= c5)
    }

    @Test
    void elBoteTraeCincoPildoras() {
        assertEquals(5, PillTable.PILLS_PER_ITEM);
        assertTrue(PillTable.hasPillsLeft(1));
        assertFalse(PillTable.hasPillsLeft(0));
    }

    @Test
    void laResacaLlegaDiezSegundosDespuesDeComer() {
        // 200 ticks = los 10s del "schedule" del datapack; es lo que hace util atiborrarse en combate
        assertEquals(200, PillTable.HANGOVER_DELAY_TICKS);
        assertTrue(PillTable.HANGOVER_DELAY_TICKS > PillTable.EAT_DELAY_TICKS);
    }

    @Test
    void hayMascarasParaLosSeisIdsDelDatapack() {
        assertEquals(6, MaskType.values().length);
        for (int id = 1; id <= 6; id++) {
            int expected = id;
            assertTrue(java.util.Arrays.stream(MaskType.values())
                            .anyMatch(mask -> mask.datapackId() == expected),
                    "falta la mascara con id " + id);
        }
    }

    @Test
    void ponerseYQuitarseLaMascaraEsSimetrico() {
        ItemFlags sinMascara = ItemFlags.DEFAULT;
        assertFalse(sinMascara.hasMask());

        ItemFlags conMascara = sinMascara.withMask(MaskType.TOBI_1.id());
        assertTrue(conMascara.hasMask());
        assertEquals(java.util.Optional.of(MaskType.TOBI_1), conMascara.mask().flatMap(MaskType::byId));

        assertFalse(conMascara.withoutMask().hasMask());
    }

    @Test
    void unaMascaraDesconocidaNoRevienta() {
        // estado tolerante: un NBT con una mascara de otro pack se lee como "sin mascara resoluble"
        assertTrue(ItemFlags.DEFAULT.withMask("kakashi").mask().flatMap(MaskType::byId).isEmpty());
    }
}
