package com.zamu.dojutsu.dojutsu;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registro de las definiciones de ojo ({@link EyeDefinition}) cargadas desde datos {@code [S05]}. Lo puebla
 * {@link EyeReloadListener} en cada carga/{@code /reload}; lo consultaran los servicios de despertar/evolucion
 * (DJT-114/115), el strain (DJT-116) y el menu ocular (DJT-117).
 *
 * <p>Mismo patron que {@link ClanRegistry}/{@code JutsuRegistry}: el ojo <b>no es un enum</b>, es contenido
 * identificado por id (un pack puede anadir/variar ojos sin tocar codigo). Se ordena por id (listado estable).
 */
public final class EyeRegistry {

    private static volatile Map<String, EyeDefinition> definitions = Map.of();

    private EyeRegistry() {
    }

    /** Reemplaza las definiciones cargadas (lo llama el reload listener). */
    public static void load(Map<String, EyeDefinition> loaded) {
        definitions = Collections.unmodifiableMap(new TreeMap<>(loaded));
    }

    /** Definicion de un ojo por id, o vacio si no existe. */
    public static Optional<EyeDefinition> get(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    /** Vista de solo lectura de los ojos cargados, ordenados por id. */
    public static Map<String, EyeDefinition> all() {
        return definitions;
    }
}
