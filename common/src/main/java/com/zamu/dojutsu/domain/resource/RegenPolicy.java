package com.zamu.dojutsu.domain.resource;

import java.util.Set;

/**
 * Politica de regeneracion "timer->pulso" del chakra {@code [S02]}.
 *
 * <p>El datapack implementa la regen como un <b>divisor de frecuencia</b>: acumula ticks en un timer
 * y, al alcanzar un <em>umbral</em>, suma un <em>incremento</em> de chakra y resetea el timer. Esta
 * clase encapsula ese mecanismo (via {@link #tick()}) y la resolucion de la tabla de umbrales e
 * incrementos por factores (via {@link #resolve(Set)}), reproduciendo exactamente los numeros de
 * {@code [S02]}. La tasa efectiva es {@code chakra/seg = incremento x (20/umbral)}.
 *
 * <p>El umbral y el incremento son inmutables; solo el timer es estado transitorio de regen. Dominio
 * puro: sin imports de Minecraft ni de loader.
 *
 * <h2>Tabla de regen (modo x clase), fiel a {@code [S02]}</h2>
 * <pre>
 *   modo\clase   ESTANDAR   OTSUTSUKI   UZUMAKI   HASHIRAMA
 *   ninguno      120/+3     60/+4       60/+6     60/+8
 *   sage 1        60/+4     30/+5       30/+7     30/+9
 *   sage 2        30/+4     15/+5       15/+7     15/+9
 * </pre>
 *
 * <p><b>Resolucion de combinaciones</b> (no especificada en {@code [S02]}; decision de diseno
 * coherente con "mejor clan/DNA y mayor modo -> regen mas veloz"): si concurren varias clases de
 * clan/DNA gana la mas fuerte (Hashirama &gt; Uzumaki &gt; Otsutsuki); si concurren ambos modos gana
 * el mas alto (Sage 2 &gt; Sage 1).
 */
public final class RegenPolicy {

    /** Ticks por segundo de Minecraft; base de la formula de tasa. */
    public static final int TICKS_PER_SECOND = 20;

    private final int thresholdTicks;
    private final int increment;
    private int timer;

    /**
     * @param thresholdTicks ticks necesarios para un pulso (debe ser &gt; 0)
     * @param increment      chakra sumado por pulso (no puede ser negativo)
     * @throws IllegalArgumentException si {@code thresholdTicks <= 0} o {@code increment < 0}
     */
    public RegenPolicy(int thresholdTicks, int increment) {
        if (thresholdTicks <= 0) {
            throw new IllegalArgumentException("thresholdTicks debe ser > 0: " + thresholdTicks);
        }
        if (increment < 0) {
            throw new IllegalArgumentException("increment no puede ser negativo: " + increment);
        }
        this.thresholdTicks = thresholdTicks;
        this.increment = increment;
    }

    /**
     * Resuelve la politica (umbral + incremento) para un conjunto de factores segun la tabla de
     * {@code [S02]}. Un conjunto vacio equivale a la regen estandar (120/+3).
     */
    public static RegenPolicy resolve(Set<RegenFactor> factors) {
        SageLevel mode = sageLevel(factors);
        RegenClass clazz = regenClass(factors);
        return switch (mode) {
            case NONE -> switch (clazz) {
                case STANDARD -> new RegenPolicy(120, 3);
                case OTSUTSUKI -> new RegenPolicy(60, 4);
                case UZUMAKI -> new RegenPolicy(60, 6);
                case HASHIRAMA -> new RegenPolicy(60, 8);
            };
            case SAGE_1 -> switch (clazz) {
                case STANDARD -> new RegenPolicy(60, 4);
                case OTSUTSUKI -> new RegenPolicy(30, 5);
                case UZUMAKI -> new RegenPolicy(30, 7);
                case HASHIRAMA -> new RegenPolicy(30, 9);
            };
            case SAGE_2 -> switch (clazz) {
                case STANDARD -> new RegenPolicy(30, 4);
                case OTSUTSUKI -> new RegenPolicy(15, 5);
                case UZUMAKI -> new RegenPolicy(15, 7);
                case HASHIRAMA -> new RegenPolicy(15, 9);
            };
        };
    }

    /**
     * Avanza el timer un tick. Al alcanzar el umbral, resetea el timer y devuelve el incremento del
     * pulso; en caso contrario devuelve 0.
     *
     * @return el chakra a sumar en este tick (0 salvo en el tick de pulso)
     */
    public int tick() {
        timer++;
        if (timer >= thresholdTicks) {
            timer = 0;
            return increment;
        }
        return 0;
    }

    /** Reinicia el timer de regen (p. ej. al cambiar de modo/clase). */
    public void resetTimer() {
        timer = 0;
    }

    public int timer() {
        return timer;
    }

    public int thresholdTicks() {
        return thresholdTicks;
    }

    public int increment() {
        return increment;
    }

    /** Tasa efectiva de regen: {@code incremento x (20/umbral)} chakra por segundo. */
    public double chakraPerSecond() {
        return increment * (double) TICKS_PER_SECOND / thresholdTicks;
    }

    private static SageLevel sageLevel(Set<RegenFactor> factors) {
        if (factors.contains(RegenFactor.SAGE_MODE_2)) {
            return SageLevel.SAGE_2;
        }
        if (factors.contains(RegenFactor.SAGE_MODE_1)) {
            return SageLevel.SAGE_1;
        }
        return SageLevel.NONE;
    }

    private static RegenClass regenClass(Set<RegenFactor> factors) {
        if (factors.contains(RegenFactor.HASHIRAMA_DNA)) {
            return RegenClass.HASHIRAMA;
        }
        if (factors.contains(RegenFactor.CLAN_UZUMAKI)) {
            return RegenClass.UZUMAKI;
        }
        if (factors.contains(RegenFactor.CLAN_OTSUTSUKI)) {
            return RegenClass.OTSUTSUKI;
        }
        return RegenClass.STANDARD;
    }

    @Override
    public String toString() {
        return "RegenPolicy[umbral=" + thresholdTicks + ", incremento=" + increment + "]";
    }

    /** Nivel de modo sabio que afecta a la regen. */
    private enum SageLevel {
        NONE, SAGE_1, SAGE_2
    }

    /** Clase de regen por clan/DNA (el {@code d.diff_cr} del datapack). */
    private enum RegenClass {
        STANDARD, OTSUTSUKI, UZUMAKI, HASHIRAMA
    }
}
