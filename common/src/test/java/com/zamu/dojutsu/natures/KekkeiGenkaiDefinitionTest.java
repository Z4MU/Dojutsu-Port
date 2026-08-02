package com.zamu.dojutsu.natures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests de las definiciones <b>reales</b> de {@code data/dojutsu/kekkei_genkai/*.json} (DJT-052): que parsean,
 * que estan los 18 y que sus valores coinciden con {@code natures/give/kg/<kg>} del datapack (RA-4).
 *
 * <p>Lee los JSON del classpath, asi que valida el <b>contenido enviado</b>, no un duplicado del test: si
 * alguien edita un JSON con un valor equivocado o una referencia rota, esto falla.
 */
class KekkeiGenkaiDefinitionTest {

    private static final Gson GSON = new Gson();

    private static KekkeiGenkaiDefinition load(String id) {
        String path = "/data/dojutsu/kekkei_genkai/" + id + ".json";
        try (InputStream in = KekkeiGenkaiDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "falta el JSON: " + path);
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return KekkeiGenkaiDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar " + path, e);
        }
    }

    @Test
    void los18KekkeiGenkaiTienenDefinicionYParsean() {
        EnumMap<KekkeiGenkai, KekkeiGenkaiDefinition> all = new EnumMap<>(KekkeiGenkai.class);
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            KekkeiGenkaiDefinition def = load(KekkeiGenkaiService.id(kg));
            assertEquals(kg, def.kekkeiGenkai(), "el id del JSON no coincide con su nombre de archivo");
            all.put(kg, def);
        }
        assertEquals(18, all.size());
    }

    @Test
    void losComponentesCoincidenConElDatapack() {
        // natures/give/kg/<kg>: cada 'unless has_<nat> -> give/<nat>'.
        assertEquals(List.of(NatureType.EARTH, NatureType.WATER), load("wood").components());
        assertEquals(List.of(NatureType.WIND, NatureType.WATER), load("ice").components());
        assertEquals(List.of(NatureType.FIRE, NatureType.EARTH), load("lava").components());
        assertEquals(List.of(NatureType.LIGHTNING, NatureType.WATER), load("storm").components());
        assertEquals(List.of(NatureType.FIRE, NatureType.WATER), load("boil").components());
        assertEquals(List.of(NatureType.FIRE, NatureType.WIND, NatureType.EARTH), load("dust").components());
        assertEquals(List.of(NatureType.WIND, NatureType.EARTH), load("magnet").components());
        assertEquals(List.of(NatureType.LIGHTNING, NatureType.EARTH), load("explosion").components());
        assertEquals(List.of(NatureType.FIRE, NatureType.WIND), load("scorch").components());
        assertEquals(List.of(NatureType.FIRE, NatureType.EARTH, NatureType.WATER), load("steel").components());
        assertEquals(List.of(NatureType.WIND, NatureType.YIN), load("dark").components());
        assertEquals(List.of(NatureType.WIND, NatureType.LIGHTNING), load("swift").components());
        assertEquals(List.of(NatureType.EARTH, NatureType.WATER, NatureType.YANG), load("crystal").components());
        assertEquals(List.of(NatureType.WIND, NatureType.WATER), load("typhoon").components());
        assertEquals(List.of(NatureType.EARTH, NatureType.WATER), load("mud").components());
        assertEquals(List.of(NatureType.FIRE), load("blaze").components()); // un unico componente
        assertTrue(load("shikotsumyaku").components().isEmpty()); // sin componentes
        assertTrue(load("jugo_clan").components().isEmpty());
    }

    @Test
    void laXpPorKgNoEsUniforme() {
        // RA-4: [S03] dice "KG = 10" y es FALSO. Valores exactos de cada natures/give/kg/<kg>.
        assertEquals(10, load("wood").ninjaXp());
        assertEquals(8, load("ice").ninjaXp());
        assertEquals(12, load("lava").ninjaXp());
        assertEquals(8, load("storm").ninjaXp());
        assertEquals(8, load("boil").ninjaXp());
        assertEquals(12, load("dust").ninjaXp());
        assertEquals(9, load("magnet").ninjaXp());
        assertEquals(10, load("explosion").ninjaXp());
        assertEquals(8, load("scorch").ninjaXp());
        assertEquals(8, load("steel").ninjaXp());
        assertEquals(9, load("dark").ninjaXp());
        assertEquals(9, load("swift").ninjaXp());
        assertEquals(9, load("crystal").ninjaXp());
        assertEquals(9, load("typhoon").ninjaXp());
        assertEquals(8, load("mud").ninjaXp());
        assertEquals(10, load("blaze").ninjaXp());
        assertEquals(10, load("shikotsumyaku").ninjaXp());
        assertEquals(20, load("jugo_clan").ninjaXp()); // el mas alto
    }

    @Test
    void soloIceYBlazeRegalanAfinidad() {
        assertEquals(Map.of(NatureType.WATER, 10), load("ice").affinityBonus()); // aff_water += 10
        assertEquals(Map.of(NatureType.FIRE, 20), load("blaze").affinityBonus()); // aff_fire += 20
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            if (kg != KekkeiGenkai.ICE && kg != KekkeiGenkai.BLAZE) {
                assertTrue(load(KekkeiGenkaiService.id(kg)).affinityBonus().isEmpty(),
                        "no deberia tener bonus de afinidad: " + kg);
            }
        }
    }

    @Test
    void unaReferenciaRotaFallaAlParsear() {
        // Validacion de refs: un JSON de contenido con un id inexistente debe verse, no colarse en silencio.
        JsonElement json = GSON.fromJson("{\"id\":\"wood\",\"components\":[\"plasma\"],\"ninjaXp\":10}", JsonElement.class);
        assertTrue(KekkeiGenkaiDefinition.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent());

        JsonElement unknownKg = GSON.fromJson("{\"id\":\"hashirama_cells\",\"ninjaXp\":30}", JsonElement.class);
        assertTrue(KekkeiGenkaiDefinition.CODEC.parse(JsonOps.INSTANCE, unknownKg).error().isPresent());
    }
}
