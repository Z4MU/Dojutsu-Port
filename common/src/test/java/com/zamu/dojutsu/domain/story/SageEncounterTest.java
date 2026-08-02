package com.zamu.dojutsu.domain.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests de los dos Sabios (F9/DJT-200). Lo que hay que blindar es justo lo que la ficha borraba al llamarlos
 * "el Sabio de los Seis Caminos" en singular: que son <b>dos</b>, con clanes <b>disjuntos</b> y ojos
 * <b>distintos</b>.
 */
class SageEncounterTest {

    @Test
    void cadaLinajeTieneUnSoloSabio() {
        assertEquals(SageEncounter.HAGOROMO, SageEncounter.forClan("uzumaki").orElseThrow());
        assertEquals(SageEncounter.HAGOROMO, SageEncounter.forClan("uchiha").orElseThrow());
        assertEquals(SageEncounter.HAGOROMO, SageEncounter.forClan("senju").orElseThrow());
        assertEquals(SageEncounter.HAMURA, SageEncounter.forClan("hyuga").orElseThrow());
    }

    @Test
    void losDemasClanesNoVenANadie() {
        assertTrue(SageEncounter.forClan("nara").isEmpty());
        assertTrue(SageEncounter.forClan("otsutsuki").isEmpty());
        assertTrue(SageEncounter.forClan(null).isEmpty());
    }

    @Test
    void losConjuntosDeClanesSonDisjuntos() {
        Set<String> seen = new HashSet<>();
        for (SageEncounter sage : SageEncounter.values()) {
            for (String clan : sage.clans()) {
                assertTrue(seen.add(clan), "clan repetido entre sabios: " + clan);
            }
        }
        assertEquals(4, seen.size());
    }

    @Test
    void elCaminoYinDespiertaOjosDistintos() {
        assertEquals("rinnegan", SageEncounter.HAGOROMO.yinEye());
        assertEquals("tenseigan", SageEncounter.HAMURA.yinEye());
        assertNotEquals(SageEncounter.HAGOROMO.yinEye(), SageEncounter.HAMURA.yinEye());
    }

    @Test
    void seResuelvenPorIdYLosDesconocidosNoRompen() {
        assertEquals(SageEncounter.HAMURA, SageEncounter.byId("hamura").orElseThrow());
        assertTrue(SageEncounter.byId("kaguya").isEmpty());
    }
}
