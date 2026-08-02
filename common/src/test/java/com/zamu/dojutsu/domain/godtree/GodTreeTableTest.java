package com.zamu.dojutsu.domain.godtree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

/**
 * Tests del Arbol Divino (F9/DJT-215). Verificado contra {@code god_tree/{main,grow}} y
 * {@code plant_god_tree/{cast,summon}}.
 *
 * <p>Lo que se blinda: el <b>orden</b> de las estructuras (una etapa cambiada de sitio pasa desapercibida hasta
 * que alguien ve crecer el arbol al reves) y los <b>desplazamientos</b>, que no son iguales para el brote y para
 * el arbol.
 */
class GodTreeTableTest {

    @Test
    void sonCincoEtapasDeCincoMinutos() {
        assertEquals(5, GodTreeTable.MAX_STAGE);
        assertEquals(6000, GodTreeTable.TICKS_PER_STAGE);
        assertEquals(30000, GodTreeTable.TOTAL_TICKS);
    }

    @Test
    void elMensajeDiceQuinceMinutosPeroSonVeinticinco() {
        // anomalia del datapack portada: manda el numero de etapas, no el texto
        assertEquals(25, GodTreeTable.TOTAL_TICKS / 20 / 60);
    }

    @Test
    void cadaEtapaTieneSuEstructuraYNoSeRepiten() {
        assertEquals(GodTreeTable.MAX_STAGE, GodTreeTable.STAGE_STRUCTURES.size());
        assertEquals(GodTreeTable.MAX_STAGE, new HashSet<>(GodTreeTable.STAGE_STRUCTURES).size());
        assertEquals("god_tree/sapling/god_sapling_1", GodTreeTable.structureForStage(1));
        assertEquals("god_tree/god_tree_0", GodTreeTable.structureForStage(2));
        assertEquals("god_tree/god_tree_3", GodTreeTable.structureForStage(5));
    }

    @Test
    void unaEtapaFueraDeRangoNoSeInventaEstructura() {
        assertThrows(IllegalArgumentException.class, () -> GodTreeTable.structureForStage(0));
        assertThrows(IllegalArgumentException.class, () -> GodTreeTable.structureForStage(6));
    }

    @Test
    void elBroteYElArbolNoSeColocanIgual() {
        // grow: la etapa 1 usa posX/posZ -3; de la 2 en adelante, -8
        assertEquals(-3, GodTreeTable.offsetForStage(1));
        assertEquals(-8, GodTreeTable.offsetForStage(2));
        assertEquals(-8, GodTreeTable.offsetForStage(5));
        // y el que se planta va a -1 (plant_god_tree/summon)
        assertEquals(-1, GodTreeTable.SAPLING_OFFSET);
    }

    @Test
    void creceAlLlegarAlUmbralYFloreceEnLaUltima() {
        assertFalse(GodTreeTable.readyToGrow(GodTreeTable.TICKS_PER_STAGE - 1));
        assertTrue(GodTreeTable.readyToGrow(GodTreeTable.TICKS_PER_STAGE));
        assertFalse(GodTreeTable.isBloomed(4));
        assertTrue(GodTreeTable.isBloomed(5));
        assertTrue(GodTreeTable.isBloomed(6), "godtree_stage_6p vuelve a clampar a 5");
    }

    @Test
    void plantarCuestaMilChakra() {
        assertEquals(1000, GodTreeTable.PLANT_CHAKRA);
        assertEquals(300, GodTreeTable.PLANT_COOLDOWN_TICKS);
        assertEquals("rinne_sharingan", GodTreeTable.REWARD_EYE);
    }
}
