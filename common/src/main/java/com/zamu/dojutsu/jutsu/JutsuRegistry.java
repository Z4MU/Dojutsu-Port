package com.zamu.dojutsu.jutsu;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Registro de los jutsus cargados desde datos ({@code [S06]}). Lo puebla
 * {@link JutsuReloadListener} en cada carga/{@code /reload}; lo consultaran el pipeline de
 * casteo (DJT-074), el aprendizaje (DJT-075) y el comando de debug.
 *
 * <p>Reemplaza la tabla de despacho de {@code run_jutsu} (141 ramas {@code if score matches N})
 * por datos identificados <b>por nombre</b> — el indice numerico {@code JutsuId} del datapack
 * no se porta (regla 12: ids por nombre, nunca indices). Mismo patron que {@code ClanRegistry}:
 * un pack puede anadir jutsus sin tocar codigo; mapa ordenado por id para listados estables.
 */
public final class JutsuRegistry {

    private static volatile Map<String, JutsuEntry> entries = Map.of();

    private JutsuRegistry() {
    }

    /** Reemplaza las entradas cargadas (lo llama el reload listener). */
    public static void load(Map<String, JutsuEntry> loaded) {
        entries = Collections.unmodifiableMap(new TreeMap<>(loaded));
    }

    /** Entrada de un jutsu por id, o vacio si no existe (id desconocido: no se puede castear). */
    public static Optional<JutsuEntry> get(String id) {
        return Optional.ofNullable(entries.get(id));
    }

    /** Vista de solo lectura de todas las entradas, ordenadas por id. */
    public static Map<String, JutsuEntry> all() {
        return entries;
    }
}
