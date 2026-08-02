package com.zamu.dojutsu.jutsu.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Tests de la logica pura del golpe cargado (F4/DJT-085d): la ventana del {@code Primed} y el parseo de
 * parametros contra los JSON reales. El glue (hook de golpe, rayo, daño) se valida en runtime.
 */
class PrimedStrikeTest {

    private static final Gson GSON = new Gson();

    @Test
    void elGolpeArmadoVivoHastaExpirarLaVentana() {
        PrimedStrikeManager.Primed primed = new PrimedStrikeManager.Primed("chidori", 10.0F, 1060L);
        assertTrue(primed.isLiveAt(1000L));
        assertTrue(primed.isLiveAt(1059L));
        assertFalse(primed.isLiveAt(1060L)); // al tick de expiracion ya no descarga
        assertFalse(primed.isLiveAt(2000L));
    }

    @Test
    void losParametrosDeLosPilotosCoincidenConLosJson() {
        assertParams("lightning/lightning_bolt", 6.0F, 30);
        assertParams("lightning/chidori", 10.0F, 60);
        assertParams("lightning/lightning_cutter", 12.0F, 40);
        assertParams("lightning/drilling_hand_of_sage_lightning", 14.0F, 60);
    }

    @Test
    void losDefaultsAplicanSiFaltanParametros() {
        PrimedStrikeEffect.Params p = PrimedStrikeEffect.Params.parse(new JsonObject());
        assertEquals(6.0F, p.damage());
        assertEquals(30, p.primingTicks());
    }

    private static void assertParams(String subpath, float damage, int priming) {
        JsonObject effectParams = effectParams(subpath);
        PrimedStrikeEffect.Params p = PrimedStrikeEffect.Params.parse(effectParams);
        assertEquals(damage, p.damage());
        assertEquals(priming, p.primingTicks());
    }

    private static JsonObject effectParams(String subpath) {
        String path = "/data/dojutsu/jutsu/" + subpath + ".json";
        try (InputStream in = PrimedStrikeTest.class.getResourceAsStream(path)) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return json.getAsJsonObject().getAsJsonObject("effectParams");
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo cargar " + path, e);
        }
    }
}
