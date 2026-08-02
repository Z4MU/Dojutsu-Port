package com.zamu.dojutsu.domain.bijuu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests del catalogo y las estadisticas de las Bestias con Cola (F7/DJT-160). Dominio puro, verificado contra
 * {@code tailed_beasts/spawn/*}, {@code main}, {@code ai/cast_1} y {@code death}.
 */
class BeastStatsTest {

    @Test
    void hayDiezBestiasNumeradasDeUnaADiez() {
        assertEquals(10, TailedBeast.values().length);
        Set<Integer> tails = new HashSet<>();
        for (TailedBeast beast : TailedBeast.values()) {
            tails.add(beast.tails());
        }
        assertEquals(10, tails.size());
        assertTrue(tails.contains(1));
        assertTrue(tails.contains(10));
    }

    @Test
    void cadaBestiaTieneSuPropiaTecnicaInsignia() {
        // cast_1 asigna una tecnica distinta a cada una: ninguna se repite
        Set<String> signatures = new HashSet<>();
        for (TailedBeast beast : TailedBeast.values()) {
            assertTrue(signatures.add(beast.signatureMove()),
                    "tecnica insignia repetida: " + beast.signatureMove());
        }
    }

    @Test
    void soloElDiezColasEsEspecial() {
        assertTrue(TailedBeast.TEN_TAILS.isTenTails());
        for (TailedBeast beast : EnumSet.complementOf(EnumSet.of(TailedBeast.TEN_TAILS))) {
            assertFalse(beast.isTenTails(), beast.id() + " no deberia ser el Diez Colas");
        }
    }

    @Test
    void elDiezColasAtacaMasSeguidoQueLasDemas() {
        assertEquals(BeastStats.AI_INTERVAL_TEN_TAILS, BeastStats.aiIntervalTicks(TailedBeast.TEN_TAILS));
        assertEquals(BeastStats.AI_INTERVAL_TICKS, BeastStats.aiIntervalTicks(TailedBeast.NINE_TAILS));
        assertTrue(BeastStats.AI_INTERVAL_TEN_TAILS < BeastStats.AI_INTERVAL_TICKS);
    }

    @Test
    void laRecompensaCreceConLasColas() {
        // death: ninja_xp = 90 + 10*colas ; experiencia = 1000 + 100*colas
        assertEquals(100, BeastStats.ninjaXpReward(TailedBeast.ONE_TAIL));
        assertEquals(180, BeastStats.ninjaXpReward(TailedBeast.NINE_TAILS));
        assertEquals(190, BeastStats.ninjaXpReward(TailedBeast.TEN_TAILS));
        assertEquals(1100, BeastStats.experienceReward(TailedBeast.ONE_TAIL));
        assertEquals(2000, BeastStats.experienceReward(TailedBeast.TEN_TAILS));
    }

    @Test
    void lasBestiasSeResuelvenPorIdYPorColas() {
        assertEquals(Optional.of(TailedBeast.NINE_TAILS), TailedBeast.byId("nine_tails"));
        assertEquals(Optional.of(TailedBeast.FOUR_TAILS), TailedBeast.byTails(4));
        assertTrue(TailedBeast.byId("eleven_tails").isEmpty()); // estado tolerante
        assertTrue(TailedBeast.byTails(0).isEmpty());
        assertTrue(TailedBeast.byTails(11).isEmpty());
    }

    @Test
    void lasDiezCompartenCuerpo() {
        // spawn_tb_entity es una sola funcion para las diez: el cuerpo no depende de la bestia
        assertEquals(1024.0, BeastStats.MAX_HEALTH);
        assertEquals(30.0, BeastStats.ARMOR);
        assertEquals(64.0, BeastStats.FOLLOW_RANGE);
    }
}
