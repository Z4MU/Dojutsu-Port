package com.zamu.dojutsu.natures;

import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de las definiciones de Kekkei Genkai ({@link KekkeiGenkaiDefinition}) cargadas desde datos
 * ({@code [S03]}). Lo puebla {@link KekkeiGenkaiReloadListener} en cada carga/{@code /reload}; lo consulta
 * {@link KekkeiGenkaiService}.
 *
 * <p>Reemplaza las 18 funciones {@code natures/give/kg/<kg>} (copy-paste) por una tabla de datos. Mismo patron
 * que {@code UnlockRegistry} (F2).
 */
public final class KekkeiGenkaiRegistry {

    private static volatile Map<KekkeiGenkai, KekkeiGenkaiDefinition> definitions = Map.of();

    private KekkeiGenkaiRegistry() {
    }

    /** Reemplaza las definiciones cargadas (lo llama el reload listener). */
    public static void load(Map<KekkeiGenkai, KekkeiGenkaiDefinition> loaded) {
        EnumMap<KekkeiGenkai, KekkeiGenkaiDefinition> copy = new EnumMap<>(KekkeiGenkai.class);
        copy.putAll(loaded);
        definitions = Collections.unmodifiableMap(copy);
    }

    /** Definicion de un KG, o vacio si el datapack no la trae (KG sin definir: no se puede otorgar). */
    public static Optional<KekkeiGenkaiDefinition> get(KekkeiGenkai kg) {
        return Optional.ofNullable(definitions.get(kg));
    }

    /** Vista de solo lectura de las definiciones cargadas (inspeccion/tests/comando de debug). */
    public static Map<KekkeiGenkai, KekkeiGenkaiDefinition> all() {
        return definitions;
    }
}
