package com.zamu.dojutsu.domain.trading;

import java.util.random.RandomGenerator;

/**
 * Reglas de los comerciantes ninja {@code [S18]} (F8/DJT-190). Dominio puro y verificado contra
 * {@code main/internal/setup_custom_entities}, {@code trades/setup_shinobi} y {@code trader/{init,try}}.
 *
 * <h2>Solo uno de cada diez aldeanos es un shinobi</h2>
 * {@code setup_shinobi} tira {@code trades_rng} ({@code rolls 0..19}, veinte resultados) y convierte con
 * {@code matches 0..1}: <b>{@value #SHINOBI_CHANCE_PERCENT}%</b>. Es lo que hace que encontrar uno signifique
 * algo — si todos vendieran kunai, no serian shinobi, serian una tienda.
 *
 * <h2>El ritmo del mercader es "veces por dia", no "cada N ticks"</h2>
 * {@code trader/try} programa el siguiente intento a <b>{@value #DAY_TICKS} / rate</b> ticks: la config no dice
 * cada cuanto aparece, dice <b>cuantas oportunidades hay al dia</b>. Con rate 1 son 24 000 ticks (un dia entero);
 * con 24, uno cada 1 000. La tabla de 24 lineas del datapack es literalmente esa division hecha a mano.
 *
 * <h2>Y la "probabilidad" va al reves de lo que parece</h2>
 * {@code traderspawnchance} tira en el rango <b>[chance, 10]</b> y gana el <b>10</b>. O sea que
 * {@code TraderSpawnChance = 10} es <b>certeza</b> y {@code = 1} es una de cada diez: el numero no es la
 * probabilidad, es <b>cuanto se acerca al techo</b>. Facil de portar del reves, asi que va con test propio.
 */
public final class TradingTable {

    /** Resultados de la tirada de conversion ({@code trades_rng}: {@code rolls 0..19}). */
    public static final int SHINOBI_ROLL_OUTCOMES = 20;

    /** Resultados que convierten al aldeano ({@code matches 0..1}). */
    public static final int SHINOBI_WINNING_ROLLS = 2;

    /** Probabilidad de que un aldeano nuevo sea un shinobi. */
    public static final int SHINOBI_CHANCE_PERCENT =
            SHINOBI_WINNING_ROLLS * 100 / SHINOBI_ROLL_OUTCOMES;

    /** Ticks de un dia de Minecraft; la base del ritmo del mercader. */
    public static final int DAY_TICKS = 24_000;

    /** Oportunidades por dia maximas ({@code init}: {@code traderspawnrate_25p → 24}). */
    public static final int MAX_SPAWN_RATE = 24;

    /** Techo de la tirada de aparicion: el valor que gana ({@code try}: {@code matches 10}). */
    public static final int SPAWN_CHANCE_CEILING = 10;

    /** Nivel de la profesion del shinobi villager ({@code setup_shinobi}: {@code librarian} nivel 5). */
    public static final int SHINOBI_VILLAGER_LEVEL = 5;

    private TradingTable() {
    }

    /** Decide (puro) si una tirada de {@code 0..19} convierte al aldeano en shinobi. */
    public static boolean isShinobiRoll(int roll) {
        return roll < SHINOBI_WINNING_ROLLS;
    }

    /**
     * Tira la conversion de un aldeano: {@code true} si le toca ser shinobi. Azar inyectado (testeable).
     *
     * <p>El glue no usa esta sobrecarga porque el {@code RandomSource} de Minecraft no es un
     * {@link RandomGenerator}: llama a {@link #isShinobiRoll} con la tirada ya hecha, que es la misma fuente de
     * verdad.
     */
    public static boolean rollShinobi(RandomGenerator random) {
        return isShinobiRoll(random.nextInt(SHINOBI_ROLL_OUTCOMES));
    }

    /**
     * Ticks entre intentos de aparicion del mercader, a partir de las oportunidades por dia configuradas.
     * {@code rate <= 0} desactiva el ritmo propio (devuelve {@code 0}: no se programa nada).
     */
    public static int attemptIntervalTicks(int rate) {
        if (rate <= 0) {
            return 0;
        }
        return DAY_TICKS / Math.min(rate, MAX_SPAWN_RATE);
    }

    /**
     * Tira la aparicion del mercader en un intento.
     *
     * <p><b>Ojo con la direccion:</b> el rango es {@code [chance, 10]} y gana el {@value #SPAWN_CHANCE_CEILING},
     * asi que {@code chance = 10} <b>siempre</b> acierta y {@code chance = 1} acierta una de cada diez.
     */
    public static boolean rollSpawn(int chance, RandomGenerator random) {
        return isSpawnRoll(chance, random.nextInt(spawnRollSpan(chance)));
    }

    /** Caras del dado de aparicion para esa configuracion: {@code 10 - chance + 1}. */
    public static int spawnRollSpan(int chance) {
        int floor = Math.min(Math.max(chance, 1), SPAWN_CHANCE_CEILING);
        return SPAWN_CHANCE_CEILING - floor + 1;
    }

    /** Decide (puro) si un desplazamiento ya sorteado sobre {@code [chance, 10]} acierta el techo. */
    public static boolean isSpawnRoll(int chance, int drawn) {
        int floor = Math.min(Math.max(chance, 1), SPAWN_CHANCE_CEILING);
        return floor + drawn == SPAWN_CHANCE_CEILING;
    }

    /** Probabilidad de aparicion de un intento, en tanto por uno (para el comando de QA). */
    public static double spawnProbability(int chance) {
        int floor = Math.min(Math.max(chance, 1), SPAWN_CHANCE_CEILING);
        return 1.0 / (SPAWN_CHANCE_CEILING - floor + 1);
    }
}
