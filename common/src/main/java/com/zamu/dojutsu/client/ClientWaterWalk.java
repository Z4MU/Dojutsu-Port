package com.zamu.dojutsu.client;

/**
 * Cache <b>de solo lectura</b> de si el jugador local camina sobre el agua (F9/DJT-205). La alimenta el receptor
 * de {@code WaterWalkPayload} (S2C); la consume {@code WaterWalkClient} en el tick de cliente.
 *
 * <p>Mismas reglas que {@link ClientEyeMenu}: solo presentacion —el servidor sigue siendo quien decide si te
 * sostienes—, no se persiste y se limpia al salir del mundo. <b>Sin imports de MC-cliente</b>: inocua aunque se
 * cargue en un servidor dedicado.
 */
public final class ClientWaterWalk {

    private static boolean active;

    private ClientWaterWalk() {
    }

    /** Anota el estado observado (lo llama el receptor del payload). */
    public static void set(boolean value) {
        active = value;
    }

    /** {@code true} si el jugador local se esta sosteniendo sobre el agua. */
    public static boolean active() {
        return active;
    }

    /** Limpia el estado (al cambiar de mundo / desconectar). */
    public static void clear() {
        active = false;
    }
}
