package com.zamu.dojutsu.domain.sage;

/**
 * Tabla de reunion de <b>energia natural</b> {@code [S08]} (F6/DJT-135): cuanto hay que estarse quieto para
 * ganar cada punto de carga. Dominio puro y <b>verificada contra el datapack</b>
 * ({@code properties.mcfunction}: {@code GatherTimeLevel_*} y {@code GatherTimeLevel_WithOil_*};
 * {@code chakra/main}: {@code max_sagechakra 100}; {@code gather_natural_energy}: {@code +1} por pulso).
 *
 * <h2>Cargar es más rápido cuanto mayor es el nivel</h2>
 * El intervalo se <b>halva</b> en cada nivel: 800 ticks por punto en el nivel 0 (40 s) frente a 25 en el nivel 5
 * (1,25 s). El <b>aceite de sapo</b> ({@code oiled}) reduce el intervalo a la mitad otra vez.
 *
 * <h2>Antes de empezar hay que asentarse</h2>
 * El datapack exige {@value #STILL_WAIT_TICKS} ticks de quietud (60 s, {@code still_wait_1200}) <b>antes</b> de
 * que empiece a acumularse energia — quedarse quieto un instante no basta.
 */
public final class NaturalEnergyTable {

    /** Carga maxima de energia natural ({@code d.max_sagechakra}); al alcanzarla uno se vuelve sabio. */
    public static final int MAX_CHARGE = 100;

    /** Energia ganada por pulso ({@code NatureEnergyCharge d.config}). */
    public static final int CHARGE_PER_PULSE = 1;

    /** Quietud previa antes de empezar a reunir ({@code still_wait_1200}). */
    public static final int STILL_WAIT_TICKS = 1200;

    /** Nivel maximo de sabio {@code [S08]}. */
    public static final int MAX_SAGE_LEVEL = 5;

    /** Ticks quieto por punto de energia, por nivel 0..5 ({@code GatherTimeLevel_N}). */
    private static final int[] GATHER_TICKS = {800, 400, 200, 100, 50, 25};

    /** Igual, con aceite de sapo ({@code GatherTimeLevel_WithOil_N}): la mitad, salvo el ultimo (12, no 12.5). */
    private static final int[] GATHER_TICKS_OILED = {400, 200, 100, 50, 25, 12};

    private NaturalEnergyTable() {
    }

    /**
     * Ticks que hay que estar quieto para ganar {@link #CHARGE_PER_PULSE} de energia.
     *
     * @param sageLevel nivel de sabio 0..5 (0 = aun no lo es); se <b>clampa</b> al rango valido
     * @param oiled     lleva aceite de sapo encima ({@code is_oiled}): reduce el intervalo a la mitad
     */
    public static int gatherTicks(int sageLevel, boolean oiled) {
        int level = Math.min(Math.max(sageLevel, 0), MAX_SAGE_LEVEL);
        return oiled ? GATHER_TICKS_OILED[level] : GATHER_TICKS[level];
    }

    /** {@code true} si la carga alcanzo el maximo: es el momento de volverse sabio ({@code is_at_max}). */
    public static boolean isFullyCharged(int charge) {
        return charge >= MAX_CHARGE;
    }
}
