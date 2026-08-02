package com.zamu.dojutsu.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

/** Tests de la fisica pura del proyectil ninja (F4/DJT-070). */
class ProjectileMathTest {

    private static final double EPS = 1.0e-9;

    // ------------------------------------------------------------------
    // advance
    // ------------------------------------------------------------------

    @Test
    void advanceSumaLaVelocidadALaPosicion() {
        Vec3 next = ProjectileMath.advance(new Vec3(10, 64, -5), new Vec3(1.5, -0.2, 3));
        assertEquals(11.5, next.x, EPS);
        assertEquals(63.8, next.y, EPS);
        assertEquals(-2.0, next.z, EPS);
    }

    @Test
    void advanceConVelocidadCeroNoSeMueve() {
        Vec3 pos = new Vec3(1, 2, 3);
        Vec3 next = ProjectileMath.advance(pos, Vec3.ZERO);
        assertEquals(pos.x, next.x, EPS);
        assertEquals(pos.y, next.y, EPS);
        assertEquals(pos.z, next.z, EPS);
    }

    // ------------------------------------------------------------------
    // applyGravity
    // ------------------------------------------------------------------

    @Test
    void laGravedadRestaSoloEnY() {
        Vec3 next = ProjectileMath.applyGravity(new Vec3(0.5, 0.1, -0.5), 0.03);
        assertEquals(0.5, next.x, EPS);
        assertEquals(0.07, next.y, EPS);
        assertEquals(-0.5, next.z, EPS);
    }

    @Test
    void gravedadCeroDevuelveElMismoVector() {
        Vec3 velocity = new Vec3(1, 1, 1);
        assertSame(velocity, ProjectileMath.applyGravity(velocity, 0.0));
    }

    @Test
    void laGravedadSeAcumulaTickATick() {
        Vec3 velocity = new Vec3(0, 0, 0);
        velocity = ProjectileMath.applyGravity(velocity, 0.05);
        velocity = ProjectileMath.applyGravity(velocity, 0.05);
        velocity = ProjectileMath.applyGravity(velocity, 0.05);
        assertEquals(-0.15, velocity.y, EPS);
    }

    @Test
    void gravedadNegativaEsRechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> ProjectileMath.applyGravity(Vec3.ZERO, -0.01));
    }

    @Test
    void gravedadNoFinitaEsRechazada() {
        assertThrows(IllegalArgumentException.class,
                () -> ProjectileMath.applyGravity(Vec3.ZERO, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> ProjectileMath.applyGravity(Vec3.ZERO, Double.POSITIVE_INFINITY));
    }

    // ------------------------------------------------------------------
    // isExpired
    // ------------------------------------------------------------------

    @Test
    void noExpiraAntesDeLaVidaMaxima() {
        assertFalse(ProjectileMath.isExpired(0, 100));
        assertFalse(ProjectileMath.isExpired(99, 100));
    }

    @Test
    void expiraJustoEnLaVidaMaximaYDespues() {
        assertTrue(ProjectileMath.isExpired(100, 100));
        assertTrue(ProjectileMath.isExpired(101, 100));
    }

    @Test
    void vidaMaximaNoPositivaEsRechazada() {
        assertThrows(IllegalArgumentException.class, () -> ProjectileMath.isExpired(0, 0));
        assertThrows(IllegalArgumentException.class, () -> ProjectileMath.isExpired(0, -5));
    }
}
