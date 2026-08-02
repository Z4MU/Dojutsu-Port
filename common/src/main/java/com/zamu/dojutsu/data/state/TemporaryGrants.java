package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Naturalezas y Kekkei Genkai <b>prestados</b> al ninja por una fuente externa ({@code d.tmp_<nat>} /
 * {@code d.tkg_<kg>} del datapack, {@code [S03]}): cada entrada recuerda <b>quien</b> los presto, para poder
 * retirarlos cuando esa fuente desaparezca.
 *
 * <p><b>"Temporal" aqui significa PRESTADO, no "con fecha de caducidad"</b> (evidencia del datapack): quien
 * marca {@code d.tmp_*} es <b>solo</b> {@code jinchuriki/sealing/*} (la bestia con cola te presta sus
 * naturalezas) y quien lo limpia es <b>solo</b> {@code jinchuriki/removal/*} via {@code natures/remove_tmp}.
 * <b>No hay ningun temporizador</b> en el datapack: el flag es <b>procedencia</b>, no tiempo. Por eso este
 * modelo guarda la <b>fuente</b> (p. ej. {@code "one_tail"}) y no un timestamp, y no hace falta ningun sistema
 * por tick que sondee vencimientos.
 *
 * <p><b>Corrige {@code [S03-R3]}</b> (duplicacion permanente/temporal): en vez de duplicar el juego de flags
 * ({@code d.has_*} + {@code d.tmp_*} + {@code d.tkg_*}), hay <b>un unico</b> conjunto de naturalezas/KG
 * poseidos en {@link NatureProfile} y, aparte, esta marca de cuales son prestados y por quien.
 *
 * <p><b>Mejora sobre el datapack:</b> el datapack no recuerda la fuente, asi que {@code remove_tmp} retira
 * <b>todos</b> los prestamos a la vez; con {@code AllowMultJinchuriki} activo, extraer una bestia te quitaria
 * tambien lo que te presto otra. Guardar la fuente lo hace preciso. Con una sola bestia (el caso por defecto)
 * el comportamiento es identico.
 *
 * <p>Value object <b>inmutable</b>; se serializa dentro de {@link NatureProfile}.
 *
 * @param natures      naturaleza prestada -> id de la fuente que la presto
 * @param kekkeiGenkai KG prestado -> id de la fuente que lo presto
 */
public record TemporaryGrants(Map<NatureType, String> natures, Map<KekkeiGenkai, String> kekkeiGenkai) {

    /** Sin prestamos activos. */
    public static final TemporaryGrants DEFAULT = new TemporaryGrants(Map.of(), Map.of());

    /**
     * Codec del sub-estado. Los mapas viajan como {@code nombre -> fuente}; un nombre desconocido (NBT corrupto
     * o de una version futura) se <b>descarta</b>, misma politica tolerante que el resto de {@code data.state}.
     */
    public static final Codec<TemporaryGrants> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("natures", Map.of())
                    .forGetter(g -> byName(g.natures())),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("kekkeiGenkai", Map.of())
                    .forGetter(g -> byName(g.kekkeiGenkai()))
    ).apply(instance, TemporaryGrants::fromCodec));

    public TemporaryGrants {
        natures = immutableCopy(natures, NatureType.class);
        kekkeiGenkai = immutableCopy(kekkeiGenkai, KekkeiGenkai.class);
    }

    /** Indica si no hay ningun prestamo activo. */
    public boolean isEmpty() {
        return natures.isEmpty() && kekkeiGenkai.isEmpty();
    }

    /** Indica si la naturaleza esta prestada (frente a ser propia). */
    public boolean isTemporary(NatureType nature) {
        return natures.containsKey(nature);
    }

    /** Indica si el KG esta prestado. */
    public boolean isTemporary(KekkeiGenkai kg) {
        return kekkeiGenkai.containsKey(kg);
    }

    /** Fuente que presto la naturaleza, o vacio si no esta prestada. */
    public Optional<String> sourceOf(NatureType nature) {
        return Optional.ofNullable(natures.get(nature));
    }

    /** Fuente que presto el KG, o vacio si no esta prestado. */
    public Optional<String> sourceOf(KekkeiGenkai kg) {
        return Optional.ofNullable(kekkeiGenkai.get(kg));
    }

    /** Naturalezas prestadas por una fuente concreta. */
    public Set<NatureType> naturesFrom(String source) {
        EnumMap<NatureType, String> matching = new EnumMap<>(NatureType.class);
        natures.forEach((nature, from) -> {
            if (from.equals(source)) {
                matching.put(nature, from);
            }
        });
        return Collections.unmodifiableSet(matching.keySet());
    }

    /** KG prestados por una fuente concreta. */
    public Set<KekkeiGenkai> kekkeiGenkaiFrom(String source) {
        EnumMap<KekkeiGenkai, String> matching = new EnumMap<>(KekkeiGenkai.class);
        kekkeiGenkai.forEach((kg, from) -> {
            if (from.equals(source)) {
                matching.put(kg, from);
            }
        });
        return Collections.unmodifiableSet(matching.keySet());
    }

    /** Copia marcando una naturaleza como prestada por {@code source}. */
    public TemporaryGrants withNature(NatureType nature, String source) {
        Map<NatureType, String> next = mutableCopy(natures, NatureType.class);
        next.put(nature, source);
        return new TemporaryGrants(next, kekkeiGenkai);
    }

    /** Copia sin la marca de prestamo de una naturaleza (idempotente). */
    public TemporaryGrants withoutNature(NatureType nature) {
        if (!natures.containsKey(nature)) {
            return this;
        }
        Map<NatureType, String> next = mutableCopy(natures, NatureType.class);
        next.remove(nature);
        return new TemporaryGrants(next, kekkeiGenkai);
    }

    /** Copia marcando un KG como prestado por {@code source}. */
    public TemporaryGrants withKekkeiGenkai(KekkeiGenkai kg, String source) {
        Map<KekkeiGenkai, String> next = mutableCopy(kekkeiGenkai, KekkeiGenkai.class);
        next.put(kg, source);
        return new TemporaryGrants(natures, next);
    }

    /** Copia sin la marca de prestamo de un KG (idempotente). */
    public TemporaryGrants withoutKekkeiGenkai(KekkeiGenkai kg) {
        if (!kekkeiGenkai.containsKey(kg)) {
            return this;
        }
        Map<KekkeiGenkai, String> next = mutableCopy(kekkeiGenkai, KekkeiGenkai.class);
        next.remove(kg);
        return new TemporaryGrants(natures, next);
    }

    private static TemporaryGrants fromCodec(Map<String, String> natures, Map<String, String> kekkeiGenkai) {
        return new TemporaryGrants(parse(natures, NatureType.class), parse(kekkeiGenkai, KekkeiGenkai.class));
    }

    /** Serializa las claves enum a nombres en minusculas, ordenados (NBT determinista). */
    private static <E extends Enum<E>> Map<String, String> byName(Map<E, String> values) {
        Map<String, String> named = new TreeMap<>();
        values.forEach((key, source) -> named.put(key.name().toLowerCase(Locale.ROOT), source));
        return named;
    }

    /** Deserializa nombres a enums descartando los desconocidos (tolerancia ante NBT corrupto o futuro). */
    private static <E extends Enum<E>> Map<E, String> parse(Map<String, String> named, Class<E> type) {
        Map<E, String> parsed = new LinkedHashMap<>();
        named.forEach((name, source) -> {
            for (E value : type.getEnumConstants()) {
                if (value.name().toLowerCase(Locale.ROOT).equals(name)) {
                    parsed.put(value, source);
                    return;
                }
            }
        });
        return parsed;
    }

    /**
     * Copia mutable con el tipo de clave <b>explicito</b>. Nunca {@code new EnumMap<>(otroMapa)}: ese
     * constructor lanza si el mapa esta vacio y no es ya un {@code EnumMap} (no puede inferir el tipo de la
     * clave), que es justo el caso de {@link #DEFAULT} (sus mapas van envueltos en {@code unmodifiableMap}).
     */
    private static <E extends Enum<E>> Map<E, String> mutableCopy(Map<E, String> values, Class<E> type) {
        EnumMap<E, String> copy = new EnumMap<>(type);
        copy.putAll(values);
        return copy;
    }

    private static <E extends Enum<E>> Map<E, String> immutableCopy(Map<E, String> values, Class<E> type) {
        return Collections.unmodifiableMap(mutableCopy(values, type));
    }
}
