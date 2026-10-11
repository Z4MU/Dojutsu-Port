package com.zamu.dojutsu.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Encaje entre el modelo 3D de un jutsu-proyectil y su entidad (F11/DJT-241).
 *
 * <p>En la primera prueba en partida de la Bala de fuego la bola visible iba ~1 bloque por delante de la entidad y
 * medía 0.8 bloques contra una hitbox de 0.25. Nada de eso da error: se ve mal y ya. Este test lo convierte en un
 * fallo de build para <b>todos</b> los jutsus con {@code displayModel3d}, no solo para el piloto.
 *
 * <p>Convenio (ver {@code tools/modelos/README.md}): los cubos de la bola se llaman {@code bola_*}.
 */
class Jutsu3dFitTest {

    private static final Gson GSON = new Gson();

    /** Tolerancia del diametro frente a {@code hitboxSize}, en bloques. */
    private static final double SIZE_TOLERANCE = 0.05D;

    /** Tolerancia del pivote, en pixeles de modelo ya escalados. */
    private static final double PIVOT_TOLERANCE = 0.5D;

    private record Projectile3d(String id, JsonObject params) {
    }

    @Test
    void hayAlMenosUnJutsuConModelo3d() {
        // si el escaneo dejara de encontrar ficheros, los demas tests pasarian sin comprobar nada
        assertFalse(projectiles3d().isEmpty());
    }

    @Test
    void laHitboxMideLoQueLaBolaDelModelo() {
        for (Projectile3d jutsu : projectiles3d()) {
            JsonObject model = worldModel(jutsu.id());
            double diameter = ballSpan(model, 0) * groundScale(model) / 16.0D;
            assertTrue(jutsu.params().has("hitboxSize"),
                    jutsu.id() + ": un modelo 3D necesita hitboxSize (la bola mide " + diameter + " bloques)");
            assertEquals(diameter, jutsu.params().get("hitboxSize").getAsDouble(), SIZE_TOLERANCE,
                    jutsu.id() + ": hitboxSize debe coincidir con el diametro de la bola del modelo");
        }
    }

    @Test
    void elCentroDeLaBolaCaeEnLaPosicionDeLaEntidad() {
        for (Projectile3d jutsu : projectiles3d()) {
            JsonObject model = worldModel(jutsu.id());
            JsonArray ball = ballElements(model);
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (JsonElement el : ball) {
                minZ = Math.min(minZ, el.getAsJsonObject().getAsJsonArray("from").get(2).getAsDouble());
                maxZ = Math.max(maxZ, el.getAsJsonObject().getAsJsonArray("to").get(2).getAsDouble());
            }
            // Minecraft centra el modelo en 8; la traslacion de ground (que no se escala) debe compensar lo que
            // la bola se aleja de ese centro una vez escalada.
            double offset = ((minZ + maxZ) / 2.0D - 8.0D) * groundScale(model) + groundTranslationZ(model);
            assertEquals(0.0D, offset, PIVOT_TOLERANCE,
                    jutsu.id() + ": la bola no esta centrada en la entidad; ajusta display.ground.translation");
        }
    }

    @Test
    void unJutsuConModelo3dLlevaEstela() {
        for (Projectile3d jutsu : projectiles3d()) {
            assertTrue(jutsu.params().has("trail"), jutsu.id() + ": un modelo 3D sin estela parece congelado");
        }
    }

    // ------------------------------------------------------------------

    private static List<Projectile3d> projectiles3d() {
        List<Projectile3d> found = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of(Jutsu3dFitTest.class.getResource("/data/dojutsu/jutsu").toURI()))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(in, JsonObject.class);
                    JsonObject params = json.has("effectParams") && json.get("effectParams").isJsonObject()
                            ? json.getAsJsonObject("effectParams") : new JsonObject();
                    if (params.has("displayModel3d") && params.get("displayModel3d").getAsBoolean()) {
                        found.add(new Projectile3d(json.get("id").getAsString(), params));
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("no se pudieron recorrer los jutsus", e);
        }
        return found;
    }

    /** El modelo al que apunta la override 3D del jutsu en {@code models/item/jutsu.json}. */
    private static JsonObject worldModel(String jutsuId) {
        int cmd = JutsuModelData.worldModelOf(jutsuId);
        String modelId = null;
        for (JsonElement el : read("/assets/dojutsu/models/item/jutsu.json").getAsJsonArray("overrides")) {
            JsonObject override = el.getAsJsonObject();
            if (override.getAsJsonObject("predicate").get("custom_model_data").getAsInt() == cmd) {
                modelId = override.get("model").getAsString();
            }
        }
        assertNotNull(modelId, jutsuId + ": sin override 3D " + cmd);
        return read("/assets/dojutsu/models/" + modelId.substring("dojutsu:".length()) + ".json");
    }

    private static JsonArray ballElements(JsonObject model) {
        JsonArray ball = new JsonArray();
        for (JsonElement el : model.getAsJsonArray("elements")) {
            JsonObject element = el.getAsJsonObject();
            if (element.has("name") && element.get("name").getAsString().startsWith("bola_")) {
                ball.add(element);
            }
        }
        assertFalse(ball.isEmpty(), "el modelo no tiene cubos bola_*");
        return ball;
    }

    /** Lo que ocupa la bola en un eje (0 = x), en pixeles de modelo. */
    private static double ballSpan(JsonObject model, int axis) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (JsonElement el : ballElements(model)) {
            min = Math.min(min, el.getAsJsonObject().getAsJsonArray("from").get(axis).getAsDouble());
            max = Math.max(max, el.getAsJsonObject().getAsJsonArray("to").get(axis).getAsDouble());
        }
        return max - min;
    }

    private static double groundScale(JsonObject model) {
        return model.getAsJsonObject("display").getAsJsonObject("ground").getAsJsonArray("scale").get(0).getAsDouble();
    }

    private static double groundTranslationZ(JsonObject model) {
        return model.getAsJsonObject("display").getAsJsonObject("ground").getAsJsonArray("translation").get(2)
                .getAsDouble();
    }

    private static JsonObject read(String path) {
        try (InputStream in = Jutsu3dFitTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "no existe " + path);
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo leer " + path, e);
        }
    }
}
