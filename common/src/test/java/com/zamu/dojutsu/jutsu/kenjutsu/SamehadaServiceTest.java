package com.zamu.dojutsu.jutsu.kenjutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la logica pura de la absorcion de la Samehada (F4/DJT-084). El glue (evento de daño, lectura del
 * item en mano, sync) se valida en runtime (necesita {@code ServerPlayer}/{@code ItemStack}).
 */
class SamehadaServiceTest {

    @Test
    void deUnJugadorConMuchoChakraDrenaComoMuchoElTope() {
        // absorb_ply: hasta 100 (ABSORB_PER_HIT).
        assertEquals(100, SamehadaService.drainFromPlayer(500));
        assertEquals(100, SamehadaService.drainFromPlayer(100));
    }

    @Test
    void deUnJugadorConPocoChakraDrenaSoloLoQueTiene() {
        assertEquals(40, SamehadaService.drainFromPlayer(40));
        assertEquals(1, SamehadaService.drainFromPlayer(1));
    }

    @Test
    void deUnJugadorSinChakraNoDrenaNada() {
        assertEquals(0, SamehadaService.drainFromPlayer(0));
        assertEquals(0, SamehadaService.drainFromPlayer(-5)); // NBT corrupto: nunca negativo
    }

    @Test
    void elTopeYRequisitoCoincidenConElDatapackReescalado() {
        assertEquals(100, SamehadaService.ABSORB_PER_HIT); // +100 fijo de mob / tope de drenaje
        assertEquals(250, SamehadaService.KENJUTSU_REQ);    // 1000 datapack ÷4
        assertEquals("samehada", SamehadaService.WEAPON_TYPE);
    }
}
