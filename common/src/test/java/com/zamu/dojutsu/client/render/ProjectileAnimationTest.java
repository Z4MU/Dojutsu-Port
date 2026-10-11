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

    @Test
    void alSalirNoSeVeYLuegoLlegaATamanoReal() {
        assertEquals(0.0F, ProjectileAnimation.spawnScale(0.0F), EPS);
        assertEquals(0.0F, ProjectileAnimation.spawnScale(ProjectileAnimation.GROW_START_TICKS), EPS);
        assertEquals(1.0F, ProjectileAnimation.spawnScale(
                ProjectileAnimation.GROW_START_TICKS + ProjectileAnimation.GROW_TICKS), EPS);
        assertEquals(1.0F, ProjectileAnimation.spawnScale(500.0F), EPS);
    }

    @Test
    void creceSinEncogerNunca() {
        float previous = 0.0F;
        for (float age = 0.0F; age < 12.0F; age += 0.05F) {
            float scale = ProjectileAnimation.spawnScale(age);
            assertTrue(scale >= previous - EPS, "encoge a la edad " + age);
            previous = scale;
        }
    }

    @Test
    void laEstelaNuncaLlegaALaCamaraDeQuienLanza() {
        // El fallo del segundo video de prueba: la pantalla entera amarilla al lanzar. La estela de la Bala de fuego
        // se alarga 1.8 bloques por detras de la bola (18 px x 1.6 / 16); a 1 bloque por tick, la bola esta a
        // "edad" bloques de la camara. La cola debe quedarse siempre por delante, con margen.
        float tailBlocks = 1.8F;
        float speed = 1.0F;
        for (float age = 0.0F; age < 20.0F; age += 0.05F) {
            float tailEnd = age * speed - tailBlocks * ProjectileAnimation.spawnScale(age);
            assertTrue(tailEnd > 0.3F || ProjectileAnimation.spawnScale(age) == 0.0F,
                    "a la edad " + age + " la cola llega a " + tailEnd + " bloques de la camara");
        }
    }
}
