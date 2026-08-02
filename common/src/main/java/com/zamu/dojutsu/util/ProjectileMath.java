package com.zamu.dojutsu.util;

import java.util.Objects;
import net.minecraft.world.phys.Vec3;

/**
 * Fisica pura del proyectil ninja (F4/DJT-070): avance de posicion, gravedad opcional y
 * expiracion por tiempo de vida. Sin estado y sin dependencias de nivel/entidad, para que la
 * fisica sea verificable con tests unitarios (mismo criterio que el dominio de F1); el glue
 * (raycast, spawn, discard) vive en {@link com.zamu.dojutsu.entity.NinjaProjectile}.
 *
 * <p>Convenciones: la velocidad es un desplazamiento por tick; la gravedad es una resta por
 * tick sobre el eje Y (0 = vuelo rectilineo, el caso tipico de los jutsu-proyectil del
 * datapack, que teletransportaban armor stands en linea recta).
 */
public final class ProjectileMath {

    private ProjectileMath() {
    }

    /** Posicion del siguiente tick: la actual mas el desplazamiento de este tick. */
    public static Vec3 advance(Vec3 position, Vec3 velocity) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(velocity, "velocity");
        return position.add(velocity);
    }

    /**
     * Velocidad del siguiente tick: aplica la gravedad como resta sobre Y. Con gravedad 0
     * devuelve el mismo vector (vuelo rectilineo, sin objeto nuevo por tick).
     *
     * @param gravityPerTick resta por tick sobre Y; debe ser {@code >= 0} y finita
     */
    public static Vec3 applyGravity(Vec3 velocity, double gravityPerTick) {
        Objects.requireNonNull(velocity, "velocity");
        requireValidGravity(gravityPerTick);
        if (gravityPerTick == 0.0D) {
            return velocity;
        }
        return new Vec3(velocity.x, velocity.y - gravityPerTick, velocity.z);
    }

    /**
     * Indica si el proyectil agoto su vida maxima. La frontera es inclusiva: con vida N, el
     * tick N ya expira (N ticks de vuelo: 0..N-1).
     *
     * @param maxLifetimeTicks vida maxima en ticks; debe ser {@code > 0}
     */
    public static boolean isExpired(int ageTicks, int maxLifetimeTicks) {
        requireValidLifetime(maxLifetimeTicks);
        return ageTicks >= maxLifetimeTicks;
    }

    /** Valida una gravedad configurable: finita y no negativa. */
    public static void requireValidGravity(double gravityPerTick) {
        if (!Double.isFinite(gravityPerTick) || gravityPerTick < 0.0D) {
            throw new IllegalArgumentException("gravedad invalida (debe ser >= 0 y finita): " + gravityPerTick);
        }
    }

    /** Valida una vida maxima configurable: estrictamente positiva. */
    public static void requireValidLifetime(int maxLifetimeTicks) {
        if (maxLifetimeTicks <= 0) {
            throw new IllegalArgumentException("vida maxima invalida (debe ser > 0): " + maxLifetimeTicks);
        }
    }
}
