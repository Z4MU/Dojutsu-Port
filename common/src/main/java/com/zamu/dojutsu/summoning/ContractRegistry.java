package com.zamu.dojutsu.summoning;

import com.zamu.dojutsu.domain.summoning.SummonContractDef;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de contratos de invocacion cargados desde {@code data/dojutsu/contracts/*.json}
 * ({@link ContractReloadListener}). Mismo patron que {@code ClanRegistry}/{@code EyeRegistry}.
 *
 * <p>La <b>especie</b> es la clave inversa que mas se usa: al golpear a un bicho con un contrato en blanco hay
 * que saber si es contratable y en que tier, y eso es una busqueda por tipo de entidad. Se mantiene el indice
 * invertido en la carga en vez de recorrer los 27 en cada golpe.
 */
public final class ContractRegistry {

    private static final Map<String, SummonContractDef> BY_ID = new ConcurrentHashMap<>();
    private static final Map<String, SummonContractDef> BY_ENTITY = new ConcurrentHashMap<>();

    private ContractRegistry() {
    }

    /** Reemplaza el contenido del registro (carga y {@code /reload}). */
    public static void load(Map<String, SummonContractDef> contracts) {
        BY_ID.clear();
        BY_ENTITY.clear();
        BY_ID.putAll(contracts);
        contracts.values().forEach(def -> BY_ENTITY.put(def.entityType(), def));
    }

    /** Contrato por su id ({@code "wolf"}); vacio si no existe. */
    public static Optional<SummonContractDef> get(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** Contrato de una especie ({@code "minecraft:wolf"}); vacio si no es contratable. */
    public static Optional<SummonContractDef> forEntity(String entityType) {
        return Optional.ofNullable(BY_ENTITY.get(entityType));
    }

    /** Todos los contratos cargados. */
    public static Collection<SummonContractDef> all() {
        return BY_ID.values();
    }

    /** Numero de contratos cargados (log de arranque y comando de QA). */
    public static int size() {
        return BY_ID.size();
    }
}
