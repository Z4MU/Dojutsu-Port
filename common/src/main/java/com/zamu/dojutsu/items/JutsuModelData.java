package com.zamu.dojutsu.items;

import java.util.Map;

/**
 * Mapa de <b>presentacion</b> jutsu → {@code CustomModelData} (F4/DJT-085b). El ítem {@code dojutsu:jutsu} es
 * uno solo; su modelo se selecciona por el data component {@code minecraft:custom_model_data} (predicado vanilla
 * de 1.21), que {@link JutsuItem#createStack} fija segun este mapa. El modelo base
 * ({@code models/item/jutsu.json}) enlaza cada numero con el modelo del jutsu.
 *
 * <p><b>Presentacion, no gameplay:</b> el numero de modelo NO vive en el JSON de la tecnica (que es
 * coste/cd/afinidad/efecto), sino aqui — separa el "que hace" del "como se ve". Se reutilizan los
 * {@code CustomModelData} <b>autenticos del datapack</b> (extraidos de {@code give/jutsu/*}) para fidelidad y
 * para no colisionar con futuros contenidos.
 *
 * <p>Anadir la textura de un jutsu nuevo = 1 entrada aqui + su modelo + su textura + 1 override en el modelo
 * base. Un jutsu sin entrada (0) usa el modelo por defecto (pergamino generico).
 */
public final class JutsuModelData {

    /** {@code jutsuId → CustomModelData} (numeros autenticos del datapack). 0 = modelo por defecto. */
    private static final Map<String, Integer> MODEL_DATA = Map.ofEntries(
            // Fuego
            Map.entry("great_fireball", 301),
            Map.entry("fire_bullet", 356),
            Map.entry("fireproof", 314),
            Map.entry("fire_transport", 315),
            Map.entry("fire_dragon_flame_bullet", 338),
            Map.entry("exploding_dragon_strike", 347),
            Map.entry("great_fire_annihilation", 401),
            // Rayo
            Map.entry("lightning_bolt", 302),
            Map.entry("lightning_instantaneous_body", 304),
            Map.entry("lightning_net", 350),
            Map.entry("tiger_scissors", 351),
            Map.entry("chidori", 452),
            Map.entry("lightning_cutter", 460),
            Map.entry("kirin", 305),
            Map.entry("drilling_hand_of_sage_lightning", 465),
            // Cristal (DJT-095, KG earth+water+yang)
            Map.entry("the_gods_crossing", 466),
            Map.entry("crystal_needles", 467),
            Map.entry("jade_crystal_prison", 468),
            Map.entry("crystal_wall", 469),
            // Aburame (DJT-111, jutsu de clan, sin gate)
            Map.entry("insect_sphere", 358),
            Map.entry("insect_tornado", 359),
            // Fuinjutsu (DJT-110, skill fuinjutsu + yang)
            Map.entry("four_symbols_seal", 447),
            Map.entry("reverse_four_symbols_sealing", 448),
            Map.entry("four_red_yang_formation", 449),
            // Medical (DJT-109, skill medical)
            Map.entry("creation_rebirth", 405),
            Map.entry("wide_healing", 408),
            Map.entry("mystical_palm", 409),
            Map.entry("regenerative_healing", 410),
            // Genjutsu (DJT-108, skill genjutsu + yin) — phantom_sound_chains sin textura ni CMD
            Map.entry("paralysis", 423),
            Map.entry("bringer_of_darkness", 424),
            Map.entry("flower_hill", 425),
            Map.entry("hundred_blooming_flowers", 426),
            Map.entry("unknown_fire", 427),
            // Ígnea / Blaze (DJT-106, KG solo posesión) — kagutsuchi_blade es un arma (CMD 1004), no jutsu
            Map.entry("honoikazuchi", 411),
            Map.entry("kagutsuchi", 412),
            // Lodo / Mud (DJT-105, KG earth+water)
            Map.entry("landslide", 480),
            Map.entry("muddy_water_wall", 481),
            Map.entry("mud_doll", 482),
            // Tifón / Typhoon (DJT-104, KG wind+water)
            Map.entry("consecutive_bursting_strong_winds", 478),
            Map.entry("great_consecutive_bursting_extreme_winds", 479),
            // Rapidez / Swift (DJT-103, KG wind+lightning)
            Map.entry("shadowless_flight", 476),
            Map.entry("instantaneous_movement", 477),
            // Oscuridad / Dark (DJT-102, KG wind+yin)
            Map.entry("judgment", 474),
            Map.entry("inhaling_maw", 475),
            // Acero / Steel (DJT-101, KG fire+earth+water)
            Map.entry("impervious_armor", 470),
            Map.entry("steel_projectile", 471),
            Map.entry("steel_shield", 472),
            Map.entry("steel_spike", 473),
            // Abrasación / Scorch (DJT-100, KG fire+wind) — great_steaming_explosive_blast sin textura ni CMD
            Map.entry("extremely_steaming_murder", 413),
            Map.entry("multiple_falling_suns", 415),
            // Explosión / Explosion (DJT-099, KG lightning+earth) — clay_doll summona lobo, sin textura ni CMD
            Map.entry("landmine_fist", 329),
            Map.entry("explosive_landmine", 330),
            Map.entry("exploding_palm", 442),
            Map.entry("c0", 443),
            Map.entry("c3", 444),
            Map.entry("c1", 492),
            // Magnetismo / Magnet (DJT-098, KG wind+earth)
            Map.entry("iron_sand_wall", 331),
            Map.entry("iron_sand_wave", 332),
            Map.entry("iron_sand_drizzle", 333),
            Map.entry("sand_waterfall_funeral", 334),
            Map.entry("sand_binding_coffin", 335),
            Map.entry("sand_wave", 336),
            // Ebullición / Boil (DJT-097, KG water+fire)
            Map.entry("boiling_mist_dragon", 328),
            Map.entry("meltdown", 428),
            Map.entry("erupting_propulsion_fist", 429),
            Map.entry("skilled_mist", 430),
            // Tormenta / Storm (DJT-096, KG lightning+water)
            Map.entry("thunder_cloud_inner_wave", 325),
            Map.entry("laser_circus", 326),
            Map.entry("weather_manipulation", 327),
            // Lava (DJT-094, KG earth+fire)
            Map.entry("lava_snake_bullet", 322),
            Map.entry("intense_flow", 323),
            Map.entry("raining_debris", 324),
            // Hielo (DJT-093, KG water+wind)
            Map.entry("frozen_waste", 318),
            Map.entry("blizzard", 319),
            Map.entry("ice_rock_dome", 320),
            Map.entry("ice_spear", 321),
            Map.entry("ice_golem", 354),
            // Madera / Mokuton (DJT-092, primer KG)
            Map.entry("wood_dome", 339),
            Map.entry("great_forest", 340),
            Map.entry("great_spear_tree", 341),
            Map.entry("tree_bind_flourishing_burial", 342),
            Map.entry("spontaneous_tree_summoning", 343),
            Map.entry("nativity_of_a_world_of_trees", 344),
            Map.entry("four_pillar_house", 445),
            Map.entry("four_pillar_prison", 446),
            // Taijutsu (DJT-090)
            Map.entry("chakra_enhanced_strength", 485),
            Map.entry("heavenly_foot_of_pain", 486),
            Map.entry("crimson_drizzle", 487),
            Map.entry("cherry_blossom_impact", 488),
            // Yin-Yang (DJT-089) — sword_of_nunoboko usa modelo 3D de arma, sin CMD (textura por defecto)
            Map.entry("truth_seeking_scorched_earth", 461),
            Map.entry("creation_of_all_things", 462),
            Map.entry("six_paths_cross_lantern", 463),
            // Viento (DJT-088)
            Map.entry("propelling_winds", 307),
            Map.entry("wind_instantaneous_body", 316),
            Map.entry("wind_torpedo", 317),
            Map.entry("drilling_air_bullet", 346),
            Map.entry("wind_mode", 400),
            Map.entry("wind_cloak", 438),
            Map.entry("rasengan", 451),
            Map.entry("rasenshuriken", 453),
            Map.entry("big_ball_rasengan", 458),
            // Tierra (DJT-087)
            Map.entry("earth_prison", 306),
            Map.entry("earth_dragon_blast", 308),
            Map.entry("earth_instantaneous_body", 309),
            Map.entry("rock_gun", 352),
            Map.entry("earth_golem", 353),
            Map.entry("tunnelling", 355),
            // Agua (DJT-086)
            Map.entry("water_prison", 310),
            Map.entry("water_instantaneous_body", 311),
            Map.entry("breathing_underwater", 312),
            Map.entry("water_puddle", 313),
            Map.entry("rising_water_slicer", 348),
            Map.entry("water_severing_wave", 349),
            Map.entry("hidden_mist", 483),
            // Otros pilotos ya volcados
            Map.entry("substitution", 484),
            Map.entry("healing", 407),
            Map.entry("earth_wall", 337),
            Map.entry("vacuum_wave", 345),
            // Kenjutsu (tecnicas de espada)
            Map.entry("dancing_blade_risk", 495),
            Map.entry("assassination_technique", 496),
            Map.entry("chakra_shock_slash", 497),
            Map.entry("chakra_shockwave_slash", 498),
            Map.entry("secret_sword_snowstorm_beheading", 499));

