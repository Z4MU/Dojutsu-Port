package com.zamu.dojutsu.network.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.JutsuKnowledge;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests de las partes puras del handler de la tecla de cast (F4/DJT-080): anti-spam y la puerta
 * "solo se castea lo que se conoce". El glue con {@code ServerPlayer}/inventario se valida en runtime.
 */
class CastJutsuHandlerTest {

    // --- Anti-spam ---

    @Test
    void laPrimeraPulsacionNoEstaLimitada() {
        assertFalse(CastJutsuHandler.isRateLimited(UUID.randomUUID(), 1_000L));
    }

    @Test
    void pulsacionDemasiadoSeguidaSeLimita() {
        UUID player = UUID.randomUUID();
        assertFalse(CastJutsuHandler.isRateLimited(player, 1_000L));
        assertTrue(CastJutsuHandler.isRateLimited(player, 1_000L + CastJutsuHandler.MIN_INTERVAL_MS - 1));
        assertFalse(CastJutsuHandler.isRateLimited(player, 1_000L + CastJutsuHandler.MIN_INTERVAL_MS));
    }

    @Test
    void elLimiteEsPorJugadorYForgetLoLimpia() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertFalse(CastJutsuHandler.isRateLimited(a, 1_000L));
        assertFalse(CastJutsuHandler.isRateLimited(b, 1_000L)); // otro jugador no se ve afectado
        CastJutsuHandler.forget(a);
        assertFalse(CastJutsuHandler.isRateLimited(a, 1_000L)); // sin memoria previa
    }

    // --- Puerta "solo se castea lo conocido" (la valida el handler contra JutsuKnowledge) ---

    @Test
    void soloSeCasteaUnJutsuConocido() {
        JutsuKnowledge knowledge = JutsuKnowledge.DEFAULT.withKnown("fire_bullet");
        assertTrue(knowledge.knows("fire_bullet"));   // aprendido con el learner -> casteable
        assertFalse(knowledge.knows("great_fireball")); // item por comando/residuo -> rechazado
    }
}
