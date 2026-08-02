package com.zamu.dojutsu.chakra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.ObservableField;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.domain.resource.RegenFactor;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests de la regeneracion de chakra (DJT-031): tabla de {@code [S02]}, timer persistido y chakra natural. */
class ChakraRegenTest {

    private static final Set<RegenFactor> NONE = EnumSet.noneOf(RegenFactor.class);

    /** Crea un jugador con un estado de chakra concreto, sin marcas de sucio. */
    private static PlayerNinjaData data(ChakraState state) {
        PlayerNinjaData d = new PlayerNinjaData();
        d.setChakra(state);
        d.dirtyTracker().clear();
        return d;
    }

    @Test
    void regenEstandarPulsaCada120Con3() {
        PlayerNinjaData d = data(ChakraState.DEFAULT);
        for (int i = 1; i < 120; i++) {
            assertFalse(ChakraService.advanceRegen(d, NONE), "sin pulso en el tick " + i);
        }
        assertEquals(0, d.chakra().chakra());
        assertEquals(119, d.chakra().chakraTimer());
        assertTrue(ChakraService.advanceRegen(d, NONE), "pulso en el tick 120");
        assertEquals(3, d.chakra().chakra());
        assertEquals(0, d.chakra().chakraTimer(), "el timer se reinicia tras el pulso");
    }

    @Test
    void regenUzumakiPulsaCada60Con6() {
        Set<RegenFactor> uzumaki = EnumSet.of(RegenFactor.CLAN_UZUMAKI);
        PlayerNinjaData d = data(ChakraState.DEFAULT);
        for (int i = 1; i < 60; i++) {
            ChakraService.advanceRegen(d, uzumaki);
        }
        assertEquals(0, d.chakra().chakra());
        assertTrue(ChakraService.advanceRegen(d, uzumaki));
        assertEquals(6, d.chakra().chakra());
    }

    @Test
    void alMaximoElPulsoNoCambiaNiSincroniza() {
        // Timer a punto de pulsar, pero chakra ya al maximo: el pulso se clampa (sin cambio) y no marca sucio.
        PlayerNinjaData d = data(new ChakraState(100, 100, 0, 100, 119, 0));
        assertFalse(ChakraService.advanceRegen(d, NONE));
        assertEquals(100, d.chakra().chakra());
        assertTrue(d.dirtyTracker().isEmpty());
    }

    @Test
    void soloElPulsoMarcaObservable() {
        PlayerNinjaData d = data(new ChakraState(0, 100, 0, 100, 118, 0));
        ChakraService.advanceRegen(d, NONE); // 118 -> 119: solo timer, no observable
        assertTrue(d.dirtyTracker().isEmpty());
        ChakraService.advanceRegen(d, NONE); // 119 -> 120: pulso +3
        assertTrue(d.dirtyTracker().isDirty(ObservableField.CHAKRA));
        assertEquals(3, d.chakra().chakra());
    }

    @Test
    void elTimerPersisteEntreTicks() {
        PlayerNinjaData d = data(ChakraState.DEFAULT);
        for (int i = 0; i < 45; i++) {
            ChakraService.advanceRegen(d, NONE);
        }
        assertEquals(45, d.chakra().chakraTimer());
    }

    @Test
    void jugoRegeneraUnoCadaDosTicks() {
        PlayerNinjaData d = data(ChakraState.DEFAULT);
        ChakraService.advanceNaturalRegen(d, true); // jugoTimer 0 -> 1 (tick de espera)
        assertEquals(0, d.chakra().naturalChakra());
        assertEquals(1, d.chakra().jugoTimer());
        ChakraService.advanceNaturalRegen(d, true); // +1 natural, jugoTimer -> 0
        assertEquals(1, d.chakra().naturalChakra());
        assertEquals(0, d.chakra().jugoTimer());
    }

    @Test
    void jugoInactivoNoCambiaNada() {
        PlayerNinjaData d = data(ChakraState.DEFAULT);
        ChakraService.advanceNaturalRegen(d, false);
        assertEquals(ChakraState.DEFAULT, d.chakra());
    }

    @Test
    void jugoParaAl99PorCiento() {
        PlayerNinjaData d = data(new ChakraState(0, 100, 99, 100, 0, 1));
        ChakraService.advanceNaturalRegen(d, true); // cap99 = 99, ya alcanzado -> sin cambio
        assertEquals(99, d.chakra().naturalChakra());
    }

    @Test
    void chakraNaturalNoEsObservable() {
        PlayerNinjaData d = data(new ChakraState(0, 100, 0, 100, 0, 1));
        ChakraService.advanceNaturalRegen(d, true); // +1 natural
        assertEquals(1, d.chakra().naturalChakra());
        assertTrue(d.dirtyTracker().isEmpty(), "el chakra natural no viaja al cliente");
    }
}
