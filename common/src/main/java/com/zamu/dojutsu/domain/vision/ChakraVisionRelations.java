package com.zamu.dojutsu.domain.vision;

import java.util.Map;
import java.util.Optional;

/**
 * Clasificacion <b>estatica</b> de entidades por relacion para la chakra vision {@code [S25]} (DJT-125), por id
 * de tipo de entidad vanilla. Es la traduccion directa de los cuatro
 * {@code tags/entity_types/chakra_highlights/*.json} del datapack, a codigo puro y testeable.
 *
 * <p><b>Clasificacion estatica, no selector por tick (RB-3):</b> el datapack corria
 * {@code execute at @e[type=#tag] run particle} cada tick para cada categoria; aqui la pertenencia es una tabla
 * fija que el servicio consulta al refrescar el glow, sin barrer selectores cada tick.
 *
 * <p><b>Solo se resaltan los tipos listados</b> (los que el datapack etiquetaba): un tipo desconocido devuelve
 * vacio y no se colorea. Fiel: el datapack tampoco resaltaba lo que no estaba en ninguna etiqueta.
 */
public final class ChakraVisionRelations {

    private static final Map<String, EntityRelationship> BY_TYPE = buildTable();

    private ChakraVisionRelations() {
    }

    /**
     * Relacion de un tipo de entidad ({@code "minecraft:zombie"}), o vacio si el datapack no lo resaltaba.
     *
     * @param entityTypeId id del tipo ({@code EntityType.getKey(...).toString()})
     */
    public static Optional<EntityRelationship> of(String entityTypeId) {
        return Optional.ofNullable(BY_TYPE.get(entityTypeId));
    }

    private static Map<String, EntityRelationship> buildTable() {
        var map = new java.util.HashMap<String, EntityRelationship>();
        // special (chakra_highlights/special.json): aliados utiles
        put(map, EntityRelationship.SPECIAL, "villager", "wolf", "cat", "ocelot", "fox", "donkey", "horse",
                "mule", "skeleton_horse", "snow_golem", "iron_golem", "wandering_trader", "piglin");
        // friendly (friendly.json): pasivos inofensivos
        put(map, EntityRelationship.FRIENDLY, "chicken", "cow", "mooshroom", "parrot", "pig", "sheep", "strider",
                "turtle");
        // neutral (neutral.json): atacan solo si se les provoca
        put(map, EntityRelationship.NEUTRAL, "cave_spider", "dolphin", "enderman", "llama", "panda",
                "polar_bear", "spider", "zombified_piglin");
        // hostile (hostile.json)
        put(map, EntityRelationship.HOSTILE, "blaze", "creeper", "drowned", "elder_guardian", "evoker", "ghast",
                "guardian", "hoglin", "husk", "phantom", "piglin_brute", "pillager", "ravager", "shulker",
                "skeleton", "stray", "vex", "vindicator", "witch", "wither_skeleton", "zoglin", "zombie",
                "zombie_villager");
        return Map.copyOf(map);
    }

    private static void put(Map<String, EntityRelationship> map, EntityRelationship relationship, String... names) {
        for (String name : names) {
            map.put("minecraft:" + name, relationship);
        }
    }
}
