package com.zamu.dojutsu.domain.bijuu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests del selector de tecnicas de la IA de las bestias (F7/DJT-161). Toda la decision es pura, asi que los
 * cuatro slots, los turnos vacios y las excepciones del Diez Colas se prueban valor a valor, sin Minecraft.
 * Verificado contra {@code tailed_beasts/ai/get_move} + {@code cast_1..4}.
 */
class BeastAiPlanTest {

    /** Generador que devuelve una tirada fijada. */
    private static final class ScriptedRandom extends Random {
        private final int value;

        private ScriptedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value % bound;
        }
    }

    @Test
    void laTiradaVaDeCeroACinco() {
        assertEquals(0, BeastAiPlan.roll(new ScriptedRandom(0)));
        assertEquals(5, BeastAiPlan.roll(new ScriptedRandom(5)));
    }

    @Test
    void unaBestiaNormalDesperdiciaLaMitadDeSusTurnos() {
        // 0, 3 y 5 no estan mapeados para las nueve primeras: la mitad de los turnos son pausa
        int acting = 0;
        for (int roll = BeastAiPlan.ROLL_MIN; roll <= BeastAiPlan.ROLL_MAX; roll++) {
            if (BeastAiPlan.choose(TailedBeast.NINE_TAILS, roll, false, true).isPresent()) {
                acting++;
            }
        }
        assertEquals(3, acting);
    }

    @Test
    void elDiezColasAprovechaUnTurnoMas() {
        // el 3 SI esta mapeado para el Diez Colas (Orbes Buscadores de la Verdad): 4 de 6
        int acting = 0;
        for (int roll = BeastAiPlan.ROLL_MIN; roll <= BeastAiPlan.ROLL_MAX; roll++) {
            if (BeastAiPlan.choose(TailedBeast.TEN_TAILS, roll, false, true).isPresent()) {
                acting++;
            }
        }
        assertEquals(4, acting);
    }

    @Test
    void laTiradaUnoLanzaLaInsignia() {
        assertEquals(Optional.of("chakra_arms_barrage"),
                BeastAiPlan.choose(TailedBeast.NINE_TAILS, 1, false, true));
        assertEquals(Optional.of("sand_buckshot"),
                BeastAiPlan.choose(TailedBeast.ONE_TAIL, 1, false, true));
    }

    @Test
    void laBolaBestiaConColaSoloSaleSiTeTieneEncima() {
        assertEquals(Optional.of(BeastAiPlan.TAILED_BEAST_BALL),
                BeastAiPlan.choose(TailedBeast.FIVE_TAILS, 2, true, true));
        // sin nadie cerca, esa misma tirada cae al slot 3 (su insignia)
        assertEquals(Optional.of("unrivalled_strength"),
                BeastAiPlan.choose(TailedBeast.FIVE_TAILS, 2, false, true));
    }

    @Test
    void elDiezColasUsaSuPropiaBolaBestiaConCola() {
        assertEquals(Optional.of(BeastAiPlan.TAILED_BEAST_BALL_TEN_TAILS),
                BeastAiPlan.choose(TailedBeast.TEN_TAILS, 2, true, true));
    }

    @Test
    void losOrbesBuscadoresDeLaVerdadSonExclusivosDelDiezColas() {
        assertEquals(Optional.of(BeastAiPlan.TRUTH_SEEKING_ORB),
                BeastAiPlan.choose(TailedBeast.TEN_TAILS, 3, false, true));
        for (TailedBeast beast : TailedBeast.values()) {
            if (!beast.isTenTails()) {
                assertTrue(BeastAiPlan.choose(beast, 3, false, true).isEmpty(),
                        beast.id() + " no deberia tener slot 4");
            }
        }
    }

    @Test
    void conElTenpenchiiEnCuentaAtrasElDiezColasRecurreALosBrazosDeChakra() {
        assertEquals(Optional.of("tenpenchii"),
                BeastAiPlan.choose(TailedBeast.TEN_TAILS, 1, false, true));
        assertEquals(Optional.of(BeastAiPlan.CHAKRA_ARMS_BARRAGE),
                BeastAiPlan.choose(TailedBeast.TEN_TAILS, 1, false, false));
    }

    @Test
    void laTiradaCuatroSeparaAlDiezColasDelResto() {
        // para las nueve es otra vez la insignia; para el Diez Colas, el slot 3
        assertEquals(Optional.of("tentacle_barrage"),
                BeastAiPlan.choose(TailedBeast.EIGHT_TAILS, 4, false, true));
        assertEquals(Optional.of(BeastAiPlan.CHAKRA_ARMS_BARRAGE),
                BeastAiPlan.choose(TailedBeast.TEN_TAILS, 4, false, true));
    }

    @Test
    void elCeroYElCincoNuncaHacenNada() {
        for (TailedBeast beast : TailedBeast.values()) {
            assertTrue(BeastAiPlan.choose(beast, 0, true, true).isEmpty());
            assertTrue(BeastAiPlan.choose(beast, 5, true, true).isEmpty());
        }
    }
}
