package com.zamu.dojutsu.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Tests de la orientacion de los modelos 3D en vuelo (F11/DJT-240).
 *
 * <p>Es la clase de matematica que se equivoca <b>en silencio</b>: un signo cambiado no da error, da una Bala de
 * fuego que vuela de lado o con la nariz hacia quien la lanza -que es justo el fallo que se vio en la primera
 * prueba en partida-. Por eso se comprueba contra los angulos <b>tal como los calcula vanilla</b>, no contra
 * numeros elegidos a mano.
 */
class ProjectileOrientationTest {

    private static final float EPS = 1.0e-4F;

    /** Los angulos que fija {@code Projectile.updateRotation} para una velocidad dada. */
    private static float[] vanillaAngles(double vx, double vy, double vz) {
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        float yaw = (float) Math.toDegrees(Math.atan2(vx, vz));
        float pitch = (float) Math.toDegrees(Math.atan2(vy, horizontal));
        return new float[] {yaw, pitch};
    }

    private static void assertNosePointsAlong(double vx, double vy, double vz) {
        float[] angles = vanillaAngles(vx, vy, vz);
        Vector3f nose = ProjectileOrientation.nose(angles[0], angles[1]);
        Vector3f expected = new Vector3f((float) vx, (float) vy, (float) vz).normalize();
        assertEquals(expected.x, nose.x, EPS, "x para v=(" + vx + "," + vy + "," + vz + ")");
        assertEquals(expected.y, nose.y, EPS, "y para v=(" + vx + "," + vy + "," + vz + ")");
        assertEquals(expected.z, nose.z, EPS, "z para v=(" + vx + "," + vy + "," + vz + ")");
    }

    @Test
    void sinGiroLaNarizMiraAlSur() {
        // el convenio del modelo: nariz hacia +Z
        Vector3f nose = ProjectileOrientation.nose(0.0F, 0.0F);
        assertEquals(0.0F, nose.x, EPS);
        assertEquals(0.0F, nose.y, EPS);
        assertEquals(1.0F, nose.z, EPS);
    }

    @Test
    void laNarizSigueALosCuatroPuntosCardinales() {
        assertNosePointsAlong(0, 0, 1);   // sur
        assertNosePointsAlong(0, 0, -1);  // norte: el caso del error, con la nariz hacia atras
        assertNosePointsAlong(1, 0, 0);   // este
        assertNosePointsAlong(-1, 0, 0);  // oeste
    }

    @Test
    void laNarizSubeCuandoElProyectilSube() {
        // ojo: en un proyectil el xRot positivo es SUBIR, al reves que en una entidad viva
        assertNosePointsAlong(0, 1, 1);
        assertNosePointsAlong(1, 2, 0);
    }

    @Test
    void laNarizBajaCuandoElProyectilCae() {
        // el arco de un proyectil con gravedad
        assertNosePointsAlong(0, -1, 1);
        assertNosePointsAlong(-2, -1, 3);
    }

    @Test
    void cualquierDireccionDeDisparo() {
        assertNosePointsAlong(0.3, 0.1, -0.9);
        assertNosePointsAlong(-0.7, 0.4, 0.2);
        assertNosePointsAlong(0.05, -0.6, -0.4);
    }
}
