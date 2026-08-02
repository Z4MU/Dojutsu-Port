package com.zamu.dojutsu.domain.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de las reglas del encuentro con los Sabios (F9/DJT-200). Verificado contra
 * {@code main/internal/hagoromo/{queue,try,retry}} y sus predicados.
 *
 * <p>Los tres valores que se portan del reves con facilidad tienen test propio: la <b>escalada</b> de la
 * probabilidad (que es piedad, no castigo), su <b>tope</b>, y el hecho de que el rango exigido es el <b>maximo</b>.
 */
class StoryTableTest {

    @Test
    void laProbabilidadSubeSiNoTeHaPasado() {
        assertEquals(5, StoryTable.chanceFor(0));
        assertEquals(8, StoryTable.chanceFor(1));
        assertEquals(10, StoryTable.chanceFor(2));
    }

    @Test
    void yDejaDeSubirEnElDiezPorCiento() {
        // retry solo incrementa unless hagoromo_try_2p: el contador se queda en el tope, y con el la probabilidad
        assertEquals(1, StoryTable.nextAttempts(0));
        assertEquals(2, StoryTable.nextAttempts(1));
        assertEquals(2, StoryTable.nextAttempts(2));
        assertEquals(10, StoryTable.chanceFor(StoryTable.nextAttempts(2)));
        assertEquals(10, StoryTable.chanceFor(99));
    }

    @Test
    void unEncuentroExitosoDejaElContadorEnUno() {
        // start pone hagoromo_try 0 y el retry posterior lo sube: la siguiente tirada es al 8%, no al 5%
        assertEquals(1, StoryTable.attemptsAfterEncounter());
        assertEquals(StoryTable.CHANCE_SECOND, StoryTable.chanceFor(StoryTable.attemptsAfterEncounter()));
    }

    @Test
    void haceFaltaSerKage() {
        assertEquals(6, StoryTable.REQUIRED_RANK);
        assertFalse(StoryTable.rankAllows(5));
        assertTrue(StoryTable.rankAllows(6));
        assertTrue(StoryTable.rankAllows(7));
    }

    @Test
    void laTiradaAciertaPorDebajoDeLaProbabilidad() {
        assertTrue(StoryTable.isHit(1, 5));
        assertTrue(StoryTable.isHit(5, 5));
        assertFalse(StoryTable.isHit(6, 5));
        assertFalse(StoryTable.isHit(100, 10));
    }

    @Test
    void laColaEsDeUnaHora() {
        assertEquals(3600, StoryTable.QUEUE_SECONDS);
        assertEquals(72000, StoryTable.QUEUE_TICKS);
    }
}
