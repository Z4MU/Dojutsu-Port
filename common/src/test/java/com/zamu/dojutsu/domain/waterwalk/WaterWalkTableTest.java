package com.zamu.dojutsu.domain.waterwalk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de caminar sobre el agua (F9/DJT-205). Verificado contra {@code water_walking/*} y el predicado
 * {@code can_walk_on_water}.
 *
 * <p>Lo que se blinda es justo lo que la ficha daba por otra cosa: que la "llave" es un <b>umbral de chakra</b>,
 * no un flag — y que son <b>dos</b> condiciones, no una.
 */
class WaterWalkTableTest {

    @Test
    void laLlaveSonDosUmbralesDeChakraNoUnFlag() {
        assertEquals(300, WaterWalkTable.REQUIRED_MAX_CHAKRA);
        assertEquals(2, WaterWalkTable.REQUIRED_CHAKRA);
        assertTrue(WaterWalkTable.canWalk(300, 2));
    }

    @Test
    void noBastaConTenerChakra_haceFaltaHaberCrecido() {
        // chakra de sobra pero deposito pequeño: aun no sabe hacerlo
        assertFalse(WaterWalkTable.canWalk(299, 299));
        assertTrue(WaterWalkTable.canWalk(300, 2));
    }

    @Test
    void niConDepositoGrandeSiEstasAgotado() {
        assertFalse(WaterWalkTable.canWalk(20000, 1));
        assertFalse(WaterWalkTable.canWalk(20000, 0));
        assertTrue(WaterWalkTable.canWalk(20000, 2));
    }

    @Test
    void seCobraUnoCadaTresTicks() {
        assertEquals(1, WaterWalkTable.CHAKRA_COST);
        assertEquals(3, WaterWalkTable.COST_INTERVAL_TICKS);
        // el contador va 0,1,2 y cobra al llegar a 2 (waterwalk_time >= 2), reiniciando
        assertFalse(WaterWalkTable.shouldCharge(0));
        assertFalse(WaterWalkTable.shouldCharge(1));
        assertTrue(WaterWalkTable.shouldCharge(2));
    }

    @Test
    void elAmplificadorNuncaPuedeSerNegativo() {
        // MobEffectInstance hace Mth.clamp(amplifier, 0, 255): no hay forma de pedir "velocidad objetivo 0",
        // asi que la levitacion no puede sostener por si sola y solo se usa para apagar la gravedad
        assertTrue(WaterWalkTable.HOVER_AMPLIFIER >= 0);
        assertEquals(0, WaterWalkTable.HOVER_AMPLIFIER, "el minimo, para que su empuje residual sea el menor");
        assertEquals(128, WaterWalkTable.SINK_AMPLIFIER);
    }

    @Test
    void laBandaDeSuperficieAguantaElEmpujeResidual() {
        double surface = 63.0;
        assertTrue(WaterWalkTable.nearSurface(63.0, surface));
        assertTrue(WaterWalkTable.nearSurface(63.5, surface), "subir un poco no debe soltarlo");
        assertTrue(WaterWalkTable.nearSurface(62.5, surface), "empezar a hundirse aun es recuperable");
        assertFalse(WaterWalkTable.nearSurface(64.0, surface));
        assertFalse(WaterWalkTable.nearSurface(62.0, surface));
    }

    @Test
    void elAnclajeDelServidorEsRedDeSeguridad_noCorreccionPorTick() {
        // quien sostiene la altura es el cliente (WaterWalkClient); el servidor solo interviene ante desvios
        // que el cliente no puede explicar. Un umbral fino convertia cada tick en un tiron: ese fue el bug.
        assertFalse(WaterWalkTable.shouldSnap(63.0, 63.0));
        assertFalse(WaterWalkTable.shouldSnap(63.05, 63.0), "una deriva pequeña NO debe corregirse");
        assertFalse(WaterWalkTable.shouldSnap(62.8, 63.0));
        assertTrue(WaterWalkTable.shouldSnap(63.5, 63.0), "un desvio grande si");
    }

    @Test
    void elAguaSeBuscaJustoBajoLosPies() {
        assertEquals(-0.35, WaterWalkTable.SURFACE_PROBE);
    }
}
