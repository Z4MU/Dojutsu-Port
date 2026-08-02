package com.zamu.dojutsu.domain.items;

/**
 * Reglas de las <b>Pildoras de Racion Militar</b> {@code [S14]} (F8/DJT-170). Dominio puro y verificado contra
 * {@code items/ration_pills/{use,eat,queue,queue_exc,effects}}.
 *
 * <h2>La resaca llega tarde, y esa es toda la gracia</h2>
 * Comer una pildora te devuelve <b>un quinto de tu chakra maximo</b> al instante. La factura no llega hasta
 * {@value #HANGOVER_DELAY_TICKS} ticks despues (10 segundos): debilidad, lentitud y fatiga minera. En medio de
 * un combate eso son diez segundos de ventaja... y despues diez de castigo justo cuando ya no puedes elegir.
 *
 * <h2>Encadenar pildoras no encadena resacas</h2>
 * {@code eat} solo encola la resaca {@code unless ration_pill_queued}: comerte las cinco seguidas te da las
 * cinco recargas y <b>una sola</b> resaca. El datapack lo consigue con una bandera y un {@code schedule}; aqui
 * es la misma idea sin polling.
 *
 * <p>El bote trae {@value #PILLS_PER_ITEM} pildoras y cada mordisco gasta una; al agotarse desaparece.
 */
public final class PillTable {

    /** Pildoras que trae un bote nuevo ({@code PillCount:5b}). */
    public static final int PILLS_PER_ITEM = 5;

    /** Divisor del chakra maximo que devuelve cada pildora ({@code eat}: {@code max_chakra / 5}). */
    public static final int CHAKRA_FRACTION = 5;

    /** Ticks entre mordiscos ({@code d.pill_delay = 10}). */
    public static final int EAT_DELAY_TICKS = 10;

    /** Ticks hasta que llega la resaca ({@code queue}: {@code schedule ... 10s}). */
    public static final int HANGOVER_DELAY_TICKS = 200;

    /** Duracion de la debilidad de la resaca, en ticks ({@code effects}: {@code weakness 10}). */
    public static final int HANGOVER_WEAKNESS_TICKS = 200;

    /** Amplificador de la debilidad y de la lentitud de la resaca. */
    public static final int HANGOVER_AMPLIFIER = 1;

    /** Duracion de la fatiga minera de la resaca ({@code mining_fatigue 6}). */
    public static final int HANGOVER_FATIGUE_TICKS = 120;

    private PillTable() {
    }

    /** Chakra que devuelve una pildora a quien tiene ese maximo. */
    public static int chakraRestored(int maxChakra) {
        return maxChakra / CHAKRA_FRACTION;
    }

    /** Indica si al bote le quedan pildoras. */
    public static boolean hasPillsLeft(int pillCount) {
        return pillCount > 0;
    }
}
