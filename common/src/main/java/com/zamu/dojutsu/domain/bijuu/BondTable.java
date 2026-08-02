package com.zamu.dojutsu.domain.bijuu;

import java.util.Map;

/**
 * <b>Vinculo</b> con la bestia sellada {@code [S09]} (F7/DJT-166): el unico progreso del juego que se mide en
 * <b>tiempo jugado con ella dentro</b>, no en actos. Verificado contra {@code jinchuriki/bond_main},
 * {@code bond_increase} y los predicados {@code bond/req_N} / {@code bf_req_N}.
 *
 * <h2>Se gana esperando, y cada bestia tarda lo suyo</h2>
 * {@code bond_main} suma <b>un tick por tick</b> mientras portas la bestia — no hace falta hacer nada. Lo que
 * cambia es cuanto hay que esperar: <b>2 horas</b> por nivel con las mas dociles (2, 3, 5, 6 y 7), <b>3</b> con
 * Shukaku, el Cuatro y el Ocho, y <b>5</b> con Kurama. El ultimo escalon (3 → 4, el que abre el Modo Baryon)
 * cuesta casi el doble: <b>8 horas</b> con el Nueve Colas.
 *
 * <h2>El nivel 3 es el que importa</h2>
 * Al llegar a 3 el mensaje cambia de "sientes que tu vinculo se fortalece" a "te has hecho amigo de
 * <b>Kurama</b>" —la bestia pasa a tener nombre— y es justo el nivel que desbloquea sus tecnicas dentro de la
 * forma ({@link JinchurikiFormTable#MOVES_LEVEL}). El 4 abre el Modo Baryon.
 *
 * <p>El <b>Diez Colas no tiene vinculo</b>: {@code main} excluye explicitamente a {@code is_jinchuriki_10} del
 * bloque de bond.
 */
public final class BondTable {

    /** Chakra maximo que suma cada subida de vinculo ({@code bond_increase}: {@code max_chakra += 50}). */
    public static final int LEVEL_UP_MAX_CHAKRA = 50;

    /** Ninja_xp por subida de vinculo. */
    public static final int LEVEL_UP_NINJA_XP = 10;

    /** Nivel en el que la bestia pasa a tener nombre propio y presta sus tecnicas. */
    public static final int FRIENDSHIP_LEVEL = 3;

    /** Ticks de vinculo por nivel hasta el 3 ({@code bond/req_N}). */
    private static final Map<TailedBeast, Integer> PER_LEVEL = Map.ofEntries(
            Map.entry(TailedBeast.ONE_TAIL, 216_000),    // 3 h
            Map.entry(TailedBeast.TWO_TAILS, 144_000),   // 2 h
            Map.entry(TailedBeast.THREE_TAILS, 144_000),
            Map.entry(TailedBeast.FOUR_TAILS, 216_000),
            Map.entry(TailedBeast.FIVE_TAILS, 144_000),
            Map.entry(TailedBeast.SIX_TAILS, 144_000),
            Map.entry(TailedBeast.SEVEN_TAILS, 144_000),
            Map.entry(TailedBeast.EIGHT_TAILS, 216_000),
            Map.entry(TailedBeast.NINE_TAILS, 360_000)); // 5 h

    /** Ticks del ultimo escalon, 3 → 4, el que abre el Baryon ({@code bond/bf_req_N}). */
    private static final Map<TailedBeast, Integer> FINAL_LEVEL = Map.ofEntries(
            Map.entry(TailedBeast.ONE_TAIL, 360_000),    // 5 h
            Map.entry(TailedBeast.TWO_TAILS, 288_000),   // 4 h
            Map.entry(TailedBeast.THREE_TAILS, 288_000),
            Map.entry(TailedBeast.FOUR_TAILS, 432_000),  // 6 h
            Map.entry(TailedBeast.FIVE_TAILS, 288_000),
            Map.entry(TailedBeast.SIX_TAILS, 288_000),
            Map.entry(TailedBeast.SEVEN_TAILS, 288_000),
            Map.entry(TailedBeast.EIGHT_TAILS, 360_000),
            Map.entry(TailedBeast.NINE_TAILS, 576_000)); // 8 h

    private BondTable() {
    }

    /**
     * Indica si esa bestia acumula vinculo. El <b>Diez Colas no</b> ({@code main} lo excluye del bloque), asi que
     * su portador se queda en el nivel que tuviera.
     */
    public static boolean hasBond(TailedBeast beast) {
        return !beast.isTenTails();
    }

    /**
     * Ticks de vinculo que hacen falta para pasar del nivel dado al siguiente, o {@code 0} si esa bestia no
     * acumula vinculo o ya esta al maximo.
     */
    public static int requiredTicks(TailedBeast beast, int currentLevel) {
        if (!hasBond(beast) || currentLevel >= JinchurikiFormTable.MAX_LEVEL) {
            return 0;
        }
        Map<TailedBeast, Integer> table = currentLevel >= FRIENDSHIP_LEVEL ? FINAL_LEVEL : PER_LEVEL;
        return table.getOrDefault(beast, 0);
    }

    /**
     * Decide (puro) si con este vinculo acumulado toca subir de nivel.
     *
     * @param beast        bestia sellada
     * @param currentLevel nivel actual
     * @param bondTicks    ticks acumulados desde la ultima subida
     */
    public static boolean levelsUp(TailedBeast beast, int currentLevel, int bondTicks) {
        int required = requiredTicks(beast, currentLevel);
        return required > 0 && bondTicks >= required;
    }
}
