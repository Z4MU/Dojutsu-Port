package com.zamu.dojutsu.domain.eightgates;

/**
 * Tabla de las <b>Ocho Puertas</b> {@code [S11]} (F6/DJT-145): que puertas tienes desbloqueadas, cuanto tardas en
 * encadenar aperturas, a que ritmo se acumula y se recupera la fatiga, y que te hace pasarte de 95. Dominio puro
 * y <b>verificada contra el datapack</b> ({@code eight_gates/main}, {@code activate}, {@code get_cd},
 * {@code run}, {@code deactivate}, {@code strain/*}, {@code unlocks/eg_req_*}).
 *
 * <h2>Nada de esto se guarda: se deriva del taijutsu</h2>
 * El datapack recalcula {@code d.eg_max} y {@code d.eg_mastery} <b>cada tick</b> desde {@code d.taijutsu} en
 * {@code internal/unlocks}. Aqui son funciones puras del nivel de taijutsu ({@link #maxGate},
 * {@link #hasMastery}) en vez de campos persistidos: no pueden desincronizarse y no hay migracion que mantener.
 * Los requisitos van en <b>escala del mod</b> (datapack ÷ 4, misma convencion que kenjutsu/taijutsu desde
 * DJT-082): el 300 del datapack es {@value #ACTIVATION_TAIJUTSU} aqui, y el 2000 de la maestria es
 * {@value #MASTERY_TAIJUTSU}.
 *
 * <h2>Progresar te deja encadenar puertas mas rapido</h2>
 * {@link #openCooldownTicks} baja de 180 ticks (con una sola puerta desbloqueada) a 20 (con las ocho) y a
 * {@value #OPEN_COOLDOWN_MASTERED} dominandolas. En paralelo, {@link #firstStrainTickTicks} <b>sube</b> de 20 a
 * 65 (75 dominadas): el primer mordisco de fatiga tarda mas en llegar. Ojo: ambas tablas se indexan por la
 * puerta <b>maxima desbloqueada</b>, no por la que acabas de abrir.
 *
 * <h2>La fatiga baja siempre, tambien con las puertas cerradas</h2>
 * Cada {@link #decayPulseTicks} ticks se descuenta {@link #decayAmount}. Y el castigo de 95/98 sigue aplicandose
 * con las puertas ya cerradas: la resaca dura hasta que la fatiga baje.
 */
public final class EightGatesTable {

    /** Nivel de taijutsu (escala del mod) para abrir cualquier puerta ({@code activate}: datapack 300). */
    public static final int ACTIVATION_TAIJUTSU = 75;

    /** Nivel de taijutsu que otorga la maestria ({@code eg_req_mastery}: datapack 2000). */
    public static final int MASTERY_TAIJUTSU = 500;

    /** Ticks entre mordiscos de fatiga con una puerta abierta ({@code run}: {@code d.eg_stimer = 40}). */
    public static final int STRAIN_PULSE_TICKS = 40;

    /** Ticks de veto tras cerrar las puertas ({@code deactivate}: {@code d.eg_cd = 100}). */
    public static final int CLOSE_COOLDOWN_TICKS = 100;

    /** Ticks entre aperturas con la maestria ({@code get_cd}). */
    public static final int OPEN_COOLDOWN_MASTERED = 10;

    /** Retardo del primer mordisco de fatiga con la maestria ({@code get_cd}). */
    public static final int FIRST_STRAIN_TICK_MASTERED = 75;

    /** Ticks entre recuperaciones de fatiga para el resto del mundo ({@code d.egstimer = 200}). */
    public static final int DECAY_PULSE_TICKS = 200;

    /** Ticks entre recuperaciones <b>para el clan Lee</b> ({@code d.egstimer = 400}). Ver {@link #decayPulseTicks}. */
    public static final int DECAY_PULSE_TICKS_LEE = 400;

    /** Fatiga recuperada por pulso ({@code main}). */
    public static final int DECAY_AMOUNT = 1;

    /** Fatiga recuperada por pulso con celulas de Hashirama ({@code has_hsdna}). */
    public static final int DECAY_AMOUNT_HASHIRAMA = 3;

