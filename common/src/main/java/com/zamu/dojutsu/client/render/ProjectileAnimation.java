package com.zamu.dojutsu.client.render;

/**
 * Animacion de los modelos 3D de jutsu en vuelo (F11/DJT-241). <b>Matematica pura</b>, como
 * {@link ProjectileOrientation}, para probarla sin arrancar Minecraft.
 *
 * <p>Dos movimientos, los dos en funcion de la edad en ticks (con la fraccion del frame, para que sean suaves a
 * cualquier tasa de FPS):
 * <ul>
 *   <li><b>Giro</b> sobre el eje de vuelo: la estela y las chispas dan vueltas alrededor de la bola.</li>
 *   <li><b>Latido</b>: la bola crece y encoge un poco, como una llama que respira.</li>
 * </ul>
 */
public final class ProjectileAnimation {

    /** Grados de giro por tick: media vuelta por segundo. */
    static final float SPIN_DEGREES_PER_TICK = 9.0F;

    /** Cuanto crece y encoge la bola (6 % arriba y abajo). Mas, y se lee como un globo. */
    static final float PULSE_AMPLITUDE = 0.06F;

    /** Velocidad del latido, en radianes por tick: unos tres latidos por segundo. */
    static final float PULSE_RATE = 0.9F;

    /** Edad a la que el modelo empieza a crecer: antes esta en la cara de quien lo lanza. */
    static final float GROW_START_TICKS = 2.0F;

    /** Ticks que tarda en alcanzar su tamano real. */
    static final float GROW_TICKS = 4.0F;

    private ProjectileAnimation() {
    }

    /** Giro sobre el eje de vuelo, en grados dentro de [0, 360). */
    public static float rollDegrees(float ageTicks) {
        float roll = (ageTicks * SPIN_DEGREES_PER_TICK) % 360.0F;
        return roll < 0.0F ? roll + 360.0F : roll;
    }

    /**
     * Escala de aparicion, de 0 a 1: el jutsu se forma en vez de aparecer de golpe (F11/DJT-241).
     *
     * <p>No es solo estetica. La estela del modelo se alarga casi dos bloques por detras de la bola; a tamano real
     * desde el primer tick, envolvia la camara de quien lanzaba y le pintaba la pantalla entera de amarillo. Al
     * crecer mientras se aleja, la cola nunca llega a la camara.
     */
    public static float spawnScale(float ageTicks) {
        float t = (ageTicks - GROW_START_TICKS) / GROW_TICKS;
        if (t <= 0.0F) {
            return 0.0F;
        }
        if (t >= 1.0F) {
            return 1.0F;
        }
        return t * t * (3.0F - 2.0F * t); // smoothstep: arranca y llega sin tirones
    }

    /** Escala del latido, alrededor de 1. */
    public static float pulseScale(float ageTicks) {
        return 1.0F + PULSE_AMPLITUDE * (float) Math.sin(ageTicks * PULSE_RATE);
    }
}
