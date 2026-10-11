package com.zamu.dojutsu.entity;

import java.util.Locale;

/**
 * Estela visual de un {@link NinjaProjectile} (F11/DJT-241): el rastro que deja en vuelo y el estallido al
 * impactar. Es <b>dato observable</b> (regla 13): viaja con la entidad como un byte, y cada cliente pinta las
 * particulas por su cuenta en vez de recibirlas del servidor tick a tick.
 *
 * <p>Este enum solo nombra el estilo; las particulas viven en {@link ProjectileTrailFx}. Asi se puede probar sin
 * arrancar Minecraft (los {@code ParticleTypes} no existen fuera del juego).
 */
public enum ProjectileTrail {

    /** Sin estela: el comportamiento de siempre (kunai, bolas de las bestias, el resto de proyectiles). */
    NONE,

    /**
     * Fuego. Los colores salen de {@code fire_bullet/run.mcfunction}, que cada tick soltaba polvo
     * {@code 1 0.4 0.08} (tamano 1) y {@code 1 0.6 0.2} (tamano 0.5) sobre el armor stand.
     */
    FIRE;

    /** El estilo por su nombre en el JSON del jutsu ({@code "trail": "fire"}); desconocido o vacio = sin estela. */
    public static ProjectileTrail byName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }
        for (ProjectileTrail trail : values()) {
            if (trail.name().equals(name.trim().toUpperCase(Locale.ROOT))) {
                return trail;
            }
        }
        return NONE;
    }

    /** Identificador de red (el byte sincronizado). */
    public byte id() {
        return (byte) ordinal();
    }

    /** El estilo de un byte recibido; uno fuera de rango (version distinta) cae a {@link #NONE}. */
    public static ProjectileTrail fromId(byte id) {
        ProjectileTrail[] all = values();
        return id >= 0 && id < all.length ? all[id] : NONE;
    }
}
