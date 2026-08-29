package com.zamu.dojutsu.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Proyeccion <b>observable</b> del conocimiento de jutsus que viaja al cliente (HUD de enfriamiento y, desde
 * F11/DJT-231, el selector de jutsu). Hermano de {@link ProgressionSnapshot}: agrupa en un solo componente lo
 * que el cliente necesita saber de los jutsus, en vez de ir sumando campos sueltos al payload.
 *
 * <p><b>Por que agrupar.</b> {@code StreamCodec.composite} admite como maximo <b>seis</b> componentes, y el
 * payload ya los tenia todos ocupados. Agrupar aqui deja el payload en seis y —lo que importa mas— hace que
 * anadir el siguiente dato de jutsu (el seleccionado, la asignacion de slots) sea un campo en este record y no
 * un rediseno del payload.
 *
 * <p>Lo que <b>no</b> viaja: el progreso del learner por elemento ({@code learnProgress}), que es estado
 * interno y ningun consumidor de cliente lee.
 *
 * @param cooldowns {@code jutsuId -> tick de mundo en que vuelve a estar disponible} (F4/DJT-081). El cliente
 *                  calcula el restante con su propio tick de mundo
 * @param known     ids de los jutsus aprendidos, en orden estable, para el selector (F11/DJT-231).
 *                  <b>Opcional a proposito:</b> vacio significa "no ha cambiado, conserva la que ya tienes".
 *                  Ver la nota de tamano de abajo
 * @param selected  jutsu seleccionado para la tecla de cast (F11/DJT-231), o vacio si no hay ninguno.
 *                  Aqui vacio significa <b>"no hay seleccion"</b>, no "sin cambios": es un solo id y viaja
 *                  siempre, asi que no necesita el tratamiento de {@link #known}
 * @param slots     reparto de los ocho huecos de la rueda (F11/DJT-234), o vacio si no ha cambiado. Mismo
 *                  criterio que {@link #known} y por el mismo motivo: cambia al aprender o al reordenar, no
 *                  cada vez que se regenera chakra
 */
public record JutsuSnapshot(Map<String, Long> cooldowns, Optional<List<String>> known,
        Optional<String> selected, Optional<List<String>> slots) {

    /** Snapshot vacio (antes del primer sync). */
    public static final JutsuSnapshot EMPTY =
            new JutsuSnapshot(Map.of(), Optional.empty(), Optional.empty(), Optional.empty());

    private static final StreamCodec<ByteBuf, Map<String, Long>> COOLDOWNS_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG);

    /**
     * Lista de aprendidos, opcional: ausente = "sin cambios".
     *
     * <p><b>Es el unico campo observable con delta propio, y es deliberado.</b> Es la unica coleccion que
     * puede llegar a 128 entradas de texto, y los deltas los dispara la <b>regeneracion de chakra</b>, o sea
     * constantemente. Reenviarla en cada delta seria mandar kilobytes por jugador y por pulso de regeneracion
     * para repetir siempre lo mismo, porque la lista solo cambia al <b>aprender</b> un jutsu: unas decenas de
     * veces en toda una partida. El resto de campos observables son pequenos y siguen reenviandose enteros
     * (la deuda documentada en F2, que sigue sin doler).
     */
    private static final StreamCodec<ByteBuf, Optional<List<String>>> LIST_CODEC = ByteBufCodecs.optional(
            ByteBufCodecs.<ByteBuf, String, List<String>>collection(ArrayList::new, ByteBufCodecs.STRING_UTF8));

    public static final StreamCodec<ByteBuf, JutsuSnapshot> STREAM_CODEC = StreamCodec.composite(
            COOLDOWNS_CODEC, JutsuSnapshot::cooldowns,
            LIST_CODEC, JutsuSnapshot::known,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), JutsuSnapshot::selected,
            LIST_CODEC, JutsuSnapshot::slots,
            JutsuSnapshot::new);

    public JutsuSnapshot {
        cooldowns = Map.copyOf(cooldowns);
        known = known.map(List::copyOf);
        slots = slots.map(List::copyOf);
    }

    /**
     * Snapshot de jutsus, incluyendo cada lista <b>solo si ha cambiado</b>.
     *
     * @param known ausente para no reenviar la lista de aprendidos en un delta corriente
     * @param slots ausente para no reenviar el reparto de la rueda en un delta corriente
     */
    public static JutsuSnapshot of(Map<String, Long> cooldowns, Optional<String> selected,
            Optional<Set<String>> known, Optional<List<String>> slots) {
        return new JutsuSnapshot(cooldowns, known.map(ids -> ids.stream().sorted().toList()), selected, slots);
    }

    /** Snapshot con la lista de aprendidos incluida (snapshot completo, o delta tras aprender un jutsu). */
    public static JutsuSnapshot withKnown(Map<String, Long> cooldowns, Set<String> known,
            Optional<String> selected) {
        return of(cooldowns, selected, Optional.of(known), Optional.empty());
    }

    /** Snapshot sin las listas: el delta corriente, en el que solo pueden haber cambiado los enfriamientos. */
    public static JutsuSnapshot withoutKnown(Map<String, Long> cooldowns, Optional<String> selected) {
        return of(cooldowns, selected, Optional.empty(), Optional.empty());
    }
}
