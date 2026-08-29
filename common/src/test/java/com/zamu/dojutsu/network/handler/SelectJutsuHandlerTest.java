package com.zamu.dojutsu.network.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.network.SelectJutsuPayload;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests de las partes puras del handler del selector de jutsu (F11/DJT-231): anti-spam y la convencion de
 * "cadena vacia = retirar". La decision de si la seleccion prende vive en {@code JutsuSelectionService}.
 */
class SelectJutsuHandlerTest {

    @Test
    void laPrimeraSeleccionNoEstaLimitada() {
        assertFalse(SelectJutsuHandler.isRateLimited(UUID.randomUUID(), 1_000L));
    }

    @Test
    void seleccionDemasiadoSeguidaSeLimita() {
        UUID player = UUID.randomUUID();
        assertFalse(SelectJutsuHandler.isRateLimited(player, 1_000L));
        assertTrue(SelectJutsuHandler.isRateLimited(player, 1_000L + SelectJutsuHandler.MIN_INTERVAL_MS - 1));
        assertFalse(SelectJutsuHandler.isRateLimited(player, 1_000L + SelectJutsuHandler.MIN_INTERVAL_MS));
    }

    @Test
    void elLimiteEsPorJugadorYForgetLoLimpia() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertFalse(SelectJutsuHandler.isRateLimited(a, 1_000L));
        assertFalse(SelectJutsuHandler.isRateLimited(b, 1_000L)); // otro jugador no se ve afectado
        SelectJutsuHandler.forget(a);
        assertFalse(SelectJutsuHandler.isRateLimited(a, 1_000L)); // sin memoria previa
    }

    @Test
    void laCadenaVaciaSignificaRetirarLaSeleccion() {
        assertTrue(SelectJutsuPayload.clear().isClear());
        assertEquals("", SelectJutsuPayload.clear().jutsuId());
        assertFalse(new SelectJutsuPayload("chidori").isClear());
    }
}
