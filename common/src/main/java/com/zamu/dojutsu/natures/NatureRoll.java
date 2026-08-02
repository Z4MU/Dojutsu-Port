package com.zamu.dojutsu.natures;

import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.natures.NatureRollTable.Outcome;
import java.util.random.RandomGenerator;

/**
 * Tiradas de la asignacion aleatoria del ninja ({@code main/internal/get_random_kg} + {@code random_aff},
 * {@code [S03]}). Reproduce la <b>distribucion exacta</b> del datapack con los pesos de
 * {@link NatureRollRegistry} (extraidos de las loot tables {@code *_rng}).
 *
 * <p><b>Solo tira: no aplica nada.</b> Devuelve el resultado y es el <b>onboarding</b> (DJT-059) quien lo
 * ejecuta en el orden de {@code get_random_kg}:
 * <ol>
 *   <li>{@link #rollKekkeiGenkai} (siempre, salvo clan Lee: {@code join_finish} se lo salta entero);</li>
 *   <li>si <b>no</b> hubo premio ({@code claimed}), {@link #rollSecondAffinity} y, si acierta,
 *       {@link #rollBasicAffinity} -> esa afinidad arranca en <b>50</b>;</li>
 *   <li>siempre, {@link #rollGenjutsu}: si acierta, genjutsu +10, ninja_xp +10 y la naturaleza Yin;</li>
 *   <li>si hubo premio, <b>+20 ninja_xp</b>.</li>
 * </ol>
 * Separar tirada de aplicacion mantiene esto <b>puro y testeable con semilla</b> (el DoD del ticket es la
 * distribucion) y deja la escritura en las puertas de siempre ({@code NatureService}/{@code XpApi}).
 *
 * <p>El azar se <b>inyecta</b> ({@link RandomGenerator}, como exige {@code RollTable}): con semilla fija los
 * tests son deterministas. El llamador de runtime pasa el {@code RandomSource} del jugador adaptado.
 *
 * <p><b>Tabla muerta no portada:</b> {@code natures/give/kg/random} (loot {@code random_kg_rng_alt}, 1..18, con
 * blaze e yin) <b>no la llama nadie</b> en el datapack — el propio autor lo admite en un comentario del archivo.
 * No se porta (seria codigo muerto); si algun item de F8 la necesita, se anade entonces como otra tabla del JSON.
 */
public final class NatureRoll {

    private NatureRoll() {
    }

    /**
     * Tirada principal ({@code loot random_kg_rng}, uniforme 1..50): puede dar un KG, la naturaleza Yang o
     * nada.
     *
     * @param senju si el jugador es del clan Senju (habilita Madera en 30..40). Se pasa como flag y no se
     *              consulta el clan aqui: los clanes son DJT-056 y este servicio no debe depender de ellos
     */
    public static Outcome rollKekkeiGenkai(RandomGenerator random, boolean senju) {
        NatureRollTable table = NatureRollRegistry.table();
        if (table.range() <= 0) {
            return Outcome.nothing(); // sin tabla cargada no se otorga nada (el listener ya logueo el error)
        }
        return table.rollTable(senju).roll(random);
    }

    /**
     * Tirada de 2a afinidad ({@code loot random_aff_rng_2}, acierta 1 de 10). Solo se tira si la principal
     * <b>no</b> dio premio.
     */
    public static boolean rollSecondAffinity(RandomGenerator random) {
        return oneIn(random, NatureRollRegistry.table().secondAffinityOneIn());
    }

    /** Tirada de genjutsu ({@code loot gen_ulk_rng}, acierta 1 de 7). Se tira <b>siempre</b>. */
    public static boolean rollGenjutsu(RandomGenerator random) {
        return oneIn(random, NatureRollRegistry.table().genjutsuOneIn());
    }

    /**
     * Naturaleza basica al azar ({@code main/internal/random_aff}, uniforme 1..5). La afinidad resultante
     * arranca en 50 (lo aplica el onboarding).
     *
     * @throws IllegalStateException si no hay tabla cargada
     */
    public static NatureType rollBasicAffinity(RandomGenerator random) {
        var basics = NatureRollRegistry.table().basicAffinities();
        if (basics.isEmpty()) {
            throw new IllegalStateException("no hay tabla de asignacion aleatoria cargada");
        }
        return basics.get(random.nextInt(basics.size()));
    }

    /** {@code true} con probabilidad {@code 1/oneIn}; {@code false} si {@code oneIn} no es positivo (sin tabla). */
    private static boolean oneIn(RandomGenerator random, int oneIn) {
        return oneIn > 0 && random.nextInt(oneIn) == 0;
    }
}
