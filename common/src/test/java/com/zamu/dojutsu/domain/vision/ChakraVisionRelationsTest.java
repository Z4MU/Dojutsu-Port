package com.zamu.dojutsu.domain.vision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la clasificacion de entidades para la chakra vision (DJT-125): fija la traduccion de los cuatro
 * {@code tags/entity_types/chakra_highlights/*.json} del datapack, para que un cambio de categoria sea explicito.
 */
class ChakraVisionRelationsTest {

    @Test
    void clasificaCadaCategoriaComoElDatapack() {
        assertEquals(EntityRelationship.SPECIAL, ChakraVisionRelations.of("minecraft:villager").orElseThrow());
        assertEquals(EntityRelationship.SPECIAL, ChakraVisionRelations.of("minecraft:iron_golem").orElseThrow());
        assertEquals(EntityRelationship.FRIENDLY, ChakraVisionRelations.of("minecraft:cow").orElseThrow());
        assertEquals(EntityRelationship.NEUTRAL, ChakraVisionRelations.of("minecraft:enderman").orElseThrow());
        assertEquals(EntityRelationship.HOSTILE, ChakraVisionRelations.of("minecraft:zombie").orElseThrow());
        assertEquals(EntityRelationship.HOSTILE, ChakraVisionRelations.of("minecraft:creeper").orElseThrow());
    }

    @Test
    void unTipoNoEtiquetadoNoSeResalta() {
        // el datapack solo resaltaba lo que estaba en alguna etiqueta: jugadores, aldeas nuevas, etc. no
        assertTrue(ChakraVisionRelations.of("minecraft:player").isEmpty());
        assertTrue(ChakraVisionRelations.of("minecraft:armor_stand").isEmpty());
        assertTrue(ChakraVisionRelations.of("minecraft:allay").isEmpty());
        assertTrue(ChakraVisionRelations.of("dojutsu:ninja_projectile").isEmpty());
    }

    @Test
    void cadaRelacionTieneNombreDeEquipoYColor() {
        for (EntityRelationship relationship : EntityRelationship.values()) {
            assertEquals("dojutsu.cv." + relationship.name().toLowerCase(java.util.Locale.ROOT),
                    relationship.teamName());
            assertTrue(relationship.rgb() > 0);
            assertTrue(!relationship.teamColorName().isBlank());
        }
    }
}
