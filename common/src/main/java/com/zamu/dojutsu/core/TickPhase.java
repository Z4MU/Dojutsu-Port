package com.zamu.dojutsu.core;

/**
 * Fases del tick por jugador, en el <b>orden invariante</b> del datapack ({@code [S01 §4]} /
 * {@code [ADD/04 §5]}). Cada {@link PlayerTickSystem} declara su fase; el {@link PlayerTickPipeline} los
 * ejecuta en este orden.
 *
 * <p><b>No reordenar sin verificar el invariante de comportamiento:</b> {@link #CHAKRA} debe ir primero
 * (repone el recurso antes de cualquier consumo); {@link #PROGRESSION}/{@link #UNLOCKS} van al final. La
 * lista esta completa desde el dia 1 aunque aun no existan todos los sistemas (chakra llega en F2), para que
 * anadir un sistema en fases futuras no tenga que tocar {@code core}: solo elige su fase.
 */
public enum TickPhase {

    /** Regeneracion de chakra. Debe correr ANTES que cualquier consumo. */
    CHAKRA,
    /** Fatiga (eye/eight-gates strain). */
    STRAIN,
    /** Caminar sobre el agua. */
    WATER_WALK,
    /** Decremento de cooldowns/timers de jutsus. */
    JUTSU_TIMERS,
    /** Ojos (dojutsu): coste pasivo, habilidades activas. */
    DOJUTSU,
    /** Modo Sabio (senjutsu). */
    SAGE,
    /** Potenciacion (sellos, ocho puertas, SOAH). */
    EMPOWERMENT,
    /** Jinchuriki (formas, boost de bestia). */
    JINCHURIKI,
    /** Armas arrojadizas. */
    THROWING,
    /** Invocaciones. */
    SUMMONING,
    /** Nivelacion automatica por XP. */
    PROGRESSION,
    /** Desbloqueos por hito. */
    UNLOCKS
}
