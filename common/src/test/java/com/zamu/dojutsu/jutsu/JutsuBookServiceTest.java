package com.zamu.dojutsu.jutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zamu.dojutsu.domain.jutsu.AffinityRequirement;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import com.zamu.dojutsu.network.JutsuBookEntry;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de la vista del libro de jutsus (F11/DJT-235): el aplanado de una entrada del registro a la ficha que
 * viaja al cliente, y la lectura tolerante del dano.
 */
class JutsuBookServiceTest {

    private static JutsuEntry entry(String params) {
        JutsuDefinition definition = new JutsuDefinition("great_fireball", 140, 100,
                Optional.of(new AffinityRequirement("fire", 170)), Optional.empty(), 0, "fire", "projectile");
        return new JutsuEntry(definition, JsonParser.parseString(params));
    }

    // --- Aplanado ---

    @Test
    void laFichaLlevaLosNumerosDelRegistro() {
        JutsuBookEntry ficha = JutsuBookService.toEntry(entry("{\"damage\": 12.0}"));

        assertEquals("great_fireball", ficha.id());
        assertEquals(140, ficha.chakraCost());
        assertEquals(100, ficha.cooldownTicks());
        assertEquals("fire", ficha.category());
        assertEquals(12.0F, ficha.damage());
    }

    @Test
    void laAfinidadViajaAplanada() {
        JutsuBookEntry ficha = JutsuBookService.toEntry(entry("{}"));

        assertEquals("fire", ficha.affinity().orElseThrow().nature());
        assertEquals(170, ficha.affinity().orElseThrow().amount());
    }

    @Test
    void unJutsuUniversalNoTraeAfinidad() {
        JutsuDefinition universal = new JutsuDefinition("clone", 30, 40, Optional.empty(), Optional.empty(),
                0, "ninjutsu", "summon");

        JutsuBookEntry ficha = JutsuBookService.toEntry(new JutsuEntry(universal, new JsonObject()));

        assertTrue(ficha.affinity().isEmpty());
    }

    // --- Dano: lectura tolerante ---

    @Test
    void sinDanoDeclaradoLaFichaTraeCero() {
        // 49 de las 128 tecnicas del datapack no declaran dano (buffs, teleports, estructuras)
        assertEquals(0F, JutsuBookService.damageOf(JsonParser.parseString("{\"speed\": 1.0}")));
    }

    @Test
    void unDanoMalFormadoNoRompeElLibro() {
        // el libro debe dibujarse igual: un parametro raro no puede dejar al jugador sin pantalla
        assertEquals(0F, JutsuBookService.damageOf(JsonParser.parseString("{\"damage\": \"mucho\"}")));
        assertEquals(0F, JutsuBookService.damageOf(JsonParser.parseString("{\"damage\": {}}")));
        assertEquals(0F, JutsuBookService.damageOf(JsonParser.parseString("[]")));
        assertEquals(0F, JutsuBookService.damageOf(null));
    }

    // --- Formato ---

    @Test
    void elEnfriamientoSeMuestraEnSegundos() {
        assertEquals("5.0", JutsuBookService.toEntry(entry("{}")).cooldownSeconds()); // 100 ticks
    }

    @Test
    void elEnfriamientoRedondeaHaciaArriba() {
        // 1 tick no puede mostrarse como "0.0s": diria que esta listo cuando no lo esta
        JutsuBookEntry ficha = new JutsuBookEntry("x", 0, 1, "fire", Optional.empty(), 0F);

        assertEquals("0.1", ficha.cooldownSeconds());
    }
}
