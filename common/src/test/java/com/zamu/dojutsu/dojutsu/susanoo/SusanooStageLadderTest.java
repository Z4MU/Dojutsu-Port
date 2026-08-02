package com.zamu.dojutsu.dojutsu.susanoo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zamu.dojutsu.domain.susanoo.SusanooCostTable;
import com.zamu.dojutsu.dojutsu.susanoo.SusanooService.ToggleAction;
import org.junit.jupiter.api.Test;

/**
 * Tests de la escalera de etapas del Susanoo (DJT-121): la decision de que hace la siguiente pulsacion.
 * Reproduce el {@code advance.mcfunction} del datapack (crecer con tope en el nivel dominado) mas la
 * divergencia consciente del apagado en el tope.
 */
class SusanooStageLadderTest {

    @Test
    void apagadoLaPulsacionLoManifiesta() {
        assertEquals(ToggleAction.ACTIVATE, SusanooService.decide(false, 0, 1));
        assertEquals(ToggleAction.ACTIVATE, SusanooService.decide(false, 3, 6));
    }

    @Test
    void activoPorDebajoDelNivelDominadoCrece() {
        assertEquals(ToggleAction.ADVANCE, SusanooService.decide(true, 1, 2));
        assertEquals(ToggleAction.ADVANCE, SusanooService.decide(true, 5, 6));
    }

    @Test
    void enElTopeLaSiguientePulsacionLoApaga() {
        // divergencia consciente: el datapack no hacia nada aqui y se apagaba desde el menu; con una sola
        // tecla, una escalera sin salida dejaria al jugador sin forma de bajarse.
        assertEquals(ToggleAction.DEACTIVATE, SusanooService.decide(true, 1, 1));
        assertEquals(ToggleAction.DEACTIVATE,
                SusanooService.decide(true, SusanooCostTable.MAX_LEVEL, SusanooCostTable.MAX_LEVEL));
    }

    @Test
    void unaEtapaPorEncimaDelNivelNoSigueCreciendo() {
        // estado imposible por construccion, pero no debe dejar la escalera sin salida
        assertEquals(ToggleAction.DEACTIVATE, SusanooService.decide(true, 6, 3));
    }

    @Test
    void conMaestriaUnoElSusanooEsUnInterruptorSimple() {
        // nivel 1 dominado: manifestar y apagar, sin etapas intermedias
        assertEquals(ToggleAction.ACTIVATE, SusanooService.decide(false, 0, 1));
        assertEquals(ToggleAction.DEACTIVATE, SusanooService.decide(true, 1, 1));
    }
}
