package com.zamu.dojutsu.client.render;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Orientacion de un modelo 3D de jutsu segun la direccion de vuelo (F11/DJT-240). <b>Matematica pura</b>,
 * separada del renderer para poder probarla sin arrancar Minecraft.
 *
 * <p>El convenio del modelo es que su <b>nariz mira a +Z</b>. Vanilla guarda la direccion de un proyectil en
 * sus angulos ({@code Projectile.updateRotation}): {@code yRot = atan2(vx, vz)} y {@code xRot = atan2(vy, horiz)},
 * con {@code xRot} <b>positivo hacia arriba</b> -al reves que en una entidad viva, donde positivo es mirar abajo-.
 * Esta clase convierte esos dos angulos en la rotacion que lleva +Z del modelo hasta la velocidad.
 */
public final class ProjectileOrientation {

    private ProjectileOrientation() {
    }

    /**
     * Rotacion que apunta la nariz (+Z) del modelo en la direccion de vuelo.
     *
     * <p>Primero se inclina en el eje X local (cabeceo) y despues se gira en Y (rumbo). El signo del cabeceo va
     * invertido porque rotar +Z alrededor de X con un angulo positivo lo baja, y el {@code xRot} de un
     * proyectil es positivo cuando sube.
     *
     * @param yawDegrees   {@code yRot} del proyectil
     * @param pitchDegrees {@code xRot} del proyectil (positivo = subiendo)
     */
    public static Quaternionf noseAlongFlight(float yawDegrees, float pitchDegrees) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(yawDegrees))
                .rotateX((float) Math.toRadians(-pitchDegrees));
    }

    /** Hacia donde apunta la nariz con esa orientacion. Visible para tests. */
    static Vector3f nose(float yawDegrees, float pitchDegrees) {
        return noseAlongFlight(yawDegrees, pitchDegrees).transform(new Vector3f(0.0F, 0.0F, 1.0F));
    }
}
