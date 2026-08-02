package com.zamu.dojutsu.jutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.jutsu.JutsuLearningService.LearnOutcome;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Tests del aprendizaje secuencial (F4/DJT-075): la decision pura {@code evaluate} y las
 * secuencias <b>reales</b> del classpath (orden auditado contra {@code learn_<elem>.mcfunction}).
 */
class JutsuLearningServiceTest {

    private static final Gson GSON = new Gson();

    /** Carga la secuencia real de {@code data/dojutsu/learner_sequences/<element>.json}. */
    private static LearnSequence sequence(String element) {
        String path = "/data/dojutsu/learner_sequences/" + element + ".json";
        try (InputStream in = JutsuLearningServiceTest.class.getResourceAsStream(path)) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return LearnSequence.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo cargar " + path, e);
        }
    }

    private static JsonElement json(String raw) {
        return GSON.fromJson(raw, JsonElement.class);
    }

    // ------------------------------------------------------------------
    // Secuencias reales (orden del datapack)
    // ------------------------------------------------------------------

    @Test
    void laSecuenciaDeFuegoCoincideConElDatapack() {
        LearnSequence fire = sequence("fire");
        assertEquals(List.of("fire_bullet", "fireproof", "fire_transport", "fire_dragon_flame_bullet",
                "exploding_dragon_strike", "great_fireball", "great_fire_annihilation", "fire_cloak"),
                fire.sequence());
    }

    @Test
    void laSecuenciaDeRayoCoincideConElDatapack() {
        LearnSequence lightning = sequence("lightning");
        assertEquals(9, lightning.size());
        assertEquals("lightning_bolt", lightning.jutsuAt(0));
        assertEquals("chidori", lightning.jutsuAt(4));
        assertEquals("drilling_hand_of_sage_lightning", lightning.jutsuAt(8));
    }

    @Test
    void unElementoDesconocidoFallaElParseo() {
        var result = LearnSequence.CODEC.parse(JsonOps.INSTANCE,
                json("{\"element\":\"plasma\",\"sequence\":[\"x\"]}"));
        assertTrue(result.error().isPresent());
    }

    @Test
    void unaSecuenciaVaciaFallaElParseo() {
        var result = LearnSequence.CODEC.parse(JsonOps.INSTANCE,
                json("{\"element\":\"fire\",\"sequence\":[]}"));
        assertTrue(result.error().isPresent());
    }

    @Test
    void elElementoDebeCoincidirConElNombreDelArchivo() {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("dojutsu", "otro");
        assertTrue(LearnSequenceReloadListener.parse(file,
                json("{\"element\":\"fire\",\"sequence\":[\"fire_bullet\"]}")).isEmpty());
    }

    // ------------------------------------------------------------------
    // Decision pura del aprendizaje
    // ------------------------------------------------------------------

    @Test
    void aprendeEnElOrdenExactoDelDatapack() {
        LearnSequence fire = sequence("fire");
        JutsuKnowledge k = JutsuKnowledge.DEFAULT;
        LearnOutcome first = JutsuLearningService.evaluate(k, fire, false, id -> true);
        assertEquals(LearnOutcome.Status.LEARN, first.status());
        assertEquals("fire_bullet", first.jutsuId());
        assertEquals(1, first.newProgress());
        assertFalse(first.consumesLearner());

        k = k.withKnown(first.jutsuId()).withProgress("fire", first.newProgress());
        LearnOutcome second = JutsuLearningService.evaluate(k, fire, false, id -> true);
        assertEquals("fireproof", second.jutsuId());
    }

    @Test
    void elUltimoDeLaSecuenciaConsumeElLearner() {
        LearnSequence fire = sequence("fire");
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withProgress("fire", fire.size() - 1);
        LearnOutcome last = JutsuLearningService.evaluate(k, fire, false, id -> true);
        assertEquals(LearnOutcome.Status.LEARN, last.status());
        assertEquals("fire_cloak", last.jutsuId());
        assertTrue(last.consumesLearner());
    }

    @Test
    void conTodosAprendidosAvisaAunqueEsteEnCooldown() {
        // El datapack da precedencia a "ya los tienes todos" sobre el cooldown (lrnd_all quita on_cd).
        LearnSequence fire = sequence("fire");
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withProgress("fire", fire.size());
        assertEquals(LearnOutcome.Status.ALL_LEARNED,
                JutsuLearningService.evaluate(k, fire, true, id -> true).status());
    }

    @Test
    void elCooldownBloqueaElAprendizaje() {
        LearnSequence fire = sequence("fire");
        assertEquals(LearnOutcome.Status.ON_COOLDOWN,
                JutsuLearningService.evaluate(JutsuKnowledge.DEFAULT, fire, true, id -> true).status());
    }

    @Test
    void sinDefinicionCargadaNoAvanzaLaSecuencia() {
        // Contenido incremental (DJT-085/086): el siguiente jutsu sin definicion no se aprende ni avanza.
        LearnSequence fire = sequence("fire");
        LearnOutcome outcome = JutsuLearningService.evaluate(JutsuKnowledge.DEFAULT, fire, false, id -> false);
        assertEquals(LearnOutcome.Status.CONTENT_PENDING, outcome.status());
    }
}
