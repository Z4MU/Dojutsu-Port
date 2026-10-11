package com.zamu.dojutsu.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests de la animacion de los modelos 3D en vuelo (F11/DJT-241). */
class ProjectileAnimationTest {

    private static final float EPS = 1.0e-4F;

    @Test
    void elGiroEmpiezaEnCeroYAvanzaConLaEdad() {
        assertEquals(0.0F, ProjectileAnimation.rollDegrees(0.0F), EPS);
        assertEquals(ProjectileAnimation.SPIN_DEGREES_PER_TICK, ProjectileAnimation.rollDegrees(1.0F), EPS);
    }

    @Test
    void elGiroSiempreQuedaEnUnaVuelta() {
        // un proyectil que vive mucho no debe acumular angulos enormes (perderian precision en float)
        for (float age = 0.0F; age < 2000.0F; age += 7.3F) {
            float roll = ProjectileAnimation.rollDegrees(age);
            assertTrue(roll >= 0.0F && roll < 360.0F, "giro fuera de [0, 360): " + roll);
        }
    }

    @Test
    void elGiroEsSuaveEntreFrames() {
        // la fraccion del frame cuenta: medio tick es medio paso, no un salto
        assertEquals(ProjectileAnimation.SPIN_DEGREES_PER_TICK / 2.0F, ProjectileAnimation.rollDegrees(0.5F), EPS);
    }

    @Test
    void elLatidoSeQuedaDentroDeSuAmplitud() {
        for (float age = 0.0F; age < 500.0F; age += 0.37F) {
            float scale = ProjectileAnimation.pulseScale(age);
            assertTrue(scale >= 1.0F - ProjectileAnimation.PULSE_AMPLITUDE - EPS
                    && scale <= 1.0F + ProjectileAnimation.PULSE_AMPLITUDE + EPS, "latido fuera de rango: " + scale);
        }
    }

    @Test
    void elLatidoArrancaATamanoReal() {
        // el primer frame visible no debe dar un salto de tamano
        assertEquals(1.0F, ProjectileAnimation.pulseScale(0.0F), EPS);
    }
}
