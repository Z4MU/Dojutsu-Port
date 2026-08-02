package com.zamu.dojutsu.sage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.domain.sage.NaturalEnergyTable;
import com.zamu.dojutsu.domain.sage.SageType;
import org.junit.jupiter.api.Test;

/**
 * Tests de la decision pura de reunir energia natural (F6/DJT-135): espera previa, pulsos por nivel, aceite,
 * interrupcion al moverse y tope. Los valores son los verificados del datapack ({@link NaturalEnergyTable}).
 */
class NaturalEnergyChargingServiceTest {

    /** Estado quieto justo antes del primer pulso (espera previa ya cumplida). */
    private static SageState afterWait(int level, boolean oiled) {
        return SageState.DEFAULT.withLevel(level).withOiled(oiled)
                .withStillTicks(NaturalEnergyTable.STILL_WAIT_TICKS);
    }

    @Test
    void moverseReiniciaElAcumuladoDeQuietud() {
        SageState moving = SageState.DEFAULT.withStillTicks(500);
        assertEquals(0, NaturalEnergyChargingService.charge(moving, false).stillTicks());
    }

    @Test
    void moverseSinAcumuladoNoCreaCopia() {
        SageState idle = SageState.DEFAULT;
        assertSame(idle, NaturalEnergyChargingService.charge(idle, false));
    }

    @Test
    void primeroHayQueAsentarseAntesDeReunirNada() {
        SageState state = SageState.DEFAULT;
        for (int i = 0; i < NaturalEnergyTable.STILL_WAIT_TICKS; i++) {
            state = NaturalEnergyChargingService.charge(state, true);
        }
        assertEquals(NaturalEnergyTable.STILL_WAIT_TICKS, state.stillTicks());
        assertEquals(0, state.charge(), "durante la espera previa no se reune energia");
    }

    @Test
    void pasadaLaEsperaSeGanaUnPuntoCadaIntervaloDelNivel() {
        // nivel 0 sin aceite: 800 ticks por punto
        SageState state = afterWait(0, false);
        for (int i = 0; i < NaturalEnergyTable.gatherTicks(0, false); i++) {
            state = NaturalEnergyChargingService.charge(state, true);
        }
        assertEquals(1, state.charge());
    }

    @Test
    void elAceiteDeSapoDuplicaLaVelocidadDeCarga() {
        // con aceite el intervalo del nivel 0 es 400 (la mitad de 800)
        SageState state = afterWait(0, true);
        for (int i = 0; i < NaturalEnergyTable.gatherTicks(0, true); i++) {
            state = NaturalEnergyChargingService.charge(state, true);
        }
        assertEquals(1, state.charge());
        assertEquals(400, NaturalEnergyTable.gatherTicks(0, true));
        assertEquals(800, NaturalEnergyTable.gatherTicks(0, false));
    }

    @Test
    void alMaximoSeguirQuietoNoSuma() {
        SageState full = SageState.DEFAULT.withCharge(NaturalEnergyTable.MAX_CHARGE)
                .withStillTicks(NaturalEnergyTable.STILL_WAIT_TICKS + 5000);
        assertSame(full, NaturalEnergyChargingService.charge(full, true));
        assertTrue(full.fullyCharged());
    }

    // --- tabla verificada ---

    @Test
    void laTablaDeIntervalosEsLaDelDatapack() {
        // GatherTimeLevel_0..5 = 800, 400, 200, 100, 50, 25 (se halva por nivel)
        assertEquals(800, NaturalEnergyTable.gatherTicks(0, false));
        assertEquals(400, NaturalEnergyTable.gatherTicks(1, false));
        assertEquals(200, NaturalEnergyTable.gatherTicks(2, false));
        assertEquals(100, NaturalEnergyTable.gatherTicks(3, false));
        assertEquals(50, NaturalEnergyTable.gatherTicks(4, false));
        assertEquals(25, NaturalEnergyTable.gatherTicks(5, false));
    }

    @Test
    void laTablaConAceiteEsLaDelDatapack() {
        // GatherTimeLevel_WithOil_0..5 = 400, 200, 100, 50, 25, 12 (el ultimo es 12, no 12.5)
        assertEquals(400, NaturalEnergyTable.gatherTicks(0, true));
        assertEquals(200, NaturalEnergyTable.gatherTicks(1, true));
        assertEquals(100, NaturalEnergyTable.gatherTicks(2, true));
        assertEquals(50, NaturalEnergyTable.gatherTicks(3, true));
        assertEquals(25, NaturalEnergyTable.gatherTicks(4, true));
        assertEquals(12, NaturalEnergyTable.gatherTicks(5, true));
    }

    @Test
    void unNivelFueraDeRangoSeClampa() {
        assertEquals(800, NaturalEnergyTable.gatherTicks(-3, false));
        assertEquals(25, NaturalEnergyTable.gatherTicks(99, false));
    }

    // --- estado ---

    @Test
    void volverseSabioFijaLinajeYNivelUno() {
        SageState sage = SageState.DEFAULT.withCharge(NaturalEnergyTable.MAX_CHARGE).becomeSage(SageType.SNAKE);
        assertTrue(sage.sage());
        assertEquals(SageType.SNAKE, sage.type().orElseThrow());
        assertEquals(1, sage.level());
    }

    @Test
    void elTipoDeSabioSeResuelvePorSuId() {
        assertEquals(SageType.TOAD, SageType.byId("toad").orElseThrow());
        assertEquals(SageType.SLUG, SageType.byId("slug").orElseThrow());
        assertTrue(SageType.byId("dragon").isEmpty()); // desconocido: tolerante
    }

    @Test
    void elEstadoSeNormalizaAlRango() {
        assertEquals(NaturalEnergyTable.MAX_CHARGE, SageState.DEFAULT.withCharge(999).charge());
        assertEquals(0, SageState.DEFAULT.withCharge(-5).charge());
        assertEquals(NaturalEnergyTable.MAX_SAGE_LEVEL, SageState.DEFAULT.withLevel(99).level());
        assertFalse(SageState.DEFAULT.fullyCharged());
    }
}
