package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Sub-estado del perfil elemental del ninja {@code [S03]}: que naturalezas tiene, la afinidad (nivel/XP) de
 * cada una, sus Kekkei Genkai, si lleva celulas de Hashirama y si ya cobro los boosts por set completo.
 *
 * <p>Value object <b>inmutable</b> (patron de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}): expone {@link #DEFAULT}, {@link #CODEC} y withers. Como en
 * {@link ProgressionState}, {@link LeveledStat} es tambien un valor inmutable (DJT-063): para cambiar una
 * afinidad se <b>reemplaza</b> con {@link #withAffinity}.
 *
 * <p><b>La afinidad es el mismo eje que las habilidades</b> ({@code [S13-R1]}): un {@link LeveledStat} con la
 * misma {@code XpCurve} (cap 500), donde el nivel es {@code d.aff_<nat>} y la XP es {@code d.<nat>_xp}. No es
 * un eje paralelo. La ficha del ticket lo llamaba {@code Affinity{level,xp}}: es exactamente
 * {@link LeveledStat}, asi que se reutiliza en vez de crear un tipo gemelo.
 *
 * <p><b>Yin-Yang no se almacena, se deriva</b> ({@link #affinity}): {@code d.aff_yinyang} es en el datapack
 * {@code (aff_yin + aff_yang) / 2} ({@code menu/leveling/affinity/update_yinyang}), sin XP propia. Misma
 * postura que el rango ninja de {@link ProgressionState#rank()}: derivado, nunca cacheado, imposible de
 * desincronizar.
 *
 * <p>Las naturalezas/KG <b>prestados</b> ({@link TemporaryGrants}, {@code [S03-R3]}, DJT-053) no duplican este
 * conjunto: viven como una <b>marca de procedencia</b> aparte. {@link #natures()} y {@link #kekkeiGenkai()} son
 * <b>todo</b> lo que el ninja tiene, sea propio o prestado.
 *
 * @param natures       naturalezas poseidas (flags {@code d.has_<nat>}); incluye las prestadas
 * @param affinities    nivel/XP por naturaleza con afinidad almacenada (siempre las 7; mapa inmutable)
 * @param kekkeiGenkai  Kekkei Genkai poseidos (flags {@code d.kg_<kg>}); incluye los prestados
 * @param hashiramaDna  celulas de Hashirama implantadas (flag {@code d.has_hsdna}); no es un KG
 * @param allBasicsBoost  boost por todas las basicas ya cobrado (flag {@code d.all_basics}; idempotencia)
 * @param allNaturesBoost boost por todas las naturalezas ya cobrado (flag {@code d.all_natures}; idempotencia)
 * @param temporary     cuales de los anteriores estan <b>prestados</b> y por que fuente
 */
public record NatureProfile(Set<NatureType> natures, Map<NatureType, LeveledStat> affinities,
        Set<KekkeiGenkai> kekkeiGenkai, boolean hashiramaDna, boolean allBasicsBoost, boolean allNaturesBoost,
        TemporaryGrants temporary) {

    /** Perfil de un jugador nuevo: sin naturalezas ni KG, afinidades a nivel 0, sin boosts ni prestamos. */
    public static final NatureProfile DEFAULT =
            new NatureProfile(Set.of(), Map.of(), Set.of(), false, false, false, TemporaryGrants.DEFAULT);

    /**
     * Codec del sub-estado. Cada afinidad es un campo opcional propio (tolerancia por naturaleza, igual que las
     * habilidades en {@link ProgressionState}). Los sets viajan como listas de nombres ordenadas y toleran
     * nombres desconocidos (NBT corrupto o de una version futura): se descartan en vez de romper la carga.
     */
    public static final Codec<NatureProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("natures", List.of())
                    .forGetter(p -> names(p.natures())),
            LeveledStats.CODEC.optionalFieldOf("fire", new LeveledStat()).forGetter(p -> p.affinity(NatureType.FIRE)),
            LeveledStats.CODEC.optionalFieldOf("wind", new LeveledStat()).forGetter(p -> p.affinity(NatureType.WIND)),
            LeveledStats.CODEC.optionalFieldOf("lightning", new LeveledStat()).forGetter(p -> p.affinity(NatureType.LIGHTNING)),
            LeveledStats.CODEC.optionalFieldOf("earth", new LeveledStat()).forGetter(p -> p.affinity(NatureType.EARTH)),
            LeveledStats.CODEC.optionalFieldOf("water", new LeveledStat()).forGetter(p -> p.affinity(NatureType.WATER)),
            LeveledStats.CODEC.optionalFieldOf("yin", new LeveledStat()).forGetter(p -> p.affinity(NatureType.YIN)),
            LeveledStats.CODEC.optionalFieldOf("yang", new LeveledStat()).forGetter(p -> p.affinity(NatureType.YANG)),
            Codec.STRING.listOf().optionalFieldOf("kekkeiGenkai", List.of())
                    .forGetter(p -> names(p.kekkeiGenkai())),
            Codec.BOOL.optionalFieldOf("hashiramaDna", false).forGetter(NatureProfile::hashiramaDna),
            Codec.BOOL.optionalFieldOf("allBasicsBoost", false).forGetter(NatureProfile::allBasicsBoost),
            Codec.BOOL.optionalFieldOf("allNaturesBoost", false).forGetter(NatureProfile::allNaturesBoost),
            TemporaryGrants.CODEC.optionalFieldOf("temporary", TemporaryGrants.DEFAULT).forGetter(NatureProfile::temporary)
    ).apply(instance, NatureProfile::fromCodec));

    /** Normaliza: sets inmutables y mapa de afinidades completo (las 7 con afinidad almacenada) e inmutable. */
    public NatureProfile {
        natures = immutableCopy(natures, NatureType.class);
        kekkeiGenkai = immutableCopy(kekkeiGenkai, KekkeiGenkai.class);
        affinities = normalizeAffinities(affinities);
        Objects.requireNonNull(temporary, "temporary");
    }

    /** Indica si el ninja posee la naturaleza. */
    public boolean has(NatureType nature) {
        return natures.contains(nature);
    }

    /** Indica si el ninja posee el Kekkei Genkai. */
    public boolean has(KekkeiGenkai kg) {
        return kekkeiGenkai.contains(kg);
    }

    /**
     * Afinidad de una naturaleza. Para {@link NatureType#YIN_YANG} la <b>deriva</b> (no la almacena):
     * nivel = {@code (nivel yin + nivel yang) / 2} (division entera, como {@code update_yinyang}) y XP = 0
     * (el datapack no lleva XP de yin-yang).
     */
    public LeveledStat affinity(NatureType nature) {
        if (nature == NatureType.YIN_YANG) {
            int derived = (affinity(NatureType.YIN).level() + affinity(NatureType.YANG).level()) / 2;
            return new LeveledStat(derived, 0);
        }
        return affinities.get(nature);
    }

    /** Indica si tiene las 5 basicas (condicion del boost {@code d.all_basics}). */
    public boolean hasAllBasics() {
        return natures.containsAll(NatureType.basics());
    }

    /**
     * Indica si tiene las 7 naturalezas que exige {@code predicates/has_all_natures} (las basicas + Yin + Yang;
     * Yin-Yang <b>no</b> cuenta). Condicion del boost {@code d.all_natures}.
     */
    public boolean hasAllNatures() {
        return natures.containsAll(NatureType.withAffinity());
    }

    /** Copia con una naturaleza anadida (idempotente: si ya estaba, devuelve la misma instancia). */
    public NatureProfile withNature(NatureType nature) {
        if (natures.contains(nature)) {
            return this;
        }
        EnumSet<NatureType> next = copyOf(natures, NatureType.class);
        next.add(nature);
        return new NatureProfile(next, affinities, kekkeiGenkai, hashiramaDna, allBasicsBoost, allNaturesBoost, temporary);
    }

    /** Copia sin una naturaleza (idempotente). No toca su afinidad: el datapack tampoco la borra al hacer take. */
    public NatureProfile withoutNature(NatureType nature) {
        if (!natures.contains(nature)) {
            return this;
        }
        EnumSet<NatureType> next = copyOf(natures, NatureType.class);
        next.remove(nature);
        return new NatureProfile(next, affinities, kekkeiGenkai, hashiramaDna, allBasicsBoost, allNaturesBoost, temporary);
    }

    /**
     * Copia reemplazando la afinidad de una naturaleza.
     *
     * @throws IllegalArgumentException si la naturaleza no almacena afinidad ({@link NatureType#YIN_YANG}:
     *                                  es derivada, escribirla seria crear una fuente de verdad duplicada)
     */
    public NatureProfile withAffinity(NatureType nature, LeveledStat stat) {
        if (!nature.storesAffinity()) {
            throw new IllegalArgumentException("La afinidad de " + nature + " es derivada, no se escribe");
        }
        EnumMap<NatureType, LeveledStat> next = new EnumMap<>(affinities);
        next.put(nature, stat);
        return new NatureProfile(natures, next, kekkeiGenkai, hashiramaDna, allBasicsBoost, allNaturesBoost, temporary);
    }

    /** Copia con un Kekkei Genkai anadido (idempotente). */
    public NatureProfile withKekkeiGenkai(KekkeiGenkai kg) {
        if (kekkeiGenkai.contains(kg)) {
            return this;
        }
        EnumSet<KekkeiGenkai> next = copyOf(kekkeiGenkai, KekkeiGenkai.class);
        next.add(kg);
        return new NatureProfile(natures, affinities, next, hashiramaDna, allBasicsBoost, allNaturesBoost, temporary);
    }

    /** Copia sin un Kekkei Genkai (idempotente). */
    public NatureProfile withoutKekkeiGenkai(KekkeiGenkai kg) {
        if (!kekkeiGenkai.contains(kg)) {
            return this;
        }
        EnumSet<KekkeiGenkai> next = copyOf(kekkeiGenkai, KekkeiGenkai.class);
        next.remove(kg);
        return new NatureProfile(natures, affinities, next, hashiramaDna, allBasicsBoost, allNaturesBoost, temporary);
    }

    /** Copia con el flag de celulas de Hashirama ({@code d.has_hsdna}). */
    public NatureProfile withHashiramaDna(boolean present) {
        return new NatureProfile(natures, affinities, kekkeiGenkai, present, allBasicsBoost, allNaturesBoost, temporary);
    }

    /** Copia marcando el boost de las basicas como cobrado ({@code d.all_basics}). */
    public NatureProfile withAllBasicsBoost(boolean claimed) {
        return new NatureProfile(natures, affinities, kekkeiGenkai, hashiramaDna, claimed, allNaturesBoost, temporary);
    }

    /** Copia marcando el boost de todas las naturalezas como cobrado ({@code d.all_natures}). */
    public NatureProfile withAllNaturesBoost(boolean claimed) {
        return new NatureProfile(natures, affinities, kekkeiGenkai, hashiramaDna, allBasicsBoost, claimed, temporary);
    }

    /** Copia con otras marcas de prestamo ({@link TemporaryGrants}). La llama {@code TemporaryGrantService}. */
    public NatureProfile withTemporary(TemporaryGrants grants) {
        return new NatureProfile(natures, affinities, kekkeiGenkai, hashiramaDna, allBasicsBoost, allNaturesBoost,
                grants);
    }

    private static NatureProfile fromCodec(List<String> natures, LeveledStat fire, LeveledStat wind,
            LeveledStat lightning, LeveledStat earth, LeveledStat water, LeveledStat yin, LeveledStat yang,
            List<String> kekkeiGenkai, boolean hashiramaDna, boolean allBasicsBoost, boolean allNaturesBoost,
            TemporaryGrants temporary) {
        EnumMap<NatureType, LeveledStat> affinities = new EnumMap<>(NatureType.class);
        affinities.put(NatureType.FIRE, fire);
        affinities.put(NatureType.WIND, wind);
        affinities.put(NatureType.LIGHTNING, lightning);
        affinities.put(NatureType.EARTH, earth);
        affinities.put(NatureType.WATER, water);
        affinities.put(NatureType.YIN, yin);
        affinities.put(NatureType.YANG, yang);
        return new NatureProfile(parse(natures, NatureType.class), affinities,
                parse(kekkeiGenkai, KekkeiGenkai.class), hashiramaDna, allBasicsBoost, allNaturesBoost, temporary);
    }

    /** Serializa un set de enums como nombres ordenados (salida determinista: mismo estado -> mismo NBT). */
    private static <E extends Enum<E>> List<String> names(Set<E> values) {
        return values.stream().map(Enum::name).sorted().toList();
    }

    /** Deserializa nombres a enums descartando los desconocidos (tolerancia ante NBT corrupto o futuro). */
    private static <E extends Enum<E>> Set<E> parse(List<String> names, Class<E> type) {
        EnumSet<E> parsed = EnumSet.noneOf(type);
        for (String name : names) {
            for (E value : type.getEnumConstants()) {
                if (value.name().equals(name)) {
                    parsed.add(value);
                    break;
                }
            }
        }
        return parsed;
    }

    private static <E extends Enum<E>> EnumSet<E> copyOf(Set<E> values, Class<E> type) {
        EnumSet<E> copy = EnumSet.noneOf(type);
        copy.addAll(values);
        return copy;
    }

    private static <E extends Enum<E>> Set<E> immutableCopy(Set<E> values, Class<E> type) {
        return Collections.unmodifiableSet(copyOf(values, type));
    }

    /** Completa el mapa con las 7 naturalezas que almacenan afinidad (nivel 0 si faltan) y lo hace inmutable. */
    private static Map<NatureType, LeveledStat> normalizeAffinities(Map<NatureType, LeveledStat> given) {
        EnumMap<NatureType, LeveledStat> complete = new EnumMap<>(NatureType.class);
        for (NatureType nature : NatureType.withAffinity()) {
            LeveledStat stat = given.get(nature);
            complete.put(nature, stat != null ? stat : new LeveledStat());
        }
        return Collections.unmodifiableMap(complete);
    }
}
