package com.zamu.dojutsu.domain.bijuu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Tests de las técnicas insignia de las bestias (F10/DJT-226). Verificado contra las carpetas
 * {@code tailed_beasts/ai/moves/} ({@code summon}, {@code move}, {@code run}) y
 * {@code tailed_beasts/main.mcfunction}.
 */
class BeastSignatureTableTest {

    @Test
    void nueveDeLasDiezBestiasTienenAndanada() {
        // el Diez Colas no: su insignia es el Tenpenchii, un area de rayos, no un proyectil
        long conAndanada = Arrays.stream(TailedBeast.values())
                .filter(BeastSignatureTable::hasBarrage)
                .count();
        assertEquals(9, conAndanada);
        assertFalse(BeastSignatureTable.hasBarrage(TailedBeast.TEN_TAILS));
    }

    @Test
    void elDisparoViveDiezTicksSalvoElDelNueveColas() {
        // d.<N>t_<xx>timer = 10 en las ocho primeras; d.9t_cabtimer = 20
        for (TailedBeast beast : TailedBeast.values()) {
            if (beast == TailedBeast.NINE_TAILS || beast.isTenTails()) {
                continue;
            }
            assertEquals(10, BeastSignatureTable.flightTicks(beast), beast.id());
        }
        assertEquals(20, BeastSignatureTable.flightTicks(TailedBeast.NINE_TAILS));
    }

    @Test
    void elNueveColasLlegaElDobleDeLejos() {
        assertEquals(2 * BeastSignatureTable.flightTicks(TailedBeast.ONE_TAIL),
                BeastSignatureTable.flightTicks(TailedBeast.NINE_TAILS));
        assertNotEquals(BeastSignatureTable.rangeBlocks(TailedBeast.ONE_TAIL),
                BeastSignatureTable.rangeBlocks(TailedBeast.NINE_TAILS));
    }

    @Test
    void sonTecnicasDeCortaDistancia() {
        // (10 - 3) x 1.2 = 8.4 bloques: el nombre "perdigonada" no era decorativo
        assertEquals(8.4D, BeastSignatureTable.rangeBlocks(TailedBeast.ONE_TAIL), 1e-4); // la velocidad es float: la tolerancia es la de un float, no la de un double
        assertEquals(20.4D, BeastSignatureTable.rangeBlocks(TailedBeast.NINE_TAILS), 1e-4); // la velocidad es float: la tolerancia es la de un float, no la de un double
    }

    @Test
    void lasNueveCompartenTodosLosNumerosMenosLaVida() {
        // el hallazgo del ticket: solo cambian las particulas, y en el Nueve Colas la vida
        assertEquals(1.2F, BeastSignatureTable.SPEED_BLOCKS_PER_TICK);
        assertEquals(5.0D, BeastSignatureTable.BLAST_RADIUS);
        assertEquals(1, BeastSignatureTable.DAMAGE_AMPLIFIER); // instant_damage 1 1 = Daño II
        assertEquals(3, BeastSignatureTable.CHARGE_TICKS);
    }

    @Test
    void elJinchurikiPagaLoQueLaBestiaNo() {
        // form/one_tail/sand_buckshot/cast: chakra >= 40, d.jinform_timer = 100
        assertEquals(40, BeastSignatureTable.CHAKRA_COST);
        assertEquals(100, BeastSignatureTable.COOLDOWN_TICKS);
    }

    @Test
    void cadaBestiaConAndanadaDeclaraSuInsignia() {
        // el id de la tecnica sale del enum: si divergen, el registro no encuentra la implementacion
        for (TailedBeast beast : TailedBeast.values()) {
            if (BeastSignatureTable.hasBarrage(beast)) {
                assertTrue(beast.signatureMove() != null && !beast.signatureMove().isBlank(), beast.id());
            }
        }
    }
}
