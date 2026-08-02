package com.zamu.dojutsu.jutsu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.jutsu.LastCastRegistry.CastRecord;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/**
 * Tests de la lógica pura de candidatura a copia del Sharingan (F4/DJT-079): expiración,
 * proximidad, dimensión y exclusión del propio observador. Sin bootstrap de Minecraft
 * ({@link Vec3}/{@link ResourceKey} no lo necesitan).
 */
class LastCastRecordTest {

    private static final UUID CASTER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OBSERVER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final ResourceKey<Level> OVERWORLD =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("overworld"));
    private static final ResourceKey<Level> NETHER =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace("the_nether"));

    private static CastRecord record(Vec3 pos, long expiresAt) {
        return new CastRecord("great_fireball", CASTER, OVERWORLD, pos, expiresAt);
    }

    @Test
    void vivoAntesDeExpirarYExpiradoDespues() {
        CastRecord rec = record(Vec3.ZERO, 1000L);
        assertTrue(rec.isLiveAt(999L));
        assertFalse(rec.isLiveAt(1000L)); // frontera: al tick de expiración ya no es copiable
        assertFalse(rec.isLiveAt(1001L));
    }

    @Test
    void candidatoSiVivoEnDimensionYDentroDelRadio() {
        CastRecord rec = record(new Vec3(0, 0, 0), 1000L);
        // Observador a 10 bloques, radio 20: candidato.
        assertTrue(rec.isCandidateFor(OVERWORLD, new Vec3(10, 0, 0), 20.0D, 500L, OBSERVER));
    }

    @Test
    void noCandidatoFueraDelRadio() {
        CastRecord rec = record(new Vec3(0, 0, 0), 1000L);
        // Observador a 25 bloques, radio 20: fuera.
        assertFalse(rec.isCandidateFor(OVERWORLD, new Vec3(25, 0, 0), 20.0D, 500L, OBSERVER));
    }

    @Test
    void noCandidatoEnOtraDimension() {
        CastRecord rec = record(new Vec3(0, 0, 0), 1000L);
        assertFalse(rec.isCandidateFor(NETHER, new Vec3(1, 0, 0), 20.0D, 500L, OBSERVER));
    }

    @Test
    void noCandidatoSiExpiro() {
        CastRecord rec = record(new Vec3(0, 0, 0), 1000L);
        assertFalse(rec.isCandidateFor(OVERWORLD, new Vec3(1, 0, 0), 20.0D, 1500L, OBSERVER));
    }

    @Test
    void noTeCopiasATiMismo() {
        CastRecord rec = record(new Vec3(0, 0, 0), 1000L);
        assertFalse(rec.isCandidateFor(OVERWORLD, new Vec3(1, 0, 0), 20.0D, 500L, CASTER));
    }
}
