package com.zamu.dojutsu.domain.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de las mascaras (F8/DJT-170; ids corregidos al verificarlos en DJT-206).
 *
 * <p>El test que ya existia en {@code PillTableTest} solo comprobaba que los seis ids <b>estuvieran</b>, sin
 * mirar cual era de quien: por eso el mapa invertido paso desapercibido. Aqui se fija <b>pareja a pareja</b>.
 */
class MaskTypeTest {

    /** {@code items/masks/*}, {@code give/masks/*} y {@code items/masks/unequip_give}: Tobi 1-3, ANBU 4-6. */
    private static final Map<MaskType, Integer> DATAPACK_IDS = Map.of(
            MaskType.TOBI_1, 1,
            MaskType.TOBI_2, 2,
            MaskType.TOBI_3, 3,
            MaskType.ANBU_1, 4,
            MaskType.ANBU_2, 5,
            MaskType.ANBU_3, 6);

    @Test
    void tobiSeNumeraDelUnoAlTresYAnbuDelCuatroAlSeis() {
        // el orden natural del enum (ANBU primero) NO es el orden de d.mask_id: es la trampa de este dato
        DATAPACK_IDS.forEach((mask, expected) ->
                assertEquals(expected, mask.datapackId(), "d.mask_id equivocado en " + mask.id()));
    }

    @Test
    void losSeisIdsSonDistintosYCubrenElRangoUnoASeis() {
        assertEquals(6, MaskType.values().length);
        for (int id = 1; id <= 6; id++) {
            int expected = id;
            assertTrue(java.util.Arrays.stream(MaskType.values())
                            .anyMatch(mask -> mask.datapackId() == expected),
                    "falta la mascara con id " + id);
        }
    }

    @Test
    void seResuelvenPorIdYLoDesconocidoNoRevienta() {
        assertEquals(Optional.of(MaskType.ANBU_1), MaskType.byId("anbu_1"));
        assertEquals(Optional.of(MaskType.TOBI_3), MaskType.byId("tobi_3"));
        assertEquals(Optional.empty(), MaskType.byId("anbu_9"));
    }
}