    /** Fatiga a partir de la cual empieza el castigo leve ({@code strain/95_effects}). */
    public static final int PUNISH_AT = 95;

    /** Fatiga a partir de la cual el castigo es severo ({@code strain/98_101_effects}). */
    public static final int PUNISH_HARD_AT = 98;

    /** Fatiga que cierra las puertas a la fuerza ({@code main}: {@code strain/100 → deactivate}). */
    public static final int FORCE_CLOSE_AT = 100;

    /** Cooldown entre aperturas por puerta maxima desbloqueada (1..8), indexado desde 0 ({@code get_cd}). */
    private static final int[] OPEN_COOLDOWN = {180, 160, 140, 120, 80, 60, 40, 20};

    /** Retardo del primer mordisco de fatiga por puerta maxima desbloqueada (1..8) ({@code get_cd}). */
    private static final int[] FIRST_STRAIN_TICK = {20, 25, 30, 45, 50, 55, 60, 65};

    private EightGatesTable() {
    }

    /**
     * Puerta maxima que el ninja puede abrir segun su taijutsu: {@code 0} (ninguna) a {@code 8}. Equivale al
     * {@code d.eg_max} que {@code internal/unlocks} recalcula desde {@code d.taijutsu}.
     */
    public static int maxGate(int taijutsuLevel) {
        int max = 0;
        for (Gate gate : Gate.values()) {
            if (taijutsuLevel >= gate.requiredTaijutsu()) {
                max = gate.stage();
            }
        }
        return max;
    }

    /** Indica si domina las Ocho Puertas ({@code d.eg_mastery = 1}). */
    public static boolean hasMastery(int taijutsuLevel) {
        return taijutsuLevel >= MASTERY_TAIJUTSU;
    }

    /** Ticks que hay que esperar entre aperturas, segun la puerta maxima desbloqueada y la maestria. */
    public static int openCooldownTicks(int maxGate, boolean mastered) {
        if (mastered) {
            return OPEN_COOLDOWN_MASTERED;
        }
        return OPEN_COOLDOWN[clampIndex(maxGate)];
    }

    /** Ticks hasta el primer mordisco de fatiga tras abrir, segun la puerta maxima desbloqueada y la maestria. */
    public static int firstStrainTickTicks(int maxGate, boolean mastered) {
        if (mastered) {
            return FIRST_STRAIN_TICK_MASTERED;
        }
        return FIRST_STRAIN_TICK[clampIndex(maxGate)];
    }

    /**
     * Ticks entre recuperaciones de fatiga.
     *
     * <p><b>Cuidado con este valor:</b> el datapack le da al <b>clan Lee</b> un pulso de
     * {@value #DECAY_PULSE_TICKS_LEE} ticks frente a los {@value #DECAY_PULSE_TICKS} del resto, lo que significa
     * que Lee recupera la <b>mitad de rapido</b> — justo lo contrario de lo que cabria esperar del clan
     * especialista en taijutsu (y de lo que dice la ficha de {@code [S11]}). Se porta <b>tal cual esta</b>
     * (regla 16: manda el datapack); si se decide que es un fallo de origen, es intercambiar dos constantes.
     */
    public static int decayPulseTicks(boolean leeClan) {
        return leeClan ? DECAY_PULSE_TICKS_LEE : DECAY_PULSE_TICKS;
    }

    /** Fatiga recuperada por pulso (3 con celulas de Hashirama, 1 sin ellas). */
    public static int decayAmount(boolean hashiramaDna) {
        return hashiramaDna ? DECAY_AMOUNT_HASHIRAMA : DECAY_AMOUNT;
    }

    /** Indica si la fatiga obliga a cerrar las puertas. */
    public static boolean forcesClose(int strain) {
        return strain >= FORCE_CLOSE_AT;
    }

    /** Indice 0..7 de una puerta maxima 1..8 (0 puertas cae en el indice de la primera). */
    private static int clampIndex(int maxGate) {
        return Math.min(Math.max(maxGate, 1), Gate.values().length) - 1;
    }
}
