package com.zamu.dojutsu.domain.story;

/**
 * Reglas del encuentro con los Sabios {@code [S19]} (F9/DJT-200). Dominio puro y verificado contra
 * {@code main/internal/hagoromo/{queue,queue_try,try,retry}} y {@code hamura/*}.
 *
 * <h2>Una sola cola para todo el servidor, no una por jugador</h2>
 * La ficha pedia "colas <b>por jugador</b>". El datapack tiene <b>una</b>: {@code queue} pone una bandera global
 * ({@code #hagoromo_q}) y programa <b>un</b> {@code schedule} a {@value #QUEUE_SECONDS} s; al dispararse,
 * {@code queue_try} corre {@code as @a} y tira los dados <b>para cada jugador conectado</b>. Cambia el diseño por
 * completo: no hay N temporizadores, hay uno que reparte oportunidades.
 *
 * <h2>La probabilidad sube si no te ha pasado</h2>
 * No es un 5% fijo: {@code try} elige la tabla segun {@code d.hagoromo_try}, el contador de intentos fallidos —
 * <b>{@value #CHANCE_FIRST}%</b> el primero, <b>{@value #CHANCE_SECOND}%</b> el segundo y
 * <b>{@value #CHANCE_ONWARDS}%</b> del tercero en adelante. Es un sistema de piedad: cuanto mas esperas, mas
 * probable es. Y {@code retry} solo incrementa el contador <b>mientras no haya llegado al tope</b>, asi que se
 * queda en el 10% para siempre.
 *
 * <h2>Y hace falta ser Kage</h2>
 * {@code hagoromo/req} exige {@code d.ninja_rank >= }{@value #REQUIRED_RANK}, que en la escala del mod es
 * <b>Kage</b>. Es, con diferencia, el requisito mas alto de todo el datapack — y la respuesta al riesgo
 * <b>S19-R4</b> ("auditar que desbloquea cada camino"): lo que desbloquea es enorme, pero la puerta tambien.
 *
 * <h2>Dos apariciones, y la segunda no pregunta</h2>
 * La etapa la sube <b>el encuentro</b> ({@code start}: {@code d.hagoromo += 1}), no la eleccion. Con etapa
 * {@value #STAGE_MET} el sabio ya te hablo una vez; con {@value #STAGE_DONE} el {@code try} queda vetado para
 * siempre ({@code hagoromo_2}). Y el <b>segundo</b> dialogo <b>no ofrece opciones</b>: se despide y te da la mitad
 * que te falte ({@code gain_other_half}). Por eso rechazar no cierra nada — {@code no_path} devuelve la etapa a
 * cero y vuelves a la casilla de salida.
 *
 * <h2>Un encuentro exitoso deja el contador en 1, no en 0</h2>
 * {@code try} llama a {@code start} (que pone {@code d.hagoromo_try 0}) y <b>despues</b> a {@code retry}
 * incondicionalmente, que lo incrementa. Asi que tras aparecerse, la proxima tirada es al
 * {@value #CHANCE_SECOND}%, no al {@value #CHANCE_FIRST}%. Es un efecto del orden de las llamadas, no una regla
 * escrita — pero es el comportamiento real y se porta tal cual ({@link #attemptsAfterEncounter()}).
 */
public final class StoryTable {

    /** Segundos entre disparos de la cola global ({@code queue}: {@code schedule ... 3600s}). */
    public static final int QUEUE_SECONDS = 3600;

    /** Ticks entre disparos de la cola (una hora de juego real). */
    public static final int QUEUE_TICKS = QUEUE_SECONDS * 20;

    /** Ordinal de rango ninja minimo ({@code req}: {@code d.ninja_rank >= 6} = Kage). */
    public static final int REQUIRED_RANK = 6;

    /** Probabilidad del primer intento, en porcentaje ({@code 5_percent}). */
    public static final int CHANCE_FIRST = 5;

    /** Probabilidad del segundo intento ({@code 8_percent}). */
    public static final int CHANCE_SECOND = 8;

    /** Probabilidad del tercero en adelante ({@code 10_percent}); es el tope. */
    public static final int CHANCE_ONWARDS = 10;

    /** Intentos fallidos a partir de los cuales la probabilidad se queda en el tope ({@code hagoromo_try_2p}). */
    public static final int ATTEMPTS_AT_MAX = 2;

    /** Ninja_xp que da elegir un camino ({@code yin_path}/{@code yang_path}: {@code ninja_xp += 100}). */
    public static final int PATH_NINJA_XP = 100;

    /** Etapa tras la primera charla; a partir de aqui el dialogo es el "segundo" ({@code hagoromo_1p}). */
    public static final int STAGE_MET = 1;

    /** Etapa final: ya se aparecio dos veces y no vuelve ({@code hagoromo_2} veta el {@code try}). */
    public static final int STAGE_DONE = 2;

    private StoryTable() {
    }

    /** Probabilidad (en porcentaje) del proximo intento, segun los fallidos acumulados. */
    public static int chanceFor(int failedAttempts) {
        if (failedAttempts >= ATTEMPTS_AT_MAX) {
            return CHANCE_ONWARDS;
        }
        return failedAttempts == 1 ? CHANCE_SECOND : CHANCE_FIRST;
    }

    /**
     * Cuenta un intento fallido. El contador <b>deja de subir</b> al llegar al tope ({@code retry}: solo
     * incrementa {@code unless hagoromo_try_2p}), asi que la probabilidad se queda en el 10%.
     */
    public static int nextAttempts(int failedAttempts) {
        return failedAttempts >= ATTEMPTS_AT_MAX ? failedAttempts : failedAttempts + 1;
    }

    /**
     * Contador de intentos con el que queda un jugador <b>justo despues</b> de un encuentro: {@code start} lo
     * pone a cero y el {@code retry} posterior lo sube a uno.
     */
    public static int attemptsAfterEncounter() {
        return 1;
    }

    /** Indica si el rango alcanza para tener un encuentro. */
    public static boolean rankAllows(int rankOrdinal) {
        return rankOrdinal >= REQUIRED_RANK;
    }

    /** Decide (puro) si una tirada de {@code 1..100} acierta esa probabilidad. */
    public static boolean isHit(int roll, int chancePercent) {
        return roll <= chancePercent;
    }
}
