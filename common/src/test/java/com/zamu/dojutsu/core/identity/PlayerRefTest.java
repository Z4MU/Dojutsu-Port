package com.zamu.dojutsu.core.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tests de la identidad por UUID (DJT-020). */
class PlayerRefTest {

    private static final UUID ONLINE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OFFLINE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** Lookup fake: solo ONLINE resuelve (simula la lista de jugadores conectados). */
    private static final Map<UUID, String> CONNECTED = Map.of(ONLINE, "jugador-online");

    @Test
    void guardaElUuid() {
        assertEquals(ONLINE, PlayerRef.of(ONLINE).uuid());
    }

    @Test
    void resuelveAlJugadorOnline() {
        var resolved = PlayerRef.of(ONLINE).resolve(CONNECTED::get);
        assertTrue(resolved.isPresent());
        assertEquals("jugador-online", resolved.get());
    }

    @Test
    void jugadorOfflineResuelveVacio() {
        assertTrue(PlayerRef.of(OFFLINE).resolve(CONNECTED::get).isEmpty());
    }

    @Test
    void dosReferenciasConElMismoUuidSonIguales() {
        assertEquals(PlayerRef.of(ONLINE), PlayerRef.of(ONLINE));
        assertEquals(PlayerRef.of(ONLINE).hashCode(), PlayerRef.of(ONLINE).hashCode());
    }

    @Test
    void uuidNuloEsRechazado() {
        assertThrows(NullPointerException.class, () -> PlayerRef.of((UUID) null));
    }
}
