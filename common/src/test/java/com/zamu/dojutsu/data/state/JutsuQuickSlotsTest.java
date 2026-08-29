package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests de los ocho huecos rapidos de la rueda (F11/DJT-233).
 *
 * <p>Lo que se protege: la rueda nunca ofrece algo que no se pueda usar, siempre tiene la misma forma
 * (ocho posiciones fijas) y no nace vacia.
 */
class JutsuQuickSlotsTest {

    private static JutsuKnowledge conocidos(String... ids) {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT;
        for (String id : ids) {
            k = k.withKnown(id);
        }
        return k;
    }

    // --- Forma fija ---

    @Test
    void unNinjaNuevoTieneOchoHuecosLibres() {
        assertEquals(JutsuKnowledge.SLOT_COUNT, JutsuKnowledge.DEFAULT.quickSlots().size());
        assertTrue(JutsuKnowledge.DEFAULT.slot(0).isEmpty());
    }

    @Test
    void laListaSiempreQuedaEnOchoEntradas() {
        // NBT de otra version, o manipulado: se recorta o se rellena, nunca se rechaza
        JutsuKnowledge corta = new JutsuKnowledge(Set.of(), Map.of(), Map.of(), Optional.empty(), List.of("a"));
        JutsuKnowledge larga = new JutsuKnowledge(Set.of(), Map.of(), Map.of(), Optional.empty(),
                List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"));

        assertEquals(JutsuKnowledge.SLOT_COUNT, corta.quickSlots().size());
        assertEquals(JutsuKnowledge.SLOT_COUNT, larga.quickSlots().size());
    }

    @Test
    void unIndiceFueraDeRangoNoRompeNada() {
        JutsuKnowledge k = conocidos("chidori");

        assertTrue(k.slot(-1).isEmpty());
        assertTrue(k.slot(JutsuKnowledge.SLOT_COUNT).isEmpty());
        assertEquals(k, k.withSlot(99, "chidori"));
        assertEquals(k, k.withoutSlot(-3));
    }

    // --- La rueda no ofrece lo que no se puede usar ---

    @Test
    void unHuecoConUnJutsuNoAprendidoSeVacia() {
        // el caso del save: aprendiste algo, lo pusiste en la rueda, y un reset te lo quito
        JutsuKnowledge k = new JutsuKnowledge(Set.of("rasengan"), Map.of(), Map.of(), Optional.empty(),
                List.of("chidori", "rasengan", "", "", "", "", "", ""));

        assertTrue(k.slot(0).isEmpty(), "chidori ya no se conoce: el hueco debe quedar libre");
        assertEquals(Optional.of("rasengan"), k.slot(1));
    }

    @Test
    void colocarLoQueNoSeConoceDejaElHuecoLibre() {
        JutsuKnowledge k = conocidos("chidori").withSlot(3, "amaterasu");

        assertTrue(k.slot(3).isEmpty());
    }

    // --- Reordenar ---

    @Test
    void colocarUnJutsuLoQuitaDelHuecoAnterior() {
        // el mismo jutsu dos veces en la rueda es un hueco desperdiciado, y en partida se lee como un fallo
        JutsuKnowledge k = conocidos("chidori"); // el autorelleno lo pone en el hueco 0
        assertEquals(Optional.of("chidori"), k.slot(0));

        JutsuKnowledge movido = k.withSlot(5, "chidori");

        assertTrue(movido.slot(0).isEmpty());
        assertEquals(Optional.of("chidori"), movido.slot(5));
    }

    @Test
    void vaciarUnHuecoNoTocaLosDemas() {
        JutsuKnowledge k = conocidos("chidori", "rasengan").withoutSlot(0);

        assertTrue(k.slot(0).isEmpty());
        assertEquals(Optional.of("rasengan"), k.slot(1));
    }

    // --- Autorelleno: la rueda no nace vacia ---

    @Test
    void aprenderUnJutsuLoPoneEnElPrimerHuecoLibre() {
        JutsuKnowledge k = conocidos("chidori", "rasengan");

        assertEquals(Optional.of("chidori"), k.slot(0));
        assertEquals(Optional.of("rasengan"), k.slot(1));
    }

    @Test
    void elAutorellenoNoDuplicaNiPisaLoYaColocado() {
        JutsuKnowledge k = conocidos("chidori").withSlot(4, "chidori");

        JutsuKnowledge otraVez = k.withKnown("chidori"); // aprenderlo de nuevo es idempotente

        assertEquals(Optional.of("chidori"), otraVez.slot(4));
        assertTrue(otraVez.slot(0).isEmpty(), "no debe aparecer una segunda copia");
    }

    @Test
    void conLaRuedaLlenaElNovenoJutsuSeAprendeIgualPeroNoEntra() {
        // aprender nunca puede fallar por falta de sitio en la rueda: son cosas distintas
        JutsuKnowledge k = conocidos("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9");

        assertTrue(k.knows("a9"));
        assertFalse(k.quickSlots().contains("a9"));
    }

    // --- Persistencia ---

    @Test
    void laRuedaSobreviveAlSave() {
        JutsuKnowledge original = conocidos("chidori", "rasengan").withSlot(6, "rasengan");

        var json = JutsuKnowledge.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        JutsuKnowledge recargado = JutsuKnowledge.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertEquals(original.quickSlots(), recargado.quickSlots());
        assertEquals(Optional.of("rasengan"), recargado.slot(6));
    }

    @Test
    void unSaveViejoSinRuedaCargaConLaRuedaVacia() {
        // back-compat: partidas anteriores a DJT-233 no tienen el campo
        var json = com.google.gson.JsonParser.parseString(
                "{\"known\":[\"chidori\"],\"learnProgress\":{},\"cooldowns\":{}}");

        JutsuKnowledge k = JutsuKnowledge.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();

        assertTrue(k.knows("chidori"));
        assertEquals(JutsuKnowledge.SLOT_COUNT, k.quickSlots().size());
        assertTrue(k.slot(0).isEmpty(), "no se inventa un reparto al cargar; se llena al aprender");
    }
}
