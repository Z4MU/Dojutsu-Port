package com.zamu.dojutsu.jutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Tests de la carga data-driven de jutsus (F4/DJT-073): los JSON piloto <b>reales</b> del
 * classpath (valores auditados contra los {@code cast.mcfunction} del datapack — RA-4/regla 16)
 * y los rechazos del codec estricto y del listener.
 */
class JutsuEntryTest {

    private static final Gson GSON = new Gson();

    /** Carga la entrada real de {@code data/dojutsu/jutsu/<subcarpeta>/<id>.json}. */
    private static JutsuEntry load(String subpath) {
        String path = "/data/dojutsu/jutsu/" + subpath + ".json";
        try (InputStream in = JutsuEntryTest.class.getResourceAsStream(path)) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo cargar " + path, e);
        }
    }

    private static JsonElement json(String raw) {
        return GSON.fromJson(raw, JsonElement.class);
    }

    // ------------------------------------------------------------------
    // Pilotos reales (valores del datapack)
    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Contrato KG: afinidad multiple + posesion de KG (F4/DJT-091)
    // ------------------------------------------------------------------

    /** Decodifica una entrada desde JSON crudo (para probar formas del codec sin archivo). */
    private static JutsuDefinition decode(String raw) {
        return JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(raw)).getOrThrow().definition();
    }

    @Test
    void afinidadComoObjetoSueltoSigueDecodificando() {
        // Retrocompat: la forma de una sola afinidad (todos los jutsus previos a DJT-091).
        JutsuDefinition def = decode("""
            { "id": "great_fireball", "chakraCost": 140, "cooldownTicks": 100,
              "affinity": { "nature": "fire", "amount": 170 }, "category": "fire", "effect": "projectile" }""");
        assertEquals(1, def.affinityRequirements().size());
        assertEquals("fire", def.affinityRequirement().orElseThrow().natureId());
        assertTrue(def.kekkeiGenkaiRequirement().isEmpty());
    }

    @Test
    void afinidadComoArrayDecodificaVariosRequisitos() {
        JutsuDefinition def = decode("""
            { "id": "great_forest", "chakraCost": 120, "cooldownTicks": 200,
              "affinity": [ { "nature": "earth", "amount": 130 }, { "nature": "water", "amount": 130 } ],
              "kekkeiGenkai": "wood", "category": "wood", "effect": "structure" }""");
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("water", def.affinityRequirements().get(1).natureId());
        assertEquals(130, def.affinityRequirements().get(1).amount());
    }

    @Test
    void kekkeiGenkaiRequirementDecodificaPorNombre() {
        JutsuDefinition def = decode("""
            { "id": "great_forest", "chakraCost": 120, "cooldownTicks": 200,
              "affinity": [ { "nature": "earth", "amount": 130 }, { "nature": "water", "amount": 130 } ],
              "kekkeiGenkai": "wood", "category": "wood", "effect": "structure" }""");
        assertEquals("wood", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
    }

    @Test
    void sinCampoAfinidadLaListaEstaVacia() {
        JutsuDefinition def = decode("""
            { "id": "substitution", "chakraCost": 10, "cooldownTicks": 40,
              "category": "universal", "effect": "teleport" }""");
        assertTrue(def.affinityRequirements().isEmpty());
        assertTrue(def.affinityRequirement().isEmpty());
    }

    @Test
    void greatFireballCoincideConElDatapack() {
        JutsuDefinition def = load("fire/great_fireball").definition();
        assertEquals("great_fireball", def.id());
        assertEquals(140, def.chakraCost());
        assertEquals(100, def.cooldownTicks());
        assertEquals("fire", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(170, def.affinityRequirement().orElseThrow().amount());
        assertEquals("fire", def.category());
        assertEquals("projectile", def.effectId());
    }

    @Test
    void fireBulletCoincideConElDatapack() {
        JutsuDefinition def = load("fire/fire_bullet").definition();
        assertEquals(60, def.chakraCost());
        assertEquals(100, def.cooldownTicks());
        assertEquals(30, def.affinityRequirement().orElseThrow().amount());
    }

    @Test
    void lightningBoltCoincideConElDatapack() {
        JutsuDefinition def = load("lightning/lightning_bolt").definition();
        assertEquals(60, def.chakraCost());
        // Corregido en DJT-076: el cd real es 200t TRAS el golpe (hit_effect); los 30t eran la
        // ventana de carga del golpe ("Attack something to use Jutsu!") -> effectParams.primingTicks.
        assertEquals(200, def.cooldownTicks());
        assertEquals("primed_strike", def.effectId());
        assertEquals("lightning", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(30, def.affinityRequirement().orElseThrow().amount());
    }

    @Test
    void chidoriCoincideConElDatapack() {
        JutsuDefinition def = load("lightning/chidori").definition();
        assertEquals(100, def.chakraCost());
        assertEquals(400, def.cooldownTicks());
        assertEquals(120, def.affinityRequirement().orElseThrow().amount());
    }

    // ------------------------------------------------------------------
    // Codec estricto: defaults y rechazos
    // ------------------------------------------------------------------

    @Test
    void dancingBladeRiskCoincideConElDatapack() {
        // Tecnica de espada (F4/DJT-083): sin afinidad, requisito de nivel de kenjutsu (300 datapack ÷4 = 75).
        JutsuDefinition def = load("kenjutsu/dancing_blade_risk").definition();
        assertEquals(0, def.chakraCost());
        assertEquals(60, def.cooldownTicks());
        assertTrue(def.affinityRequirement().isEmpty());
        assertEquals("kenjutsu", def.skillRequirement().orElseThrow().skillId());
        assertEquals(75, def.skillRequirement().orElseThrow().amount());
        assertEquals("kenjutsu", def.category());
    }

    @Test
    void healingExigeNivelDeMedical() {
        // Cierra el TODO de DJT-076: healing pide medical >= 5 (20 datapack ÷4).
        JutsuDefinition def = load("universal/healing").definition();
        assertEquals("medical", def.skillRequirement().orElseThrow().skillId());
        assertEquals(5, def.skillRequirement().orElseThrow().amount());
    }

    @Test
    void habilidadDesconocidaFallaElParseo() {
        var result = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(
                "{\"id\":\"x\",\"chakraCost\":0,\"cooldownTicks\":0,\"skill\":{\"skill\":\"nadar\",\"amount\":5},"
                        + "\"category\":\"kenjutsu\",\"effect\":\"area\"}"));
        assertTrue(result.error().isPresent());
    }

    // ------------------------------------------------------------------
    // Volcado Fuego + Rayo (DJT-085): valores auditados del datapack
    // ------------------------------------------------------------------

    @Test
    void elVolcadoDeFuegoCargaConLosValoresDelDatapack() {
        // chakra / afinidad fire, auditados uno a uno en los cast.mcfunction.
        assertFireJutsu("fire/fire_transport", 80, 60);
        assertFireJutsu("fire/fire_dragon_flame_bullet", 100, 100);
        assertFireJutsu("fire/exploding_dragon_strike", 150, 140);
        assertFireJutsu("fire/great_fire_annihilation", 180, 200);
    }

    @Test
    void elVolcadoDeRayoCargaConLosValoresDelDatapack() {
        assertLightningJutsu("lightning/lightning_instantaneous_body", 80, 60);
        assertLightningJutsu("lightning/lightning_net", 80, 80);
        assertLightningJutsu("lightning/tiger_scissors", 100, 100);
        assertLightningJutsu("lightning/lightning_cutter", 200, 200);
    }

    @Test
    void elVolcadoDeAguaCargaConLosValoresDelDatapack() {
        // chakra / afinidad water, auditados en los cast.mcfunction (DJT-086).
        assertWaterJutsu("water_prison", 30, 30);
        assertWaterJutsu("water_puddle", 30, 40);
        assertWaterJutsu("breathing_underwater", 20, 50);
        assertWaterJutsu("water_instantaneous_body", 80, 60);
        assertWaterJutsu("hidden_mist", 60, 80);
        assertWaterJutsu("rising_water_slicer", 100, 100);
        assertWaterJutsu("water_severing_wave", 140, 160);
    }

    @Test
    void elVolcadoDeTierraCargaConLosValoresDelDatapack() {
        assertEarthJutsu("earth_prison", 30, 30);
        assertEarthJutsu("rock_gun", 60, 30);
        assertEarthJutsu("earth_instantaneous_body", 80, 60);
        assertEarthJutsu("earth_golem", 100, 100);
        assertEarthJutsu("earth_dragon_blast", 120, 150);
        assertEarthJutsu("tunnelling", 160, 180);
    }

    @Test
    void elVolcadoDeVientoCargaConLosValoresDelDatapack() {
        assertWindJutsu("propelling_winds", 30, 30);
        assertWindJutsu("wind_torpedo", 60, 50);
        assertWindJutsu("drilling_air_bullet", 100, 100);
        assertWindJutsu("rasengan", 100, 150);
        assertWindJutsu("big_ball_rasengan", 200, 200);
        // rasenshuriken: datapack aff 600 -> cap 500 del mod (como kirin/drilling).
        assertWindJutsu("rasenshuriken", 300, 500);
    }

    @Test
    void losRasenganSonPrimedStrike() {
        assertEquals("primed_strike", load("wind/rasengan").definition().effectId());
        assertEquals("primed_strike", load("wind/big_ball_rasengan").definition().effectId());
    }

    @Test
    void elVolcadoDeYinYangCargaConLosValoresDelDatapack() {
        // aff_yinyang del datapack (800-2000) es inalcanzable: la afinidad yin_yang es derivada
        // (yin+yang)/2, cap 500. Se capa a 500 (como kirin/rasenshuriken); el chakra diferencia el tier.
        assertYinYangJutsu("truth_seeking_scorched_earth", 900, 500);
        assertYinYangJutsu("sword_of_nunoboko", 200, 500);
        assertYinYangJutsu("six_paths_cross_lantern", 1200, 500);
        assertYinYangJutsu("creation_of_all_things", 4000, 500);
    }

    @Test
    void creationOfAllThingsEsUnHealMasivo() {
        assertEquals("heal", load("yin_yang/creation_of_all_things").definition().effectId());
        assertEquals("primed_strike", load("yin_yang/sword_of_nunoboko").definition().effectId());
    }

    @Test
    void elVolcadoDeMaderaCargaConElContratoKg() {
        // Madera (Mokuton, primer KG): gatea por poseer 'wood' + afinidad earth Y water (raw; nativity 800->500).
        assertWoodJutsu("tree_bind_flourishing_burial", 60, 100);
        assertWoodJutsu("great_spear_tree", 100, 120);
        assertWoodJutsu("great_forest", 120, 130);
        assertWoodJutsu("wood_dome", 140, 160);
        assertWoodJutsu("spontaneous_tree_summoning", 300, 250);
        assertWoodJutsu("four_pillar_prison", 400, 350);
        assertWoodJutsu("four_pillar_house", 800, 400);
        assertWoodJutsu("nativity_of_a_world_of_trees", 1000, 500); // datapack 800 -> cap 500
    }

    @Test
    void elVolcadoDeHieloCargaConElContratoKg() {
        // Hielo: KG 'ice', afinidad water Y wind (raw, todos <=500).
        assertIceJutsu("frozen_waste", 80, 60);
        assertIceJutsu("blizzard", 120, 80);
        assertIceJutsu("ice_golem", 100, 100);
        assertIceJutsu("ice_spear", 100, 120);
        assertIceJutsu("ice_rock_dome", 140, 160);
    }

    @Test
    void elVolcadoDeLavaCargaConElContratoKg() {
        // Lava: KG 'lava', afinidad earth Y fire (raw, todos <=500).
        assertLavaJutsu("lava_snake_bullet", 180, 150);
        assertLavaJutsu("raining_debris", 200, 200);
        assertLavaJutsu("intense_flow", 400, 300);
    }

    @Test
    void elVolcadoDeCristalCargaTresAfinidades() {
        // Cristal: primer KG de TRES afinidades (earth, water, yang), todas raw <=500.
        assertCrystalJutsu("crystal_wall", 60, 50);
        assertCrystalJutsu("crystal_needles", 120, 80);
        assertCrystalJutsu("jade_crystal_prison", 150, 120);
        assertCrystalJutsu("the_gods_crossing", 150, 160);
    }

    @Test
    void elVolcadoDeTormentaCargaConElContratoKg() {
        // Tormenta: KG 'storm', afinidad lightning Y water (raw, todos <=500).
        assertStormJutsu("thunder_cloud_inner_wave", 140, 120);
        assertStormJutsu("laser_circus", 180, 180);
        assertStormJutsu("weather_manipulation", 250, 250);
    }

    @Test
    void elVolcadoDeEbullicionCargaConElContratoKg() {
        // Ebullición: KG 'boil', afinidad fire Y water (raw, todos <=500).
        assertBoilJutsu("boiling_mist_dragon", 150, 140);
        assertBoilJutsu("erupting_propulsion_fist", 200, 180);
        assertBoilJutsu("meltdown", 280, 240);
        assertBoilJutsu("skilled_mist", 330, 300);
    }

    @Test
    void elVolcadoDeMagnetismoCargaConElContratoKg() {
        // Magnetismo: KG 'magnet', afinidad earth Y wind (raw, todos <=500).
        assertMagnetJutsu("sand_wave", 120, 120);
        assertMagnetJutsu("iron_sand_wall", 200, 140);
        assertMagnetJutsu("iron_sand_wave", 140, 140);
        assertMagnetJutsu("iron_sand_drizzle", 160, 160);
        assertMagnetJutsu("sand_binding_coffin", 180, 180);
        assertMagnetJutsu("sand_waterfall_funeral", 300, 250);
    }

    @Test
    void elVolcadoDeExplosionCargaConElContratoKg() {
        // Explosión: KG 'explosion', afinidad earth Y lightning (raw; c0 a 500).
        assertExplosionJutsu("c1", 140, 120);
        assertExplosionJutsu("explosive_landmine", 120, 140);
        assertExplosionJutsu("landmine_fist", 180, 180);
        assertExplosionJutsu("exploding_palm", 250, 250);
        assertExplosionJutsu("c3", 280, 280);
        assertExplosionJutsu("clay_doll", 280, 300);
        assertExplosionJutsu("c0", 600, 500);
    }

    @Test
    void elVolcadoDeAbrasacionCargaConElContratoKg() {
        // Abrasación: KG 'scorch', afinidad fire Y wind (raw, todos <=500).
        assertScorchJutsu("great_steaming_explosive_blast", 180, 150);
        assertScorchJutsu("multiple_falling_suns", 230, 200);
        assertScorchJutsu("extremely_steaming_murder", 300, 300);
    }

    @Test
    void elVolcadoDeAceroCargaTresAfinidades() {
        // Acero: KG 'steel', TRES afinidades (fire, earth, water), todas raw <=500.
        assertSteelJutsu("steel_projectile", 120, 80);
        assertSteelJutsu("impervious_armor", 200, 160);
        assertSteelJutsu("steel_shield", 140, 160);
        assertSteelJutsu("steel_spike", 180, 200);
    }

    @Test
    void elVolcadoDeOscuridadCargaConElContratoKg() {
        // Oscuridad: KG 'dark', afinidad wind Y yin (raw, todos <=500).
        assertDarkJutsu("judgment", 120, 140);
        assertDarkJutsu("inhaling_maw", 160, 200);
    }

    @Test
    void elVolcadoDeRapidezCargaConElContratoKg() {
        // Rapidez: KG 'swift', afinidad lightning Y wind (raw, todos <=500).
        assertSwiftJutsu("shadowless_flight", 100, 160);
        assertSwiftJutsu("instantaneous_movement", 200, 300);
    }

    @Test
    void elVolcadoDeTifonCargaConElContratoKg() {
        // Tifón: KG 'typhoon', afinidad water Y wind (raw, todos <=500).
        assertTyphoonJutsu("consecutive_bursting_strong_winds", 180, 150);
        assertTyphoonJutsu("great_consecutive_bursting_extreme_winds", 230, 250);
    }

    @Test
    void elVolcadoDeLodoCargaConElContratoKg() {
        // Lodo: KG 'mud', afinidad earth Y water (raw, todos <=500).
        assertMudJutsu("muddy_water_wall", 100, 100);
        assertMudJutsu("landslide", 120, 130);
        assertMudJutsu("mud_doll", 240, 200);
    }

    @Test
    void elVolcadoDeIgneaGateaSoloPorPosesionDeKg() {
        // Ígnea/Blaze: KG 'blaze' SIN requisito de afinidad (gatea solo por poseer el KG + chakra).
        JutsuDefinition hono = load("blaze/honoikazuchi").definition();
        assertEquals(180, hono.chakraCost());
        assertEquals("blaze", hono.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertTrue(hono.affinityRequirements().isEmpty()); // sin afinidad
        assertEquals("projectile", hono.effectId());

        JutsuDefinition kagu = load("blaze/kagutsuchi").definition();
        assertEquals("blaze", kagu.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertTrue(kagu.affinityRequirements().isEmpty());
        assertEquals("area", kagu.effectId());
    }

    @Test
    void elVolcadoDePolvoExigeLasTresAfinidadesAlMaximo() {
        // Polvo/Dust (Jinton de Onoki): el jutsu definitivo. Gate dust + fire/wind/earth TODAS a 500.
        JutsuDefinition def = load("dust/detachment_of_the_primitive_world").definition();
        assertEquals(600, def.chakraCost());
        assertEquals("dust", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(3, def.affinityRequirements().size());
        assertEquals("fire", def.affinityRequirements().get(0).natureId());
        assertEquals("wind", def.affinityRequirements().get(1).natureId());
        assertEquals("earth", def.affinityRequirements().get(2).natureId());
        def.affinityRequirements().forEach(req -> assertEquals(500, req.amount())); // todas al máximo
        assertEquals("dust", def.category());
    }

    @Test
    void elVolcadoDeGenjutsuCombinaSkillYAfinidad() {
        // Genjutsu: gate skill genjutsu (/4) + afinidad yin (raw). Primer caso con AMBOS requisitos.
        JutsuDefinition bringer = load("genjutsu/bringer_of_darkness").definition();
        assertEquals(60, bringer.chakraCost());
        assertEquals("genjutsu", bringer.skillRequirement().orElseThrow().skillId());
        assertEquals(18, bringer.skillRequirement().orElseThrow().amount()); // datapack 70 /4
        assertEquals("yin", bringer.affinityRequirement().orElseThrow().natureId());
        assertEquals(10, bringer.affinityRequirement().orElseThrow().amount());
        assertEquals("genjutsu", bringer.category());

        // unknown_fire: el genjutsu de mayor exigencia (150 /4 = 38, yin 60).
        JutsuDefinition uf = load("genjutsu/unknown_fire").definition();
        assertEquals(38, uf.skillRequirement().orElseThrow().amount());
        assertEquals(60, uf.affinityRequirement().orElseThrow().amount());

        // phantom_sound_chains: solo skill, sin afinidad ni coste de chakra.
        JutsuDefinition psc = load("genjutsu/phantom_sound_chains").definition();
        assertEquals(0, psc.chakraCost());
        assertTrue(psc.affinityRequirements().isEmpty());
        assertEquals("genjutsu", psc.skillRequirement().orElseThrow().skillId());
    }

    @Test
    void elVolcadoDeMedicalCargaConSkillMedical() {
        // Medical: gate skill medical (/4), familia heal. creation_rebirth combina medical + yin.
        assertMedicalJutsu("regenerative_healing", 40, 3);
        assertMedicalJutsu("wide_healing", 80, 13);
        assertMedicalJutsu("mystical_palm", 10, 25);
        assertMedicalJutsu("creation_rebirth", 1000, 125);

        JutsuDefinition cr = load("medical/creation_rebirth").definition();
        assertEquals("yin", cr.affinityRequirement().orElseThrow().natureId()); // + afinidad yin 50
        assertEquals(50, cr.affinityRequirement().orElseThrow().amount());
    }

    @Test
    void elVolcadoDeFuinjutsuCombinaSkillYYang() {
        // Fuinjutsu: gate skill fuinjutsu (/4) + afinidad yang (raw; four_red_yang cap 500).
        assertFuinjutsuJutsu("four_symbols_seal", 100, 300, 200);
        assertFuinjutsuJutsu("reverse_four_symbols_sealing", 1200, 400, 400);
        assertFuinjutsuJutsu("four_red_yang_formation", 2000, 500, 500); // yang datapack 800 -> cap 500
    }

    @Test
    void elVolcadoDeAburameNoTieneGateDeAfinidadNiSkill() {
        // Aburame: jutsu de clan, gatea solo por chakra (lo entrega el clan). Sin afinidad/skill/KG.
        JutsuDefinition sphere = load("aburame/insect_sphere").definition();
        assertEquals(20, sphere.chakraCost());
        assertTrue(sphere.affinityRequirements().isEmpty());
        assertTrue(sphere.skillRequirement().isEmpty());
        assertTrue(sphere.kekkeiGenkaiRequirement().isEmpty());
        assertEquals("aburame", sphere.category());

        JutsuDefinition tornado = load("aburame/insect_tornado").definition();
        assertEquals(30, tornado.chakraCost());
        assertTrue(tornado.affinityRequirements().isEmpty());
        assertTrue(tornado.skillRequirement().isEmpty());
    }

    private static void assertFuinjutsuJutsu(String id, int chakra, int skillAmount, int yang) {
        JutsuDefinition def = load("fuinjutsu/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("fuinjutsu", def.skillRequirement().orElseThrow().skillId());
        assertEquals(skillAmount, def.skillRequirement().orElseThrow().amount());
        assertEquals("yang", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(yang, def.affinityRequirement().orElseThrow().amount());
        assertEquals("fuinjutsu", def.category());
    }

    private static void assertMedicalJutsu(String id, int chakra, int skillAmount) {
        JutsuDefinition def = load("medical/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("medical", def.skillRequirement().orElseThrow().skillId());
        assertEquals(skillAmount, def.skillRequirement().orElseThrow().amount());
        assertEquals("heal", def.effectId());
        assertEquals("medical", def.category());
    }

    private static void assertMudJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("mud/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("mud", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("water", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("mud", def.category());
    }

    private static void assertTyphoonJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("typhoon/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("typhoon", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("water", def.affinityRequirements().get(0).natureId());
        assertEquals("wind", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("typhoon", def.category());
    }

    private static void assertSwiftJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("swift/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("swift", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("lightning", def.affinityRequirements().get(0).natureId());
        assertEquals("wind", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("swift", def.category());
    }

    private static void assertDarkJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("dark/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("dark", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("wind", def.affinityRequirements().get(0).natureId());
        assertEquals("yin", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("dark", def.category());
    }

    private static void assertSteelJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("steel/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("steel", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(3, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("fire", def.affinityRequirements().get(1).natureId());
        assertEquals("water", def.affinityRequirements().get(2).natureId());
        def.affinityRequirements().forEach(req -> assertEquals(affinity, req.amount()));
        assertEquals("steel", def.category());
    }

    private static void assertScorchJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("scorch/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("scorch", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("fire", def.affinityRequirements().get(0).natureId());
        assertEquals("wind", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("scorch", def.category());
    }

    private static void assertExplosionJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("explosion/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("explosion", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("lightning", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("explosion", def.category());
    }

    private static void assertMagnetJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("magnet/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("magnet", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("wind", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("magnet", def.category());
    }

    private static void assertBoilJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("boil/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("boil", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("fire", def.affinityRequirements().get(0).natureId());
        assertEquals("water", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("boil", def.category());
    }

    private static void assertStormJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("storm/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("storm", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("lightning", def.affinityRequirements().get(0).natureId());
        assertEquals("water", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("storm", def.category());
    }

    private static void assertCrystalJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("crystal/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("crystal", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(3, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("water", def.affinityRequirements().get(1).natureId());
        assertEquals("yang", def.affinityRequirements().get(2).natureId());
        def.affinityRequirements().forEach(req -> assertEquals(affinity, req.amount()));
        assertEquals("crystal", def.category());
    }

    private static void assertLavaJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("lava/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("lava", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("fire", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("lava", def.category());
    }

    private static void assertIceJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("ice/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("ice", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("water", def.affinityRequirements().get(0).natureId());
        assertEquals("wind", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("ice", def.category());
    }

    private static void assertWoodJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("wood/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("wood", def.kekkeiGenkaiRequirement().orElseThrow().kekkeiGenkaiId());
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirements().get(0).natureId());
        assertEquals("water", def.affinityRequirements().get(1).natureId());
        assertEquals(affinity, def.affinityRequirements().get(0).amount());
        assertEquals(affinity, def.affinityRequirements().get(1).amount());
        assertEquals("wood", def.category());
    }

    @Test
    void elVolcadoDeTaijutsuCargaConLosValoresDelDatapack() {
        // d.taijutsu del datapack (100/150/200/250) reescalado /4 como kenjutsu (mod 0-500).
        assertTaijutsuJutsu("chakra_enhanced_strength", 40, 25);
        assertTaijutsuJutsu("heavenly_foot_of_pain", 80, 38);
        assertTaijutsuJutsu("crimson_drizzle", 100, 50);
        assertTaijutsuJutsu("cherry_blossom_impact", 200, 63);
    }

    private static void assertTaijutsuJutsu(String id, int chakra, int skillAmount) {
        JutsuDefinition def = load("taijutsu/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertTrue(def.affinityRequirement().isEmpty()); // taijutsu no gasta afinidad
        assertEquals("taijutsu", def.skillRequirement().orElseThrow().skillId());
        assertEquals(skillAmount, def.skillRequirement().orElseThrow().amount());
        assertEquals("taijutsu", def.category());
    }

    private static void assertYinYangJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("yin_yang/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("yin_yang", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(affinity, def.affinityRequirement().orElseThrow().amount());
        assertEquals("yin_yang", def.category());
    }

    private static void assertWindJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("wind/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("wind", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(affinity, def.affinityRequirement().orElseThrow().amount());
        assertEquals("wind", def.category());
    }

    private static void assertEarthJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("earth/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("earth", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(affinity, def.affinityRequirement().orElseThrow().amount());
        assertEquals("earth", def.category());
    }

    private static void assertWaterJutsu(String id, int chakra, int affinity) {
        JutsuDefinition def = load("water/" + id).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("water", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(affinity, def.affinityRequirement().orElseThrow().amount());
        assertEquals("water", def.category());
    }

    @Test
    void kirinYDrillingCapanLaAfinidadAlMaximoDelMod() {
        // Datapack: kirin aff 600, drilling 800; el mod capa la afinidad a 500 (XpCurve). Capados.
        assertEquals(500, load("lightning/kirin").definition().affinityRequirement().orElseThrow().amount());
        assertEquals(300, load("lightning/kirin").definition().chakraCost());
        assertEquals(500, load("lightning/drilling_hand_of_sage_lightning").definition()
                .affinityRequirement().orElseThrow().amount());
    }

    @Test
    void losMeleeChargeUsanLaFamiliaPrimedStrike() {
        // lightning_cutter y drilling son cargas de golpe (como chidori): familia primed_strike (DJT-085d).
        assertEquals("primed_strike", load("lightning/lightning_cutter").definition().effectId());
        assertEquals("primed_strike", load("lightning/drilling_hand_of_sage_lightning").definition().effectId());
    }

    private static void assertFireJutsu(String subpath, int chakra, int affinity) {
        JutsuDefinition def = load(subpath).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("fire", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(affinity, def.affinityRequirement().orElseThrow().amount());
        assertEquals("fire", def.category());
    }

    private static void assertLightningJutsu(String subpath, int chakra, int affinity) {
        JutsuDefinition def = load(subpath).definition();
        assertEquals(chakra, def.chakraCost());
        assertEquals("lightning", def.affinityRequirement().orElseThrow().natureId());
        assertEquals(affinity, def.affinityRequirement().orElseThrow().amount());
        assertEquals("lightning", def.category());
    }

    @Test
    void jutsuUniversalSinAfinidadYConDefaults() {
        JutsuEntry entry = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(
                "{\"id\":\"sub\",\"chakraCost\":10,\"cooldownTicks\":0,\"category\":\"universal\",\"effect\":\"teleport\"}"))
                .getOrThrow();
        assertTrue(entry.definition().affinityRequirement().isEmpty());
        assertEquals(0, entry.definition().levelRequirement());
        assertTrue(entry.effectParams().isJsonObject());
        assertTrue(entry.effectParams().getAsJsonObject().isEmpty());
    }

    @Test
    void naturalezaDesconocidaFallaElParseo() {
        var result = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(
                "{\"id\":\"x\",\"chakraCost\":10,\"cooldownTicks\":0,\"affinity\":{\"nature\":\"lava\",\"amount\":5},"
                        + "\"category\":\"fire\",\"effect\":\"projectile\"}"));
        assertTrue(result.error().isPresent());
    }

    @Test
    void costeNegativoFallaElParseo() {
        var result = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(
                "{\"id\":\"x\",\"chakraCost\":-1,\"cooldownTicks\":0,\"category\":\"fire\",\"effect\":\"projectile\"}"));
        assertTrue(result.error().isPresent());
    }

    @Test
    void efectoAusenteOVacioFallaElParseo() {
        assertTrue(JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(
                "{\"id\":\"x\",\"chakraCost\":1,\"cooldownTicks\":0,\"category\":\"fire\"}")).error().isPresent());
        assertTrue(JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(
                "{\"id\":\"x\",\"chakraCost\":1,\"cooldownTicks\":0,\"category\":\"fire\",\"effect\":\"\"}"))
                .error().isPresent());
    }

    @Test
    void losParametrosDeEfectoSeConservanTalCual() {
        JutsuEntry entry = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(
                "{\"id\":\"x\",\"chakraCost\":1,\"cooldownTicks\":0,\"category\":\"fire\",\"effect\":\"projectile\","
                        + "\"effectParams\":{\"speed\":1.5,\"damage\":8}}"))
                .getOrThrow();
        assertEquals(1.5, entry.effectParams().getAsJsonObject().get("speed").getAsDouble());
        assertEquals(8, entry.effectParams().getAsJsonObject().get("damage").getAsInt());
    }

    @Test
    void losParamsAusentesNoCompartenInstancia() {
        String raw = "{\"id\":\"x\",\"chakraCost\":1,\"cooldownTicks\":0,\"category\":\"fire\",\"effect\":\"projectile\"}";
        JutsuEntry a = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(raw)).getOrThrow();
        JutsuEntry b = JutsuEntry.CODEC.parse(JsonOps.INSTANCE, json(raw)).getOrThrow();
        // Instancias frescas: un JsonObject default compartido seria envenenable (regla 10b).
        org.junit.jupiter.api.Assertions.assertNotSame(a.effectParams(), b.effectParams());
    }

    // ------------------------------------------------------------------
    // Listener: id == nombre de archivo (con subcarpetas) y robustez
    // ------------------------------------------------------------------

    @Test
    void unIdDeSoloEspaciosNoTumbaElListener() {
        // Pasa NON_EMPTY_STRING pero el constructor de dominio lanza DENTRO del apply del codec:
        // el listener debe capturarlo y descartar el archivo, nunca propagar al /reload.
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("dojutsu", "fire/x");
        var parsed = JutsuReloadListener.parse(file, json(
                "{\"id\":\" \",\"chakraCost\":1,\"cooldownTicks\":0,\"category\":\"fire\",\"effect\":\"projectile\"}"));
        assertTrue(parsed.isEmpty());
    }

    @Test
    void elIdDebeCoincidirConElNombreDelArchivo() {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("dojutsu", "fire/otro_nombre");
        var parsed = JutsuReloadListener.parse(file, json(
                "{\"id\":\"great_fireball\",\"chakraCost\":1,\"cooldownTicks\":0,\"category\":\"fire\",\"effect\":\"projectile\"}"));
        assertTrue(parsed.isEmpty());
    }

    @Test
    void elNombreBaseIgnoraLasSubcarpetas() {
        assertEquals("great_fireball", JutsuReloadListener.fileBaseName(
                ResourceLocation.fromNamespaceAndPath("dojutsu", "fire/great_fireball")));
        assertEquals("sub", JutsuReloadListener.fileBaseName(
                ResourceLocation.fromNamespaceAndPath("dojutsu", "sub")));
    }

    @Test
    void archivoValidoConSubcarpetaPasaElListener() {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath("dojutsu", "lightning/chidori");
        var parsed = JutsuReloadListener.parse(file, json(
                "{\"id\":\"chidori\",\"chakraCost\":100,\"cooldownTicks\":400,"
                        + "\"affinity\":{\"nature\":\"lightning\",\"amount\":120},"
                        + "\"category\":\"lightning\",\"effect\":\"buff\"}"));
        assertFalse(parsed.isEmpty());
        assertEquals("chidori", parsed.orElseThrow().id());
    }
}
