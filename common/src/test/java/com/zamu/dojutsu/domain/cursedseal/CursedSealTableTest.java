package com.zamu.dojutsu.domain.cursedseal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests de la tabla del sello maldito (F6/DJT-140): la <b>apuesta</b> de la inyeccion, el coste por nivel y la
 * degradacion. Azar inyectado, asi que la probabilidad se comprueba <b>exacta</b> en los bordes en vez de
 * estadisticamente.
 */
class CursedSealTableTest {

    /** Generador que devuelve valores fijados, para clavar los bordes de la tirada. */
    private static final class ScriptedRandom extends Random {
        private final int[] values;
        private int index;

        private ScriptedRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            return values[index++] % bound;
        }
    }

    @Test
    void laInyeccionTieneExitoHastaElSesentaPorCiento() {
        // nextInt(100) devuelve 0..99 y el datapack tira 1..100: el valor 59 es el ultimo exito (60)
        assertTrue(CursedSealTable.rollInjection(new ScriptedRandom(0)));   // tirada 1
        assertTrue(CursedSealTable.rollInjection(new ScriptedRandom(59)));  // tirada 60: ultimo exito
        assertFalse(CursedSealTable.rollInjection(new ScriptedRandom(60))); // tirada 61: primer fallo
        assertFalse(CursedSealTable.rollInjection(new ScriptedRandom(99))); // tirada 100
    }

    @Test
    void elRepartoAlcanzaLosSeisSellos() {
        Set<CursedSealType> seen = EnumSet.noneOf(CursedSealType.class);
        for (int i = 0; i < CursedSealType.values().length; i++) {
            seen.add(CursedSealTable.rollSealType(new ScriptedRandom(i)));
        }
        assertEquals(EnumSet.allOf(CursedSealType.class), seen);
    }

    @Test
    void inyectarCombinaLasDosTiradas() {
        // exito (tirada 1) y luego el sello 3 del reparto
        Optional<CursedSealType> ok = CursedSealTable.inject(new ScriptedRandom(0, 2));
        assertEquals(Optional.of(CursedSealType.JIROBO), ok);

        assertTrue(CursedSealTable.inject(new ScriptedRandom(99)).isEmpty()); // fallo: no consume la 2a tirada
    }

    @Test
    void madurarAbarataElCoste() {
        assertEquals(CursedSealTable.CHAKRA_COST_LEVEL_1, CursedSealTable.chakraCost(1));
        assertEquals(CursedSealTable.CHAKRA_COST_LEVEL_2, CursedSealTable.chakraCost(2));
        assertTrue(CursedSealTable.chakraCost(2) < CursedSealTable.chakraCost(1));
    }

    @Test
    void soloDegradaEnNivelDosYALasDiezMuertes() {
        assertFalse(CursedSealTable.degradesOnDeath(1, 50)); // en nivel 1 no hay nada que degradar
        assertFalse(CursedSealTable.degradesOnDeath(2, CursedSealTable.DEATHS_TO_DEGRADE - 1));
        assertTrue(CursedSealTable.degradesOnDeath(2, CursedSealTable.DEATHS_TO_DEGRADE));
    }

    @Test
    void cadaSelloTieneSuPerfilYMejoraAlMadurar() {
        // Jirobo es el mas fuerte en nivel 2; Kidomaru no da fuerza pero si la mayor agilidad
        assertEquals(3, CursedSealType.JIROBO.strength(2));
        assertEquals(0, CursedSealType.KIDOMARU.strength(2));
        assertEquals(8, CursedSealType.KIDOMARU.jumpBoost(2));
        // los que no dan salto lo marcan como NONE en ambos niveles
        assertEquals(CursedSealType.NONE, CursedSealType.TAYUYA.jumpBoost(1));
        assertEquals(CursedSealType.NONE, CursedSealType.TAYUYA.jumpBoost(2));
    }

    @Test
    void losSellosSeResuelvenPorIdYPorNumeroDelDatapack() {
        assertEquals(Optional.of(CursedSealType.SUKON_UKON), CursedSealType.byId("sukon_ukon"));
        assertEquals(Optional.of(CursedSealType.HEAVEN), CursedSealType.byDatapackId(1));
        assertTrue(CursedSealType.byId("sharingan").isEmpty()); // estado tolerante: id desconocido no revienta
    }
}
