package com.zamu.dojutsu.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

/** Tests del pipeline de tick por jugador (DJT-021). */
class PlayerTickPipelineTest {

    /** Sistema fake que registra su nombre al ejecutarse (ignora al jugador). */
    private static final class RecordingSystem implements PlayerTickSystem {
        private final TickPhase phase;
        private final String name;
        private final List<String> log;

        RecordingSystem(TickPhase phase, String name, List<String> log) {
            this.phase = phase;
            this.name = name;
            this.log = log;
        }

        @Override
        public TickPhase phase() {
            return phase;
        }

        @Override
        public void tick(ServerPlayer player) {
            log.add(name);
        }
    }

    @Test
    void ejecutaEnOrdenDeFasePeseAlOrdenDeRegistro() {
        List<String> log = new ArrayList<>();
        PlayerTickPipeline pipeline = new PlayerTickPipeline();
        pipeline.register(new RecordingSystem(TickPhase.DOJUTSU, "dojutsu", log));
        pipeline.register(new RecordingSystem(TickPhase.CHAKRA, "chakra", log));
        pipeline.register(new RecordingSystem(TickPhase.STRAIN, "strain", log));

        pipeline.run(null); // los sistemas fake ignoran al jugador

        assertEquals(List.of("chakra", "strain", "dojutsu"), log);
    }

    @Test
    void chakraCorrePrimero() {
        List<String> log = new ArrayList<>();
        PlayerTickPipeline pipeline = new PlayerTickPipeline();
        pipeline.register(new RecordingSystem(TickPhase.PROGRESSION, "progression", log));
        pipeline.register(new RecordingSystem(TickPhase.UNLOCKS, "unlocks", log));
        pipeline.register(new RecordingSystem(TickPhase.CHAKRA, "chakra", log));

        pipeline.run(null);

        assertEquals("chakra", log.get(0));
    }

    @Test
    void mismaFaseConservaOrdenDeRegistro() {
        List<String> log = new ArrayList<>();
        PlayerTickPipeline pipeline = new PlayerTickPipeline();
        pipeline.register(new RecordingSystem(TickPhase.PROGRESSION, "primero", log));
        pipeline.register(new RecordingSystem(TickPhase.PROGRESSION, "segundo", log));

        pipeline.run(null);

        assertEquals(List.of("primero", "segundo"), log);
    }

    @Test
    void registroDinamicoRespetaLaPrioridad() {
        List<String> log = new ArrayList<>();
        PlayerTickPipeline pipeline = new PlayerTickPipeline();
        pipeline.register(new RecordingSystem(TickPhase.PROGRESSION, "progression", log));
        // Un sistema registrado despues, pero de fase anterior, corre antes:
        pipeline.register(new RecordingSystem(TickPhase.CHAKRA, "chakra", log));

        pipeline.run(null);

        assertEquals(List.of("chakra", "progression"), log);
    }

    @Test
    void pipelineVacioNoHaceNada() {
        PlayerTickPipeline pipeline = new PlayerTickPipeline();
        pipeline.run(null); // no debe lanzar
        assertEquals(List.of(), pipeline.systems());
    }
}
