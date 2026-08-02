package com.zamu.dojutsu.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests del otorgamiento reactivo e idempotente de desbloqueos (DJT-038). Puro sobre el estado. */
class UnlockServiceTest {

    @BeforeEach
    void loadRegistry() {
        UnlockRegistry.load(List.of(
                new UnlockDefinition("kenjutsu_1", "kenjutsu", 5),
                new UnlockDefinition("kenjutsu_2", "kenjutsu", 10),
                new UnlockDefinition("kenjutsu_3", "kenjutsu", 20),
                new UnlockDefinition("fuinjutsu_1", "fuinjutsu", 5)));
    }

    @Test
    void cruzarUnHitoOtorgaElDesbloqueo() {
        PlayerNinjaData data = new PlayerNinjaData();
        List<String> granted = UnlockService.applyUnlocks(data, "kenjutsu", 0, 5);
        assertEquals(List.of("kenjutsu_1"), granted);
        assertTrue(data.progression().unlocked().contains("kenjutsu_1"));
    }

    @Test
    void variosHitosDeUnaVez() {
        PlayerNinjaData data = new PlayerNinjaData();
        List<String> granted = UnlockService.applyUnlocks(data, "kenjutsu", 0, 20);
        assertEquals(3, granted.size()); // kenjutsu_1 (5), _2 (10), _3 (20)
        assertTrue(data.progression().unlocked().containsAll(List.of("kenjutsu_1", "kenjutsu_2", "kenjutsu_3")));
    }

    @Test
    void esIdempotente() {
        PlayerNinjaData data = new PlayerNinjaData();
        UnlockService.applyUnlocks(data, "kenjutsu", 0, 5);
        List<String> again = UnlockService.applyUnlocks(data, "kenjutsu", 0, 5);
        assertTrue(again.isEmpty()); // ya estaba: no re-otorga
        assertEquals(1, data.progression().unlocked().size());
    }

    @Test
    void sinHitoEnElRangoNoOtorga() {
        PlayerNinjaData data = new PlayerNinjaData();
        assertTrue(UnlockService.applyUnlocks(data, "kenjutsu", 0, 4).isEmpty()); // hito en 5, no llega
        assertTrue(data.progression().unlocked().isEmpty());
    }

    @Test
    void otraHabilidadNoDispara() {
        PlayerNinjaData data = new PlayerNinjaData();
        List<String> granted = UnlockService.applyUnlocks(data, "fuinjutsu", 0, 10);
        assertEquals(List.of("fuinjutsu_1"), granted); // solo el hito de fuinjutsu, no los de kenjutsu
    }

    @Test
    void bajarDeNivelNoRevoca() {
        PlayerNinjaData data = new PlayerNinjaData();
        UnlockService.applyUnlocks(data, "kenjutsu", 0, 10); // otorga kenjutsu_1 y _2
        List<String> granted = UnlockService.applyUnlocks(data, "kenjutsu", 10, 5); // "baja"
        assertTrue(granted.isEmpty());
        assertEquals(2, data.progression().unlocked().size()); // no se revoca nada
    }
}
