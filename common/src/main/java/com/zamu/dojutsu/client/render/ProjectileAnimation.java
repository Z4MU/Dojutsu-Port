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

    private ProjectileAnimation() {
    }

    /** Giro sobre el eje de vuelo, en grados dentro de [0, 360). */
    public static float rollDegrees(float ageTicks) {
        float roll = (ageTicks * SPIN_DEGREES_PER_TICK) % 360.0F;
        return roll < 0.0F ? roll + 360.0F : roll;
    }

    /** Escala del latido, alrededor de 1. */
    public static float pulseScale(float ageTicks) {
        return 1.0F + PULSE_AMPLITUDE * (float) Math.sin(ageTicks * PULSE_RATE);
    }
}
