package com.zamu.dojutsu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.network.ProgressionSnapshot;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests de la cache read-only del cliente (DJT-028, +cooldowns DJT-081). Estado estatico: se limpia antes. */
class ClientNinjaDataTest {

    @BeforeEach
    void reset() {
        ClientNinjaData.clear();
    }

    @Test
    void sinSyncNoHayDatos() {
        assertFalse(ClientNinjaData.hasData());
        assertTrue(ClientNinjaData.onboarding().isEmpty());
    }

    @Test
    void fullHabilitaYGuardaElValor() {
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 42, 100, ProgressionSnapshot.EMPTY, Map.of());
        assertTrue(ClientNinjaData.hasData());
        assertEquals(OnboardingState.Phase.COMPLETED, ClientNinjaData.onboarding().orElseThrow());
        assertEquals(42, ClientNinjaData.chakra());
        assertEquals(100, ClientNinjaData.maxChakra());
    }

    @Test
    void deltaAntesDelFullSeIgnora() {
        ClientNinjaData.applyDelta(OnboardingState.Phase.SELECTING_CLAN, 10, 100, ProgressionSnapshot.EMPTY, Map.of());
        assertFalse(ClientNinjaData.hasData());
        assertTrue(ClientNinjaData.onboarding().isEmpty());
    }

    @Test
    void deltaTrasFullActualizaElValor() {
        ClientNinjaData.applyFull(OnboardingState.Phase.NOT_STARTED, 0, 100, ProgressionSnapshot.EMPTY, Map.of());
        ClientNinjaData.applyDelta(OnboardingState.Phase.SELECTING_AFFINITY, 55, 100, ProgressionSnapshot.EMPTY, Map.of());
        assertEquals(OnboardingState.Phase.SELECTING_AFFINITY, ClientNinjaData.onboarding().orElseThrow());
        assertEquals(55, ClientNinjaData.chakra());
    }

    @Test
    void clearVaciaLaCache() {
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                Map.of("chidori", 1400L));
        ClientNinjaData.clear();
        assertFalse(ClientNinjaData.hasData());
        assertTrue(ClientNinjaData.onboarding().isEmpty());
        assertEquals(0, ClientNinjaData.chakra());
        assertEquals(0L, ClientNinjaData.remainingCooldown("chidori", 1000L));
    }

    @Test
    void elCooldownRestanteSeCalculaConElTickDelCliente() {
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                Map.of("chidori", 1400L)); // disponible en el tick 1400
        assertEquals(400L, ClientNinjaData.remainingCooldown("chidori", 1000L));
        assertEquals(1L, ClientNinjaData.remainingCooldown("chidori", 1399L));
        assertEquals(0L, ClientNinjaData.remainingCooldown("chidori", 1400L)); // vencido
        assertEquals(0L, ClientNinjaData.remainingCooldown("chidori", 1500L)); // ya pasado
        assertEquals(0L, ClientNinjaData.remainingCooldown("great_fireball", 1000L)); // sin cd registrado
    }
}
