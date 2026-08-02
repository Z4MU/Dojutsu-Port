package com.zamu.dojutsu.jutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.domain.jutsu.AffinityRequirement;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import com.zamu.dojutsu.domain.jutsu.KekkeiGenkaiRequirement;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de la resolucion de la afinidad a la que un jutsu da XP (F4/DJT-078). Carga entradas en
 * el {@link JutsuRegistry} y comprueba la decision pura {@code affinityNatureOf}.
 */
class JutsuXpHandlerTest {

    private static JutsuEntry entry(String id, String natureId) {
        Optional<AffinityRequirement> aff = natureId == null ? Optional.empty()
                : Optional.of(new AffinityRequirement(natureId, 30));
        String effect = natureId == null ? "teleport" : "projectile";
        String category = natureId == null ? "universal" : natureId;
        return new JutsuEntry(new JutsuDefinition(id, 60, 100, aff, Optional.empty(), 0, category, effect),
                new JsonObject());
    }

    @Test
    void unJutsuConAfinidadResuelveSuNaturaleza() {
        JutsuRegistry.load(Map.of(
                "fire_bullet", entry("fire_bullet", "fire"),
                "lightning_bolt", entry("lightning_bolt", "lightning")));
        assertEquals(Optional.of(NatureType.FIRE), JutsuXpHandler.affinityNatureOf("fire_bullet"));
        assertEquals(Optional.of(NatureType.LIGHTNING), JutsuXpHandler.affinityNatureOf("lightning_bolt"));
    }

    @Test
    void unJutsuUniversalNoDaXpDeAfinidad() {
        JutsuRegistry.load(Map.of("substitution", entry("substitution", null)));
        assertTrue(JutsuXpHandler.affinityNatureOf("substitution").isEmpty());
    }

    @Test
    void unJutsuDesconocidoNoResuelveNada() {
        JutsuRegistry.load(Map.of());
        assertTrue(JutsuXpHandler.affinityNatureOf("noexiste").isEmpty());
    }

    @Test
    void laCantidadEsUnoUniformeComoElDatapack() {
        assertEquals(1, JutsuXpHandler.AFFINITY_XP_PER_CAST);
    }

    /** Un jutsu KG (Madera) sube la XP de sus DOS naturalezas componentes (earth y water) — DJT-092. */
    @Test
    void unJutsuKgDaXpASusDosNaturalezasComponentes() {
        JutsuDefinition wood = new JutsuDefinition("great_forest", 120, 200,
                List.of(new AffinityRequirement("earth", 130), new AffinityRequirement("water", 130)),
                Optional.of(new KekkeiGenkaiRequirement("wood")),
                Optional.empty(), 0, "wood", "structure");
        JutsuRegistry.load(Map.of("great_forest", new JutsuEntry(wood, new JsonObject())));
        assertEquals(List.of(NatureType.EARTH, NatureType.WATER),
                JutsuXpHandler.affinityNaturesOf("great_forest"));
        // El atajo de una sola naturaleza devuelve la primera.
        assertEquals(Optional.of(NatureType.EARTH), JutsuXpHandler.affinityNatureOf("great_forest"));
    }
}
