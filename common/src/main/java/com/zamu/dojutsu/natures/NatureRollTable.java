package com.zamu.dojutsu.natures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.rng.RollTable;
import com.zamu.dojutsu.domain.rng.WeightedEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Tabla data-driven de la asignacion aleatoria del onboarding ({@code main/internal/get_random_kg} +
 * {@code random_aff}, {@code [S03]}). Se carga de {@code data/dojutsu/rng/nature_roll.json}.
 *
 * <p><b>Como era en el datapack:</b> las loot tables {@code *_rng} no son tablas de pesos, sino un truco para
 * sacar un <b>entero uniforme</b> en {@code [1, range]} ({@code rolls:{min,max}} + {@code store result run loot
 * spawn}); el reparto real esta en los rangos que compara {@code get_random_kg}. Por eso cada entrada declara su
 * {@code [min, max]}: asi el JSON se puede auditar <b>linea a linea</b> contra la funcion original. Los enteros
 * del rango que no cubre ninguna entrada son <b>"sin premio"</b>.
 *
 * <p>{@link #rollTable(boolean)} lo convierte al {@link RollTable} del dominio (DJT-015, ya testeado): el peso
 * de cada entrada es el <b>numero de enteros de su rango</b>, mas una entrada "sin premio" con los que sobren.
 * La distribucion resultante es <b>identica</b> a la del entero uniforme.
 *
 * @param range                tamano del sorteo ({@code 1..range}); {@code random_kg_rng} = 50
 * @param entries              tramos que otorgan algo
 * @param secondAffinityOneIn  probabilidad {@code 1/N} de 2a afinidad ({@code random_aff_rng_2}: 1 de 10)
 * @param genjutsuOneIn        probabilidad {@code 1/N} de genjutsu/Yin ({@code gen_ulk_rng}: 1 de 7)
 * @param basicAffinities      naturalezas de {@code random_aff} (uniforme entre ellas)
 */
public record NatureRollTable(int range, List<Entry> entries, int secondAffinityOneIn, int genjutsuOneIn,
        List<NatureType> basicAffinities) {

    /** Tabla vacia: sin datos cargados no se otorga nada (ver {@link NatureRollRegistry}). */
    public static final NatureRollTable EMPTY = new NatureRollTable(0, List.of(), 0, 0, List.of());

    public static final Codec<NatureRollTable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("range").forGetter(NatureRollTable::range),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(NatureRollTable::entries),
            Codec.INT.fieldOf("secondAffinityOneIn").forGetter(NatureRollTable::secondAffinityOneIn),
            Codec.INT.fieldOf("genjutsuOneIn").forGetter(NatureRollTable::genjutsuOneIn),
            EnumCodecs.NATURE.listOf().fieldOf("basicAffinities").forGetter(NatureRollTable::basicAffinities)
    ).apply(instance, NatureRollTable::new));

    public NatureRollTable {
        entries = List.copyOf(entries);
        basicAffinities = List.copyOf(basicAffinities);
    }

    /**
     * Construye la {@link RollTable} del dominio para este sorteo. Cada entrada aplicable pesa lo que ocupa su
     * rango; el resto del {@code range} va a una entrada <b>"sin premio"</b> ({@link Outcome#nothing()}).
     *
     * @param senju si el jugador es del clan Senju (habilita las entradas {@code senjuOnly}: Madera 30..40)
     */
    public RollTable<Outcome> rollTable(boolean senju) {
        List<WeightedEntry<Outcome>> weighted = new ArrayList<>();
        int covered = 0;
        for (Entry entry : entries) {
            if (entry.senjuOnly() && !senju) {
                continue; // ese tramo queda sin cubrir -> cae en "sin premio"
            }
            int weight = entry.size();
            covered += weight;
            weighted.add(new WeightedEntry<>(new Outcome(entry.nature(), entry.kekkeiGenkai(), true), weight));
        }
        int nothing = Math.max(0, range - covered);
        weighted.add(new WeightedEntry<>(Outcome.nothing(), nothing));
        return new RollTable<>(weighted);
    }

    /**
     * Valida la tabla contra el propio {@code range}: tramos dentro de rango, sin solaparse y con un unico
     * premio por entrada. Un JSON mal editado alteraria las probabilidades canonicas <b>en silencio</b>
     * ({@code [S03-R4]}), asi que se comprueba al cargar.
     *
     * @return el primer problema encontrado, o vacio si la tabla es coherente
     */
    public Optional<String> validate() {
        if (range <= 0) {
            return Optional.of("range debe ser positivo: " + range);
        }
        if (secondAffinityOneIn <= 0 || genjutsuOneIn <= 0) {
            return Optional.of("las probabilidades 'oneIn' deben ser positivas");
        }
        if (basicAffinities.isEmpty()) {
            return Optional.of("basicAffinities no puede estar vacia");
        }
        boolean[] taken = new boolean[range + 1];
        for (Entry entry : entries) {
            if (entry.min() < 1 || entry.max() > range || entry.min() > entry.max()) {
                return Optional.of("tramo fuera de [1, " + range + "]: " + entry.min() + ".." + entry.max());
            }
            if (entry.nature().isEmpty() == entry.kekkeiGenkai().isEmpty()) {
                return Optional.of("cada entrada debe dar exactamente una naturaleza o un KG: "
                        + entry.min() + ".." + entry.max());
            }
            for (int i = entry.min(); i <= entry.max(); i++) {
                if (taken[i]) {
                    return Optional.of("tramos solapados en el valor " + i + " (alteraria las probabilidades)");
                }
                taken[i] = true;
            }
        }
        return Optional.empty();
    }

    /**
     * Resultado de una tirada de KG.
     *
     * @param nature       naturaleza otorgada, si la tirada dio una (Yang)
     * @param kekkeiGenkai KG otorgado, si la tirada dio uno
     * @param claimed      hubo <b>premio</b> ({@code d.rkg_gkg}): da +20 ninja_xp y <b>salta</b> la 2a afinidad.
     *                     Ojo: en el datapack el flag se marca por caer en el tramo, <b>aunque el jugador ya
     *                     tuviera</b> ese KG y no se le otorgue nada
     */
    public record Outcome(Optional<NatureType> nature, Optional<KekkeiGenkai> kekkeiGenkai, boolean claimed) {

        private static final Outcome NOTHING = new Outcome(Optional.empty(), Optional.empty(), false);

        /** Sin premio: la tirada cayo fuera de todo tramo (18..50 en el datapack). */
        public static Outcome nothing() {
            return NOTHING;
        }
    }

    /**
     * Tramo del sorteo: los enteros de {@code [min, max]} otorgan {@code nature} o {@code kekkeiGenkai}.
     *
     * @param senjuOnly solo aplica al clan Senju ({@code predicate clan/is_senju} en {@code get_random_kg})
     */
    public record Entry(int min, int max, Optional<NatureType> nature, Optional<KekkeiGenkai> kekkeiGenkai,
            boolean senjuOnly) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("min").forGetter(Entry::min),
                Codec.INT.fieldOf("max").forGetter(Entry::max),
                EnumCodecs.NATURE.optionalFieldOf("nature").forGetter(Entry::nature),
                EnumCodecs.KEKKEI_GENKAI.optionalFieldOf("kekkeiGenkai").forGetter(Entry::kekkeiGenkai),
                Codec.BOOL.optionalFieldOf("senjuOnly", false).forGetter(Entry::senjuOnly)
        ).apply(instance, Entry::new));

        /** Cuantos enteros cubre el tramo (= su peso en la {@link RollTable}). */
        public int size() {
            return max - min + 1;
        }
    }

}
