package com.zamu.dojutsu.domain.summoning;

/**
 * Reglas del <b>Jutsu de Invocacion</b> {@code [S15]} (F8/DJT-175). Dominio puro y verificado contra
 * {@code jutsu/summoning/cast}, {@code summoning/attempt_form_contract} y el predicado
 * {@code summoning/max_time}.
 *
 * <h2>Invocar no cuesta chakra</h2>
 * La ficha hablaba de "invocar por tier (<b>coste</b>)". No hay ninguna linea de chakra en
 * {@code jutsu/summoning/cast}: lo que se paga es el <b>tiempo</b>. El veto arranca en
 * {@value #COOLDOWN_LOW_FUINJUTSU} ticks (30 s) y baja a {@value #COOLDOWN_HIGH_FUINJUTSU} (10 s) a medida que
 * sube el fuinjutsu. El recurso de un maestro del sellado es su propia maestria, no su chakra.
 *
 * <h2>Hay dos suelos de fuinjutsu, y el datapack no los cuadra</h2>
 * El tier bajo se desbloquea a fuinjutsu {@code 300} del datapack ({@value SummonTier#MID_FUINJUTSU}/2 en escala
 * del mod, o sea {@value #TIER_LOW_FUINJUTSU}), pero {@code cast} exige {@code 500}
 * (= {@value #CAST_MIN_FUINJUTSU} aqui) antes de invocar nada. Entre esas dos cifras el jugador pasa la puerta
 * del tier y <b>despues</b> recibe "Requires higher Fuinjutsu!". Es una inconsistencia del datapack; el suelo
 * efectivo es {@value #CAST_MIN_FUINJUTSU} y se porta asi, dejandolo anotado.
 *
 * <h2>La invocacion dura un minuto</h2>
 * {@value #LIFETIME_TICKS} ticks y desaparece. El datapack lo consigue con <b>dos selectores globales por
 * tick</b> sobre {@code @e[type=#dojutsu:contractable,tag=d.summon]} — uno para sumar el contador y otro para
 * matar a los vencidos. Ese es exactamente el riesgo <b>S15-R3</b>, y aqui no hace falta: la caducidad la lleva
 * la propia invocacion (ver {@code SummonService}).
 */
public final class SummonTable {

    /** Ordinal de rango ninja minimo para firmar cualquier contrato ({@code d.ninja_rank >= 2} = Chunin). */
    public static final int CONTRACT_MIN_RANK = 2;

    /** Ordinal de rango para el tier alto ({@code d.ninja_rank >= 3} = Tokubetsu Jonin). */
    public static final int HIGH_TIER_RANK = 3;

    /** Fuinjutsu del tier bajo en escala del mod ({@code can_use_low}: datapack 300 ÷ 4). */
    public static final int TIER_LOW_FUINJUTSU = 75;

    /** Fuinjutsu que exige el cast en escala del mod ({@code cast}: datapack 500 ÷ 4). El suelo real. */
    public static final int CAST_MIN_FUINJUTSU = 125;

    /** Ticks que vive una invocacion ({@code max_time}: {@code d.summon_time >= 1200}). */
    public static final int LIFETIME_TICKS = 1_200;

    /** Veto entre invocaciones con fuinjutsu bajo ({@code cast}: 500..1000 → 600). */
    public static final int COOLDOWN_LOW_FUINJUTSU = 600;

    /** Veto con fuinjutsu medio ({@code cast}: 1001..1500 → 400). */
    public static final int COOLDOWN_MID_FUINJUTSU = 400;

    /** Veto con fuinjutsu alto ({@code cast}: 1501..2000 → 200). */
    public static final int COOLDOWN_HIGH_FUINJUTSU = 200;

    /** Fuinjutsu (mod) a partir del cual el veto baja a {@value #COOLDOWN_MID_FUINJUTSU} (datapack 1001 ÷ 4). */
    public static final int COOLDOWN_MID_THRESHOLD = 251;

    /** Fuinjutsu (mod) a partir del cual baja a {@value #COOLDOWN_HIGH_FUINJUTSU} (datapack 1501 ÷ 4). */
    public static final int COOLDOWN_HIGH_THRESHOLD = 376;

    /** XP de fuinjutsu por invocacion ({@code setup}: {@code fuinjutsu_xp += 1}). */
    public static final int SUMMON_FUINJUTSU_XP = 1;

    private SummonTable() {
    }

    /**
     * Ticks de veto tras invocar, segun el fuinjutsu del invocador: cuanto mas sabe, menos espera. Decision
     * pura.
     */
    public static int cooldownTicks(int fuinjutsu) {
        if (fuinjutsu >= COOLDOWN_HIGH_THRESHOLD) {
            return COOLDOWN_HIGH_FUINJUTSU;
        }
        if (fuinjutsu >= COOLDOWN_MID_THRESHOLD) {
            return COOLDOWN_MID_FUINJUTSU;
        }
        return COOLDOWN_LOW_FUINJUTSU;
    }

    /** Indica si el fuinjutsu alcanza el suelo del cast, el requisito que de verdad manda. */
    public static boolean canCast(int fuinjutsu) {
        return fuinjutsu >= CAST_MIN_FUINJUTSU;
    }
}
