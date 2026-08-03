package com.zamu.dojutsu.api;

import com.zamu.dojutsu.clans.ClanDefinition;
import com.zamu.dojutsu.dojutsu.EyeDefinition;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import com.zamu.dojutsu.domain.summoning.SummonContractDef;
import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import java.util.Map;
import java.util.Objects;

/**
 * <b>Aportar contenido de datos</b> desde otro mod (F10/DJT-220).
 *
 * <h2>Cuándo se disparan</h2>
 * Cada evento salta <b>durante la recarga de datos</b> —al cargar el mundo y en cada {@code /reload}—, justo
 * después de leer los JSON del mod y <b>antes</b> de publicar el registro. Eso importa: el contenido de un
 * tercero se recarga con el resto, no se queda pegado de un arranque anterior.
 *
 * <h2>Quién gana si dos declaran el mismo id</h2>
 * El último en escribir. Los eventos corren después de los archivos del mod, así que un tercero puede
 * <b>sustituir</b> contenido base a propósito (por ejemplo, rebalancear un jutsu) escribiendo su id. Es una
 * capacidad, no un accidente — y por eso el registro deja constancia en el log de cuántas entradas aportó cada
 * evento.
 *
 * <h2>Ejemplo</h2>
 * <pre>{@code
 * DojutsuRegistryEvents.JUTSU.register(sink ->
 *         sink.add("mimod_llamarada", new JutsuEntry(...)));
 * }</pre>
 *
 * <p>Las familias de efecto, las habilidades oculares y las técnicas de bestia <b>no</b> van por aquí: son
 * código, no datos, y se registran una vez en el arranque con {@code DojutsuApi.register*}.
 */
public final class DojutsuRegistryEvents {

    /**
     * Donde un tercero deja sus entradas. Es el mapa que se va a publicar, con una puerta delante: se valida el
     * id y se avisa si pisa a alguien.
     *
     * @param <T> tipo de la definición
     */
    @FunctionalInterface
    public interface Sink<T> {

        /**
         * Añade (o sustituye) una entrada.
         *
         * @param id    id del contenido; es la clave con la que lo buscará todo lo demás
         * @param value definición
         */
        void add(String id, T value);
    }

    /** Lo que implementa quien quiera aportar contenido de un tipo. */
    @FunctionalInterface
    public interface Contributor<T> {

        /** Invocado en cada recarga de datos. */
        void collect(Sink<T> sink);
    }

    /** Técnicas ({@code data/dojutsu/jutsu/**}). */
    public static final Event<Contributor<JutsuEntry>> JUTSU = create();

    /** Ojos y su grafo de evolución ({@code data/dojutsu/eyes/*.json}). */
    public static final Event<Contributor<EyeDefinition>> EYE = create();

    /** Clanes ({@code data/dojutsu/clans/*.json}). */
    public static final Event<Contributor<ClanDefinition>> CLAN = create();

    /** Contratos de invocación ({@code data/dojutsu/contracts/*.json}). */
    public static final Event<Contributor<SummonContractDef>> CONTRACT = create();

    private DojutsuRegistryEvents() {
    }

    /**
     * Crea un evento de aportación. Es visible al paquete <b>para los tests</b>: los eventos de Architectury no
     * se pueden desuscribir, así que un test que registre un aportador sobre {@link #JUTSU} contaminaría a los
     * demás. Con esto cada test trabaja sobre el suyo y prueba la misma mecánica.
     */
    static <T> Event<Contributor<T>> create() {
        return EventFactory.createLoop();
    }

    /**
     * Dispara un evento sobre el mapa que está a punto de publicarse. <b>Uso interno</b> (lo llaman los reload
     * listeners); un tercero se suscribe, no invoca.
     *
     * @return cuántas entradas aportaron los suscriptores
     */
    public static <T> int contribute(Event<Contributor<T>> event, Map<String, T> target) {
        int[] added = {0};
        event.invoker().collect((id, value) -> {
            Objects.requireNonNull(value, "value");
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Id de contenido vacio");
            }
            target.put(id, value);
            added[0]++;
        });
        return added[0];
    }
}
