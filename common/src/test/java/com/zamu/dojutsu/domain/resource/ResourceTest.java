package com.zamu.dojutsu.domain.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests del recurso con clamp {@code [0, max]} (DJT-010). */
class ResourceTest {

    @Test
    void constructorClampaElValorInicialAlRango() {
        assertEquals(100, new Resource(999, 100).current(), "por encima del max se clampa");
        assertEquals(0, new Resource(-5, 100).current(), "por debajo de 0 se clampa");
        assertEquals(42, new Resource(42, 100).current(), "dentro de rango se conserva");
    }

    @Test
    void fabricasFullYEmpty() {
        assertEquals(100, Resource.full(100).current());
        assertEquals(0, Resource.empty(100).current());
    }

    @Test
    void setCurrentClampa() {
        Resource r = Resource.empty(100);
        r.setCurrent(150);
        assertEquals(100, r.current());
        r.setCurrent(-1);
        assertEquals(0, r.current());
    }

    @Test
    void hasComparaConElCoste() {
        Resource r = new Resource(50, 100);
        assertTrue(r.has(50));
        assertTrue(r.has(0));
        assertFalse(r.has(51));
    }

    @Test
    void consumeExitosoRestaElCoste() {
        Resource r = new Resource(50, 100);
        assertTrue(r.consume(30));
        assertEquals(20, r.current());
    }

    @Test
    void consumeMayorQueCurrentNoCambiaNada() {
        Resource r = new Resource(20, 100);
        assertFalse(r.consume(21), "todo o nada: sin recurso suficiente no consume");
        assertEquals(20, r.current(), "no debe bajar de 0 ni quedar parcial");
    }

    @Test
    void addConOvershootSeClampaAlMax() {
        // S02-R4: el cap se aplica siempre tras cada regen.
        Resource r = new Resource(98, 100);
        r.add(8);
        assertEquals(100, r.current());
    }

    @Test
    void restoreDejaAlMaximo() {
        Resource r = Resource.empty(100);
        r.restore();
        assertEquals(100, r.current());
    }

    @Test
    void maxCeroMantieneCurrentEnCero() {
        Resource r = new Resource(0, 0);
        r.add(10);
        assertEquals(0, r.current());
        r.restore();
        assertEquals(0, r.current());
    }

    @Test
    void setMaxReclampaElCurrentHaciaAbajo() {
        Resource r = Resource.full(100);
        r.setMax(40);
        assertEquals(40, r.max());
        assertEquals(40, r.current(), "el current no puede superar el nuevo max");
    }

    @Test
    void valoresNegativosSonRechazados() {
        assertThrows(IllegalArgumentException.class, () -> new Resource(0, -1));
        Resource r = new Resource(10, 100);
        assertThrows(IllegalArgumentException.class, () -> r.setMax(-1));
        assertThrows(IllegalArgumentException.class, () -> r.has(-1));
        assertThrows(IllegalArgumentException.class, () -> r.consume(-1));
        assertThrows(IllegalArgumentException.class, () -> r.add(-1));
    }
}
