package com.zamu.dojutsu.domain.trading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Tests del comercio ninja (F8/DJT-190). Verificado contra {@code trades/setup_shinobi} y
 * {@code trader/{init,try}}. Los dos valores que se portan del reves con facilidad —la direccion de la
 * probabilidad y el "veces por dia"— van con test propio.
 */
class TradingTableTest {

    /** Generador con una tirada fijada. */
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
    void soloUnoDeCadaDiezAldeanosEsShinobi() {
        assertEquals(20, TradingTable.SHINOBI_ROLL_OUTCOMES);
        assertEquals(2, TradingTable.SHINOBI_WINNING_ROLLS);
        assertEquals(10, TradingTable.SHINOBI_CHANCE_PERCENT);
        // 0 y 1 convierten; 2 en adelante no
        assertTrue(TradingTable.rollShinobi(new ScriptedRandom(0)));
        assertTrue(TradingTable.rollShinobi(new ScriptedRandom(1)));
        assertFalse(TradingTable.rollShinobi(new ScriptedRandom(2)));
        assertFalse(TradingTable.rollShinobi(new ScriptedRandom(19)));
    }

    @Test
    void elRitmoEsVecesPorDiaNoIntervalo() {
        // la tabla de 24 lineas del datapack es esta division escrita a mano
        assertEquals(24_000, TradingTable.attemptIntervalTicks(1));
        assertEquals(12_000, TradingTable.attemptIntervalTicks(2));
        assertEquals(8_000, TradingTable.attemptIntervalTicks(3));
        assertEquals(3_428, TradingTable.attemptIntervalTicks(7)); // el datapack redondea a 3429
        assertEquals(1_000, TradingTable.attemptIntervalTicks(24));
    }

    @Test
    void unRitmoDeCeroDesactivaLaPolitica() {
        assertEquals(0, TradingTable.attemptIntervalTicks(0));
        assertEquals(0, TradingTable.attemptIntervalTicks(-5));
    }

    @Test
    void elRitmoSeCapaEnVeinticuatro() {
        assertEquals(TradingTable.attemptIntervalTicks(TradingTable.MAX_SPAWN_RATE),
                TradingTable.attemptIntervalTicks(999));
    }

    @Test
    void laProbabilidadVaAlRevesDeLoQueParece() {
        // el rango es [chance, 10] y gana el 10: chance 10 es CERTEZA, chance 1 es una de cada diez
        assertEquals(1.0, TradingTable.spawnProbability(10));
        assertEquals(0.5, TradingTable.spawnProbability(9));
        assertEquals(0.1, TradingTable.spawnProbability(1));
    }

    @Test
    void conCertezaSiempreAparece() {
        for (int roll = 0; roll < 10; roll++) {
            assertTrue(TradingTable.rollSpawn(10, new ScriptedRandom(roll)),
                    "con chance 10 deberia aparecer siempre");
        }
    }

    @Test
    void conLaProbabilidadMinimaSoloAciertaElTecho() {
        // rango 1..10: solo el 10 gana, o sea el desplazamiento 9
        assertTrue(TradingTable.rollSpawn(1, new ScriptedRandom(9)));
        for (int roll = 0; roll < 9; roll++) {
            assertFalse(TradingTable.rollSpawn(1, new ScriptedRandom(roll)));
        }
    }

    @Test
    void unaProbabilidadFueraDeRangoSeClampa() {
        assertEquals(TradingTable.spawnProbability(1), TradingTable.spawnProbability(-3));
        assertEquals(TradingTable.spawnProbability(10), TradingTable.spawnProbability(99));
    }

    @Test
    void unaOfertaConValoresAbsurdosSeSanea() {
        // un pack ajeno con precio 0 romperia el trade vanilla
        TraderOffer offer = new TraderOffer("dojutsu:kunai", 0, 0, -4, -1);
        assertEquals(1, offer.count());
        assertEquals(1, offer.price());
        assertEquals(1, offer.maxUses());
        assertEquals(0, offer.xp());
    }

    @Test
    void losDosComerciantesSeResuelvenPorId() {
        assertEquals(java.util.Optional.of(TraderCatalogue.Kind.SHINOBI_VILLAGER),
                TraderCatalogue.Kind.byId("shinobi_villager"));
        assertEquals(java.util.Optional.of(TraderCatalogue.Kind.WANDERING_TRADER),
                TraderCatalogue.Kind.byId("wandering_trader"));
        assertTrue(TraderCatalogue.Kind.byId("blacksmith").isEmpty()); // estado tolerante
    }
}
