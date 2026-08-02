package com.zamu.dojutsu.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests del mapa de presentacion jutsu → CustomModelData (F4/DJT-085b) y su consistencia con el modelo base
 * ({@code models/item/jutsu.json}): cada override del modelo debe corresponder a una entrada del mapa y
 * viceversa, para que no haya modelos huerfanos ni jutsus sin modelo declarado.
 */
class JutsuModelDataTest {

    private static final Gson GSON = new Gson();

    @Test
    void reutilizaLosCustomModelDataAutenticosDelDatapack() {
        assertEquals(356, JutsuModelData.of("fire_bullet"));
        assertEquals(301, JutsuModelData.of("great_fireball"));
        assertEquals(452, JutsuModelData.of("chidori"));
        assertEquals(499, JutsuModelData.of("secret_sword_snowstorm_beheading"));
    }

    @Test
    void unJutsuSinTexturaPortadaUsaElModeloPorDefecto() {
        assertEquals(0, JutsuModelData.of("jutsu_inexistente"));
    }

    @Test
    void elModeloBaseYElMapaEstanSincronizados() {
        // Recoge los custom_model_data del modelo base y compara con los del mapa: deben coincidir 1:1.
        Set<Integer> modelCmds = new HashSet<>();
        java.util.List<Integer> ordered = new java.util.ArrayList<>();
        try (InputStream in = JutsuModelDataTest.class.getResourceAsStream(
                "/assets/dojutsu/models/item/jutsu.json")) {
            JsonObject model = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            model.getAsJsonArray("overrides").forEach(el -> {
                int cmd = el.getAsJsonObject().getAsJsonObject("predicate").get("custom_model_data").getAsInt();
                modelCmds.add(cmd);
                ordered.add(cmd);
            });
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo leer el modelo base", e);
        }
        // Los overrides DEBEN ir ascendentes: la resolucion vanilla toma el ultimo cuyo cmd <= el del item.
        java.util.List<Integer> sorted = new java.util.ArrayList<>(ordered);
        java.util.Collections.sort(sorted);
        assertEquals(sorted, ordered, "los overrides de custom_model_data deben ir en orden ascendente");
        // Los 24 jutsus del mapa (todos con cmd > 0) deben estar en el modelo base.
        Set<Integer> mapCmds = new HashSet<>();
        for (String id : ALL_TEXTURED) {
            int cmd = JutsuModelData.of(id);
            assertTrue(cmd > 0, id + " deberia tener CustomModelData");
            mapCmds.add(cmd);
        }
        assertEquals(mapCmds, modelCmds, "el modelo base y JutsuModelData deben declarar los mismos CustomModelData");
    }

    /** Los jutsus con textura portada (DJT-085b fuego/rayo/otros + DJT-086 agua). */
    private static final String[] ALL_TEXTURED = {
            "great_fireball", "fire_bullet", "fireproof", "fire_transport", "fire_dragon_flame_bullet",
            "exploding_dragon_strike", "great_fire_annihilation", "lightning_bolt", "lightning_instantaneous_body",
            "lightning_net", "tiger_scissors", "chidori", "lightning_cutter", "kirin",
            "drilling_hand_of_sage_lightning", "substitution", "healing", "earth_wall", "vacuum_wave",
            "dancing_blade_risk", "assassination_technique", "chakra_shock_slash", "chakra_shockwave_slash",
            "secret_sword_snowstorm_beheading",
            "water_prison", "water_instantaneous_body", "breathing_underwater", "water_puddle",
            "rising_water_slicer", "water_severing_wave", "hidden_mist",
            "earth_prison", "earth_dragon_blast", "earth_instantaneous_body", "rock_gun", "earth_golem",
            "tunnelling",
            "propelling_winds", "wind_instantaneous_body", "wind_torpedo", "drilling_air_bullet", "wind_mode",
            "wind_cloak", "rasengan", "rasenshuriken", "big_ball_rasengan",
            "truth_seeking_scorched_earth", "creation_of_all_things", "six_paths_cross_lantern",
            "chakra_enhanced_strength", "heavenly_foot_of_pain", "crimson_drizzle", "cherry_blossom_impact",
            "wood_dome", "great_forest", "great_spear_tree", "tree_bind_flourishing_burial",
            "spontaneous_tree_summoning", "nativity_of_a_world_of_trees", "four_pillar_house", "four_pillar_prison",
            "frozen_waste", "blizzard", "ice_rock_dome", "ice_spear", "ice_golem",
            "lava_snake_bullet", "intense_flow", "raining_debris",
            "the_gods_crossing", "crystal_needles", "jade_crystal_prison", "crystal_wall",
            "thunder_cloud_inner_wave", "laser_circus", "weather_manipulation",
            "boiling_mist_dragon", "meltdown", "erupting_propulsion_fist", "skilled_mist",
            "iron_sand_wall", "iron_sand_wave", "iron_sand_drizzle", "sand_waterfall_funeral",
            "sand_binding_coffin", "sand_wave",
            // Explosión (clay_doll no: summona lobo, sin textura ni CMD)
            "landmine_fist", "explosive_landmine", "exploding_palm", "c0", "c3", "c1",
            // Abrasación (great_steaming_explosive_blast no: sin textura ni CMD)
            "extremely_steaming_murder", "multiple_falling_suns",
            "impervious_armor", "steel_projectile", "steel_shield", "steel_spike",
            "judgment", "inhaling_maw",
            "shadowless_flight", "instantaneous_movement",
            "consecutive_bursting_strong_winds", "great_consecutive_bursting_extreme_winds",
            "landslide", "muddy_water_wall", "mud_doll",
            "honoikazuchi", "kagutsuchi",
            // Genjutsu (phantom_sound_chains no: sin textura ni CMD)
            "paralysis", "bringer_of_darkness", "flower_hill", "hundred_blooming_flowers", "unknown_fire",
            "creation_rebirth", "wide_healing", "mystical_palm", "regenerative_healing",
            "four_symbols_seal", "reverse_four_symbols_sealing", "four_red_yang_formation",
            "insect_sphere", "insect_tornado"
    };
}
