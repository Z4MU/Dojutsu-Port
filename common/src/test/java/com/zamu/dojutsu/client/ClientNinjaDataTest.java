package com.zamu.dojutsu.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.network.JutsuSnapshot;
import com.zamu.dojutsu.network.ProgressionSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 42, 100, ProgressionSnapshot.EMPTY, JutsuSnapshot.EMPTY);
        assertTrue(ClientNinjaData.hasData());
        assertEquals(OnboardingState.Phase.COMPLETED, ClientNinjaData.onboarding().orElseThrow());
        assertEquals(42, ClientNinjaData.chakra());
        assertEquals(100, ClientNinjaData.maxChakra());
    }

    @Test
    void deltaAntesDelFullSeIgnora() {
        ClientNinjaData.applyDelta(OnboardingState.Phase.SELECTING_CLAN, 10, 100, ProgressionSnapshot.EMPTY, JutsuSnapshot.EMPTY);
        assertFalse(ClientNinjaData.hasData());
        assertTrue(ClientNinjaData.onboarding().isEmpty());
    }

    @Test
    void deltaTrasFullActualizaElValor() {
        ClientNinjaData.applyFull(OnboardingState.Phase.NOT_STARTED, 0, 100, ProgressionSnapshot.EMPTY, JutsuSnapshot.EMPTY);
        ClientNinjaData.applyDelta(OnboardingState.Phase.SELECTING_AFFINITY, 55, 100, ProgressionSnapshot.EMPTY, JutsuSnapshot.EMPTY);
        assertEquals(OnboardingState.Phase.SELECTING_AFFINITY, ClientNinjaData.onboarding().orElseThrow());
        assertEquals(55, ClientNinjaData.chakra());
    }

    @Test
    void clearVaciaLaCache() {
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withoutKnown(Map.of("chidori", 1400L), Optional.empty()));
        ClientNinjaData.clear();
        assertFalse(ClientNinjaData.hasData());
        assertTrue(ClientNinjaData.onboarding().isEmpty());
        assertEquals(0, ClientNinjaData.chakra());
        assertEquals(0L, ClientNinjaData.remainingCooldown("chidori", 1000L));
    }

    @Test
    void elCooldownRestanteSeCalculaConElTickDelCliente() {
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withoutKnown(Map.of("chidori", 1400L), Optional.empty())); // disponible en el tick 1400
        assertEquals(400L, ClientNinjaData.remainingCooldown("chidori", 1000L));
        assertEquals(1L, ClientNinjaData.remainingCooldown("chidori", 1399L));
        assertEquals(0L, ClientNinjaData.remainingCooldown("chidori", 1400L)); // vencido
        assertEquals(0L, ClientNinjaData.remainingCooldown("chidori", 1500L)); // ya pasado
        assertEquals(0L, ClientNinjaData.remainingCooldown("great_fireball", 1000L)); // sin cd registrado
    }

    // -----------------------------------------------------------------------------------------------
    // F11/DJT-231: lista de jutsus aprendidos. El delta corriente NO la trae, y eso no debe vaciarla.
    // -----------------------------------------------------------------------------------------------

    @Test
    void elFullTraeLosJutsusAprendidos() {
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withKnown(Map.of(), Set.of("chidori", "great_fireball"), Optional.empty()));

        assertEquals(List.of("chidori", "great_fireball"), ClientNinjaData.known());
        assertTrue(ClientNinjaData.knows("chidori"));
        assertFalse(ClientNinjaData.knows("rasengan"));
    }

    @Test
    void unDeltaSinListaCONSERVALaQueYaHabia() {
        // ESTE es el test que importa: los deltas los dispara la regeneracion de chakra, y llegan sin lista.
        // Si al recibirlos se vaciara, el selector de jutsu se quedaria en blanco a los pocos segundos.
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withKnown(Map.of(), Set.of("chidori"), Optional.empty()));

        ClientNinjaData.applyDelta(OnboardingState.Phase.COMPLETED, 81, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withoutKnown(Map.of(), Optional.empty()));

        assertEquals(List.of("chidori"), ClientNinjaData.known());
        assertEquals(81, ClientNinjaData.chakra()); // el resto del delta si se aplico
    }

    @Test
    void unDeltaConListaLaActualiza() {
        // al aprender un jutsu el servidor si la manda
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withKnown(Map.of(), Set.of("chidori"), Optional.empty()));

        ClientNinjaData.applyDelta(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withKnown(Map.of(), Set.of("chidori", "rasengan"), Optional.empty()));

        assertEquals(List.of("chidori", "rasengan"), ClientNinjaData.known());
    }

    // --- La rueda (F11/DJT-233): mismo peligro que la lista, mismo blindaje ---

    @Test
    void unDeltaSinRuedaCONSERVALaQueYaHabia() {
        List<String> rueda = List.of("chidori", "", "", "", "", "", "", "");
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.of(Map.of(), Optional.empty(), Optional.of(Set.of("chidori")),
                        Optional.of(rueda)));

        ClientNinjaData.applyDelta(OnboardingState.Phase.COMPLETED, 81, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withoutKnown(Map.of(), Optional.empty()));

        assertEquals(rueda, ClientNinjaData.quickSlots());
        assertEquals(Optional.of("chidori"), ClientNinjaData.slot(0));
        assertTrue(ClientNinjaData.slot(1).isEmpty());
    }

    @Test
    void unDeltaConRuedaLaActualiza() {
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.of(Map.of(), Optional.empty(), Optional.of(Set.of("chidori")),
                        Optional.of(List.of("chidori", "", "", "", "", "", "", ""))));

        ClientNinjaData.applyDelta(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.of(Map.of(), Optional.empty(), Optional.empty(),
                        Optional.of(List.of("", "chidori", "", "", "", "", "", ""))));

        assertTrue(ClientNinjaData.slot(0).isEmpty());
        assertEquals(Optional.of("chidori"), ClientNinjaData.slot(1));
    }

    @Test
    void clearOlvidaLosJutsusAprendidos() {
        // cambiar de mundo no puede dejar la lista de la partida anterior
        ClientNinjaData.applyFull(OnboardingState.Phase.COMPLETED, 80, 100, ProgressionSnapshot.EMPTY,
                JutsuSnapshot.withKnown(Map.of(), Set.of("chidori"), Optional.empty()));

        ClientNinjaData.clear();

        assertTrue(ClientNinjaData.known().isEmpty());
        assertFalse(ClientNinjaData.knows("chidori"));
    }
}
