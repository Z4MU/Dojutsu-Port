package com.zamu.dojutsu.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * La puerta compartida de sonido {@code d.sfx} (F10/DJT-227).
 *
 * <p>Solo se prueba la <b>decision</b>: reproducir de verdad necesita mundo y jugador, y eso va al checklist de
 * verificacion en partida como todo lo que un test no ve.
 */
class NinjaSfxGateTest {

    @Test
    void sinPuertaArmadaSuena() {
        assertTrue(NinjaSfx.isOpen(Long.MIN_VALUE, 0L), "nadie ha disparado todavia");
    }

    @Test
    void dentroDeLaVentanaNoSuena() {
        long armadaEn = 100L;
        long vuelveEn = armadaEn + NinjaSfx.GATE_TICKS; // 120
        assertFalse(NinjaSfx.isOpen(vuelveEn, 101L), "un tick despues sigue cerrada");
        assertFalse(NinjaSfx.isOpen(vuelveEn, 119L), "el tick anterior al limite sigue cerrada");
    }

    @Test
    void alCumplirseLaVentanaVuelveASonar() {
        // El datapack resta 1 por tick y el predicate exige min 1: al llegar a 0 vuelve a pasar. Aqui el
        // limite es inclusivo por el mismo motivo.
        assertTrue(NinjaSfx.isOpen(120L, 120L), "el tick exacto ya abre");
        assertTrue(NinjaSfx.isOpen(120L, 500L));
    }

    @Test
    void laVentanaPorDefectoEsLaDelDatapack() {
        // scoreboard players set @s d.sfx 20, en las catorce llamadas gateadas.
        org.junit.jupiter.api.Assertions.assertEquals(20, NinjaSfx.GATE_TICKS);
    }
}
