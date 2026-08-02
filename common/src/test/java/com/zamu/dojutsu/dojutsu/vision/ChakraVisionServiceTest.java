package com.zamu.dojutsu.dojutsu.vision;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la puerta pura de la chakra vision (DJT-125): resalta solo si el ojo la da con su uso, no esta
 * silenciada y hay chakra. La aplicacion del glow/equipo es glue con Minecraft y no se testea aqui.
 */
class ChakraVisionServiceTest {

    @Test
    void resaltaConOjoQueLaDaVisionEncendidaYChakra() {
        assertTrue(ChakraVisionService.shouldHighlight(true, false, true));
    }

    @Test
    void noResaltaSiElOjoNoLaDa() {
        // Ketsuryugan, o Sharingan base: grantsVision=false
        assertFalse(ChakraVisionService.shouldHighlight(false, false, true));
    }

    @Test
    void noResaltaSiEstaSilenciada() {
        // el jugador la apago (d.no_cv)
        assertFalse(ChakraVisionService.shouldHighlight(true, true, true));
    }

    @Test
    void noResaltaSinChakra() {
        // el datapack gatea la vision unless no_chakra
        assertFalse(ChakraVisionService.shouldHighlight(true, false, false));
    }
}
