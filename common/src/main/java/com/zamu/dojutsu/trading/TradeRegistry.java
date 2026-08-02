package com.zamu.dojutsu.trading;

import com.zamu.dojutsu.domain.trading.TraderCatalogue;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de catalogos de comerciante cargados desde {@code data/dojutsu/trades/*.json}
 * ({@link TradeReloadListener}). Mismo patron que {@code ContractRegistry}.
 */
public final class TradeRegistry {

    private static final Map<TraderCatalogue.Kind, TraderCatalogue> CATALOGUES =
            new EnumMap<>(TraderCatalogue.Kind.class);

    private TradeRegistry() {
    }

    /** Reemplaza el contenido del registro (carga y {@code /reload}). */
    public static synchronized void load(List<TraderCatalogue> catalogues) {
        CATALOGUES.clear();
        catalogues.forEach(catalogue -> CATALOGUES.put(catalogue.kind(), catalogue));
    }

    /** Catalogo de un comerciante, o vacio si no hay ninguno cargado para el. */
    public static synchronized Optional<TraderCatalogue> get(TraderCatalogue.Kind kind) {
        return Optional.ofNullable(CATALOGUES.get(kind));
    }

    /** Numero de catalogos cargados (log de arranque y comando de QA). */
    public static synchronized int size() {
        return CATALOGUES.size();
    }
}
