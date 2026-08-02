package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Tests del aislamiento por archivo del {@link EyeReloadListener} (DJT-113): un ojo valido carga; un id que no
 * coincide con el archivo, una familia desconocida (codec estricto) o un requisito mal formado (lanza en el
 * constructor de dominio) se descartan <b>sin romper</b> el resto de la carga.
 */
class EyeReloadListenerTest {

    private static final Gson GSON = new Gson();

    private static Optional<EyeDefinition> parse(String fileName, String json) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("dojutsu", fileName);
        return EyeReloadListener.parse(file, GSON.fromJson(json, JsonElement.class));
    }

    @Test
    void unOjoValidoCarga() {
        Optional<EyeDefinition> eye = parse("sharingan",
                "{\"id\":\"sharingan\",\"family\":\"sharingan\"}");
        assertTrue(eye.isPresent());
        assertEquals("sharingan", eye.orElseThrow().id());
    }

    @Test
    void idQueNoCoincideConElArchivoSeDescarta() {
        Optional<EyeDefinition> eye = parse("sharingan",
                "{\"id\":\"otro\",\"family\":\"sharingan\"}");
        assertFalse(eye.isPresent());
    }

    @Test
    void familiaDesconocidaSeDescarta() {
        // codec estricto en contenido: una familia que no existe falla el parseo (no se cuela en silencio).
        Optional<EyeDefinition> eye = parse("raro",
                "{\"id\":\"raro\",\"family\":\"familia_inventada\"}");
        assertFalse(eye.isPresent());
    }

    @Test
    void requisitoMalFormadoNoTumbaLaCarga() {
        // Un requisito de clan sin 'value' lanza en el constructor de EyeRequirement; el try/catch del parse lo
        // convierte en "se ignora", no en una excepcion que tumbe el /reload.
        Optional<EyeDefinition> eye = parse("sharingan",
                "{\"id\":\"sharingan\",\"family\":\"sharingan\",\"evolutions\":"
                        + "[{\"to\":\"mangekyo_itachi\",\"requirements\":[{\"type\":\"clan\"}]}]}");
        assertFalse(eye.isPresent());
    }

    @Test
    void elPlaceholderCargaSinRomper() {
        Optional<EyeDefinition> eye = parse("naka",
                "{\"id\":\"naka\",\"family\":\"mangekyo\",\"implemented\":false}");
        assertTrue(eye.isPresent());
        assertFalse(eye.orElseThrow().implemented());
    }
}
