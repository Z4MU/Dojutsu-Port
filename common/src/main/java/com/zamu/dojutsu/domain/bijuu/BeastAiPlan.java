package com.zamu.dojutsu.domain.bijuu;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Decision de la IA de una Bestia con Cola {@code [S09]} (F7/DJT-161): <b>que tecnica lanza en su turno</b>.
 * Dominio puro y verificado contra {@code tailed_beasts/ai/get_move} + {@code cast_1..4}.
 *
 * <h2>La bestia falla su turno a menudo, y es a proposito</h2>
 * El datapack tira un numero de {@value #ROLL_MIN} a {@value #ROLL_MAX} (una tabla de botin que suelta entre 0 y
 * 5 objetos, {@code loot_tables/ai_rng}) y <b>no todos los valores hacen algo</b>: el 0 y el 5 no estan mapeados,
 * y para una bestia normal el 3 tampoco. Resultado: una bestia corriente ataca en <b>3 de cada 6</b> turnos y el
 * Diez Colas en <b>4 de 6</b>. Combinado con su temporizador mas corto ({@link BeastStats#aiIntervalTicks}), el
 * Diez Colas ataca del orden de <b>2,5 veces mas</b> que cualquier otra. Esas pausas son lo que hace la pelea
 * jugable; replicarlas importa tanto como replicar el daño.
 *
 * <h2>Cuatro slots, no diez comportamientos</h2>
 * {@code cast_1..cast_4} son cuatro <b>papeles</b> y cada bestia rellena los suyos:
 * <ul>
 *   <li><b>Slot 1</b> — su tecnica insignia. El Diez Colas usa Tenpenchii, salvo que aun corra su temporizador.</li>
 *   <li><b>Slot 2</b> — la Bola Bestia con Cola. Solo si tienes a alguien <b>encima</b>
 *       ({@value #MELEE_RANGE} bloques); si no, la bestia recurre al slot 3.</li>
 *   <li><b>Slot 3</b> — de nuevo la insignia para las nueve; para el Diez Colas, Brazos de Chakra.</li>
 *   <li><b>Slot 4</b> — Orbes Buscadores de la Verdad, <b>solo</b> del Diez Colas.</li>
 * </ul>
 */
public final class BeastAiPlan {

    /** Valor minimo de la tirada ({@code ai_rng}: {@code rolls 0..5}). */
    public static final int ROLL_MIN = 0;

    /** Valor maximo de la tirada. */
    public static final int ROLL_MAX = 5;

    /** Distancia a la que la bestia considera que te tiene encima ({@code @p[distance=0..5]}). */
    public static final double MELEE_RANGE = 5.0;

    /** Bola Bestia con Cola: la tecnica que comparten las diez ({@code ai/cast_2}). */
    public static final String TAILED_BEAST_BALL = "tailed_beast_ball";

    /** Version del Diez Colas de la Bola Bestia con Cola ({@code moves/tailed_beast_ball/ten_tails}). */
    public static final String TAILED_BEAST_BALL_TEN_TAILS = "tailed_beast_ball_ten_tails";

    /** Brazos de Chakra: insignia del Nueve Colas y slot 3 del Diez Colas. */
    public static final String CHAKRA_ARMS_BARRAGE = "chakra_arms_barrage";

    /** Orbes Buscadores de la Verdad: exclusiva del Diez Colas ({@code cast_4}). */
    public static final String TRUTH_SEEKING_ORB = "truth_seeking_orb";

    private BeastAiPlan() {
    }

    /** Numero de resultados distintos de la tirada (6: del 0 al 5, ambos incluidos). */
    public static final int ROLL_OUTCOMES = ROLL_MAX - ROLL_MIN + 1;

    /**
     * Tira el dado de la IA ({@code loot dojutsu:ai_rng}): {@value #ROLL_MIN}..{@value #ROLL_MAX} uniforme.
     *
     * <p>El goal no usa esta sobrecarga porque el {@code RandomSource} de Minecraft no es un
     * {@link RandomGenerator}; usa {@link #ROLL_OUTCOMES} directamente, que es la misma fuente de verdad.
     */
    public static int roll(RandomGenerator random) {
        return ROLL_MIN + random.nextInt(ROLL_OUTCOMES);
    }

    /**
     * Elige la tecnica de este turno, o vacio si la bestia <b>no hace nada</b> (que es lo normal en la mitad de
     * sus turnos). Decision pura.
     *
     * @param beast           bestia que decide
     * @param roll            tirada {@value #ROLL_MIN}..{@value #ROLL_MAX}
     * @param targetInMelee   tiene un objetivo a {@value #MELEE_RANGE} bloques o menos
     * @param tenpenchiiReady el temporizador del Tenpenchii <b>no</b> esta corriendo (solo importa al Diez Colas)
     */
    public static Optional<String> choose(TailedBeast beast, int roll, boolean targetInMelee,
            boolean tenpenchiiReady) {
        return switch (roll) {
            case 1 -> Optional.of(slotOne(beast, tenpenchiiReady));
            case 2 -> Optional.of(targetInMelee ? slotTwo(beast) : slotThree(beast));
            case 3 -> beast.isTenTails() ? Optional.of(TRUTH_SEEKING_ORB) : Optional.empty();
            case 4 -> Optional.of(beast.isTenTails() ? slotThree(beast) : slotOne(beast, tenpenchiiReady));
            default -> Optional.empty(); // 0 y 5 no estan mapeados: la bestia se toma el turno libre
        };
    }

    /** Slot 1: la insignia. El Diez Colas cae a Brazos de Chakra si su Tenpenchii sigue en cuenta atras. */
    private static String slotOne(TailedBeast beast, boolean tenpenchiiReady) {
        if (beast.isTenTails() && !tenpenchiiReady) {
            return CHAKRA_ARMS_BARRAGE;
        }
        return beast.signatureMove();
    }

    /** Slot 2: la Bola Bestia con Cola, en su version normal o la del Diez Colas. */
    private static String slotTwo(TailedBeast beast) {
        return beast.isTenTails() ? TAILED_BEAST_BALL_TEN_TAILS : TAILED_BEAST_BALL;
    }

    /** Slot 3: la insignia otra vez; para el Diez Colas, Brazos de Chakra. */
    private static String slotThree(TailedBeast beast) {
        return beast.isTenTails() ? CHAKRA_ARMS_BARRAGE : beast.signatureMove();
    }
}
