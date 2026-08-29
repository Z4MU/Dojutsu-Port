package com.zamu.dojutsu.network.handler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.JutsuKnowledge;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Tests de las partes puras del handler de la tecla de cast (F4/DJT-080): anti-spam y de donde sale el jutsu
 * que se lanza. El glue con {@code ServerPlayer} se valida en runtime.
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

    // --- De donde sale el jutsu (F11/DJT-231: del estado, ya no del item de la mano) ---

    @Test
    void sinSeleccionNoHayNadaQueLanzar() {
        assertTrue(JutsuKnowledge.DEFAULT.selectedJutsu().isEmpty());
    }

    @Test
    void laSeleccionQueLlegaAlPipelineEstaYaValidadaContraLoConocido() {
        // La puerta "solo se castea lo conocido" no se perdio al mudarse al selector: la sostiene el estado,
        // asi que una seleccion que deje de estar entre los aprendidos no puede llegar al cast.
        JutsuKnowledge knowledge = JutsuKnowledge.DEFAULT.withKnown("fire_bullet").withSelected("fire_bullet");
        assertTrue(knowledge.selectedJutsu().isPresent());
        assertFalse(knowledge.withSelected("great_fireball").selectedJutsu()
                .filter("great_fireball"::equals).isPresent()); // no aprendido -> descartado
    }
}
