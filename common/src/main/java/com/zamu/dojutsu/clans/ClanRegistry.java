package com.zamu.dojutsu.clans;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registro de las definiciones de clan ({@link ClanDefinition}) cargadas desde datos ({@code [S04]}). Lo
 * puebla {@link ClanReloadListener} en cada carga/{@code /reload}; lo consultan {@code ClanService} (DJT-057)
 * y el onboarding (DJT-059/060, que lista los clanes elegibles).
 *
 * <p>A diferencia de los KG, el clan <b>no es un enum</b>: es contenido puro identificado por id, asi que un
 * datapack puede anadir clanes nuevos sin tocar codigo. Mismo patron que {@code KekkeiGenkaiRegistry}.
 */
public final class ClanRegistry {

    private static volatile Map<String, ClanDefinition> definitions = Map.of();

    private ClanRegistry() {
    }

    /** Reemplaza las definiciones cargadas (lo llama el reload listener). Se ordenan por id (listado estable). */
    public static void load(Map<String, ClanDefinition> loaded) {
        definitions = Collections.unmodifiableMap(new TreeMap<>(loaded));
    }

    /** Definicion de un clan por id, o vacio si no existe. */
    public static Optional<ClanDefinition> get(String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    /** Vista de solo lectura de los clanes cargados, ordenados por id. */
    public static Map<String, ClanDefinition> all() {
        return definitions;
    }
}
