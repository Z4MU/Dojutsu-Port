package com.zamu.dojutsu.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Comprueba que <b>toda técnica que un hito promete entregar existe de verdad</b> (F10).
 *
 * <h2>Qué falla si esto falla</h2>
 * Durante nueve fases hubo <b>26 de 128 jutsus sin forma de conseguirse</b>: existían como datos, tenían su
 * umbral y su efecto, y nadie los entregaba. La entrega ya está; lo que este test protege es el eslabón
 * siguiente — que el id escrito en {@code unlocks/progression.json} case con un archivo real de
 * {@code jutsu/**}. Una errata ahí no rompe el arranque (la entrega es tolerante y se salta lo que no existe):
 * simplemente el jugador no recibe nada, en silencio, meses después.
 *
 * <p>Se leen los dos como <b>ficheros</b>, no por el registro, porque el registro necesita un servidor.
 */
class UnlockGrantsJutsuTest {

    private static final Path RESOURCES = Path.of("src", "main", "resources", "data", "dojutsu");

    private static JsonArray unlocks() throws IOException {
        try (Reader reader = new InputStreamReader(
                Files.newInputStream(RESOURCES.resolve("unlocks/progression.json")), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonArray();
        }
    }

    /** Ids de jutsu que existen como archivo, sin extension ni carpeta. */
    private static Set<String> jutsuIds() throws IOException {
        try (Stream<Path> files = Files.walk(RESOURCES.resolve("jutsu"))) {
            Set<String> ids = new HashSet<>();
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> ids.add(p.getFileName().toString().replace(".json", "")));
            return ids;
        }
    }

    @Test
    void todaTecnicaPrometidaPorUnHitoExiste() throws IOException {
        Set<String> existentes = jutsuIds();
        List<String> rotos = new ArrayList<>();

        for (var element : unlocks()) {
            JsonObject unlock = element.getAsJsonObject();
            if (!unlock.has("grantsJutsus")) {
                continue;
            }
            for (var jutsu : unlock.getAsJsonArray("grantsJutsus")) {
                String id = jutsu.getAsString();
                if (!existentes.contains(id)) {
                    rotos.add(unlock.get("unlockId").getAsString() + " -> " + id);
                }
            }
        }
        assertTrue(rotos.isEmpty(), "hitos que prometen una tecnica inexistente: " + rotos);
    }

    @Test
    void losHitosEntreganLasVeintitresTecnicasQueNoTienenOtraVia() throws IOException {
        // Las secuencias de learner cubren naturalezas y Kekkei Genkai; estas 23 solo llegan por hito.
        int entregadas = 0;
        for (var element : unlocks()) {
            JsonObject unlock = element.getAsJsonObject();
            if (unlock.has("grantsJutsus")) {
                entregadas += unlock.getAsJsonArray("grantsJutsus").size();
            }
        }
        assertEquals(23, entregadas);
    }

    @Test
    void losHitosQueNoDanTecnicaSonLosQueDanOtraCosa() throws IOException {
        // Las Ocho Puertas dan una capacidad; el contrato y el kunai del Dios del Trueno dan items; el sello
        // de la Fuerza de Cien y su Creation Rebirth tambien. Que no declaren tecnica es correcto.
        List<String> sinTecnica = new ArrayList<>();
        for (var element : unlocks()) {
            JsonObject unlock = element.getAsJsonObject();
            if (!unlock.has("grantsJutsus")) {
                sinTecnica.add(unlock.get("unlockId").getAsString());
            }
        }
        assertTrue(sinTecnica.stream().allMatch(id ->
                        id.startsWith("eight_gates") || id.equals("summoning_contract_empty")
                                || id.equals("flying_thunder_god_kunai")
                                || id.equals("medical_6_strength_of_a_hundred_seal")
                                || id.equals("medical_7_creation_rebirth_soah")),
                "hay hitos sin entrega que deberian tenerla: " + sinTecnica);
    }
}
