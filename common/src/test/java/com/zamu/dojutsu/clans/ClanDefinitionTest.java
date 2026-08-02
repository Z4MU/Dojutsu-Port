package com.zamu.dojutsu.clans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests de las definiciones <b>reales</b> de {@code data/dojutsu/clans/*.json} (DJT-056): que parsean, que
 * estan los 14 y que sus valores coinciden con {@code clans/join_<clan>} del datapack (RA-4, extraccion
 * funcion a funcion — la tabla-resumen de {@code [S04]} omite varios bonus).
 */
class ClanDefinitionTest {

    private static final Gson GSON = new Gson();

    private static final List<String> CLANS = List.of("hyuga", "uchiha", "otsutsuki", "uzumaki", "chinoike",
            "nara", "kaguya", "hatake", "senju", "aburame", "akimichi", "namikaze", "sarutobi", "lee");

    private static ClanDefinition load(String id) {
        String path = "/data/dojutsu/clans/" + id + ".json";
        try (InputStream in = ClanDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "falta el JSON: " + path);
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return ClanDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar " + path, e);
        }
    }

    @Test
    void los14ClanesTienenDefinicionYParsean() {
        for (String id : CLANS) {
            assertEquals(id, load(id).id(), "el id del JSON no coincide con su nombre de archivo");
        }
        assertEquals(14, CLANS.size());
    }

    @Test
    void elMaxChakraEsElDelDatapack() {
        // clans/join_<clan>: 'scoreboard players set @s d.max_chakra N'.
        Map<String, Integer> expected = Map.ofEntries(
                Map.entry("hyuga", 120), Map.entry("uchiha", 150), Map.entry("otsutsuki", 250),
                Map.entry("uzumaki", 500), Map.entry("chinoike", 140), Map.entry("nara", 100),
                Map.entry("kaguya", 100), Map.entry("hatake", 120), Map.entry("senju", 150),
                Map.entry("aburame", 100), Map.entry("akimichi", 110), Map.entry("namikaze", 140),
                Map.entry("sarutobi", 180), Map.entry("lee", 50));
        expected.forEach((id, chakra) -> assertEquals(chakra, load(id).maxChakra(), id));
    }

    @Test
    void laNinjaXpExplicitaEsLaDelDatapack() {
        // Solo la del 'add d.ninja_xp N' del join (las naturalezas otorgadas suman la suya aparte).
        Map<String, Integer> expected = Map.ofEntries(
                Map.entry("hyuga", 20), Map.entry("uchiha", 30), Map.entry("otsutsuki", 0),
                Map.entry("uzumaki", 15), Map.entry("chinoike", 60), Map.entry("nara", 10),
                Map.entry("kaguya", 20), Map.entry("hatake", 85), Map.entry("senju", 40),
                Map.entry("aburame", 0), Map.entry("akimichi", 0), Map.entry("namikaze", 65),
                Map.entry("sarutobi", 30), Map.entry("lee", 90));
        expected.forEach((id, xp) -> assertEquals(xp, load(id).ninjaXp(), id));
    }

    @Test
    void losBonusOmitidosPorLaTablaDeS04EstanPresentes() {
        // La tabla-resumen de [S04] omite estos; las funciones join_* los tienen (regla 16).
        assertEquals(10, load("hatake").skillBonus().get(Skill.KENJUTSU));
        assertEquals(10, load("kaguya").skillBonus().get(Skill.KENJUTSU));
        assertEquals(5, load("sarutobi").skillBonus().get(Skill.KENJUTSU));
        assertEquals(10, load("namikaze").skillBonus().get(Skill.FUINJUTSU));
    }

    @Test
    void lasNaturalezasYKgInicialesSonLosDelDatapack() {
        assertEquals(List.of(NatureType.FIRE), load("uchiha").natures());
        assertEquals(List.of(NatureType.LIGHTNING), load("hatake").natures());
        assertEquals(List.of(NatureType.YIN), load("nara").natures());
        assertEquals(List.of(NatureType.YIN), load("chinoike").natures());
        assertEquals(List.of(NatureType.YANG), load("senju").natures());
        assertEquals(List.of(NatureType.YANG), load("akimichi").natures());
        assertEquals(List.of(NatureType.FIRE), load("sarutobi").natures());
        assertEquals(List.of(KekkeiGenkai.SHIKOTSUMYAKU), load("kaguya").kekkeiGenkai());
        assertEquals(List.of(KekkeiGenkai.SWIFT), load("namikaze").kekkeiGenkai());
        // Lee: el caso "sin afinidad" del onboarding (join_finish lo salta entero).
        assertTrue(load("lee").natures().isEmpty());
        assertTrue(load("lee").affinityBonus().isEmpty());
    }

    @Test
    void losDojutsusPotencialesSonDatos() {
        // El despertar (F5) leera esto, no predicates is_* hardcodeados.
        assertEquals(List.of("byakugan"), load("hyuga").potentialDojutsu());
        assertEquals(List.of("sharingan"), load("uchiha").potentialDojutsu());
        assertEquals(List.of("byakugan", "sharingan"), load("otsutsuki").potentialDojutsu()); // 50/50 (F5)
        assertEquals(List.of("ketsuryugan"), load("chinoike").potentialDojutsu());
        assertTrue(load("lee").potentialDojutsu().isEmpty());
    }

    @Test
    void soloLosCuatroClanesConJutsusOcultosDeclaranDesbloqueoProgresivo() {
        // DJT-061: predicates new_<clan>_jutsu, todos con umbral 78000 (65 min de juego).
        for (String id : List.of("nara", "kaguya", "akimichi", "aburame")) {
            assertEquals(78_000, load(id).jutsuUnlockTicks().orElseThrow(), id);
        }
        for (String id : List.of("hyuga", "uchiha", "otsutsuki", "uzumaki", "chinoike", "hatake", "senju",
                "namikaze", "sarutobi", "lee")) {
            assertTrue(load(id).jutsuUnlockTicks().isEmpty(), id);
        }
    }

    @Test
    void sarutobiLlevaDosChakraBlades() {
        // join_sarutobi llama give/weapons/chakra_blade DOS veces: la lista admite repetidos.
        assertEquals(List.of("chakra_blade", "chakra_blade"), load("sarutobi").startingItems());
    }

    @Test
    void unaReferenciaRotaFallaAlParsear() {
        JsonElement badNature = GSON.fromJson(
                "{\"id\":\"x\",\"natures\":[\"plasma\"],\"maxChakra\":100}", JsonElement.class);
        assertTrue(ClanDefinition.CODEC.parse(JsonOps.INSTANCE, badNature).error().isPresent());

        JsonElement badSkill = GSON.fromJson(
                "{\"id\":\"x\",\"skillBonus\":{\"cocina\":5},\"maxChakra\":100}", JsonElement.class);
        assertTrue(ClanDefinition.CODEC.parse(JsonOps.INSTANCE, badSkill).error().isPresent());

        JsonElement noChakra = GSON.fromJson("{\"id\":\"x\"}", JsonElement.class);
        assertTrue(ClanDefinition.CODEC.parse(JsonOps.INSTANCE, noChakra).error().isPresent());
    }
}