    private JutsuModelData() {
    }

    /** {@code CustomModelData} del jutsu, o 0 si no tiene textura portada (usa el modelo por defecto). */
    public static int of(String jutsuId) {
        return MODEL_DATA.getOrDefault(jutsuId, 0);
    }

    /**
     * Desplazamiento del rango de modelos <b>3D en el mundo</b> (F11/DJT-240, decidido en la guia de Blockbench,
     * seccion 6.1). El pergamino del inventario usa el numero autentico del datapack (301-499); su modelo 3D usa
     * {@code 3000 +} ese mismo numero. Asi los dos conviven en el mismo item sin pisarse, y el numero 3D se
     * deduce del del pergamino sin una segunda tabla que mantener.
     */
    public static final int WORLD_MODEL_OFFSET = 3000;

    /**
     * {@code CustomModelData} del modelo 3D en el mundo de un jutsu, o 0 si no tiene numero de pergamino.
     *
     * <p><b>Ojo al usarlo:</b> que exista el numero no significa que exista el modelo. Las overrides de vanilla
     * se resuelven con {@code >=}, asi que un numero sin override propia muestra el modelo de la override
     * inmediatamente inferior -el de otro jutsu-. Por eso el modelo 3D es <b>opt-in</b> por tecnica
     * ({@code displayModel3d} en su JSON) y no se aplica a todas automaticamente.
     */
    /** Un {@code CustomModelData} es de modelo 3D en el mundo (rango 3000+), no de pergamino. */
    public static boolean isWorldModel(int customModelData) {
        return customModelData >= WORLD_MODEL_OFFSET;
    }

    public static int worldModelOf(String jutsuId) {
        int scroll = of(jutsuId);
        return scroll == 0 ? 0 : WORLD_MODEL_OFFSET + scroll;
    }
}
