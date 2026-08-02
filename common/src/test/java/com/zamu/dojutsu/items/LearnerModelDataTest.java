package com.zamu.dojutsu.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests del mapa de modelos del learner (F4/DJT-085c): consistencia con el modelo base y orden ascendente de
 * los {@code custom_model_data} (la resolucion vanilla toma el ultimo cuyo valor {@code <=} el del item).
 */
class LearnerModelDataTest {

    private static final Gson GSON = new Gson();

    @Test
    void reutilizaLosCustomModelDataAutenticos() {
        assertEquals(1000, LearnerModelData.of("fire"));
        assertEquals(1002, LearnerModelData.of("lightning"));
        assertEquals(1023, LearnerModelData.of("mud"));
        assertEquals(0, LearnerModelData.of("elemento_inexistente"));
    }

    @Test
    void elModeloBaseVaAscendenteYSincronizadoConElMapa() {
        List<Integer> ordered = new ArrayList<>();
        try (InputStream in = LearnerModelDataTest.class.getResourceAsStream(
                "/assets/dojutsu/models/item/learner.json")) {
            JsonObject model = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            model.getAsJsonArray("overrides").forEach(el -> ordered.add(
                    el.getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt()));
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo leer el modelo base del learner", e);
        }
        List<Integer> sorted = new ArrayList<>(ordered);
        Collections.sort(sorted);
        assertEquals(sorted, ordered, "los overrides deben ir ascendentes");
        assertEquals(24, ordered.size(), "24 elementos con learner");
        // Cada CMD del modelo debe salir del mapa (fire=1000..mud=1023, contiguos).
        for (int cmd : ordered) {
            assertEquals(true, cmd >= 1000 && cmd <= 1023);
        }
    }
}
