package com.zamu.dojutsu.domain.sage;

/**
 * Tabla del <b>modo sabio activo</b> {@code [S08]} (F6/DJT-136): que hace falta para encenderlo, cuanto dura y
 * que exige subir de nivel. Dominio puro y <b>verificada contra el datapack</b> ({@code sage/toggle_check} para
 * las puertas, {@code sage/main} para el drenaje, {@code predicates/sage_mode/sm_lvlreq_*} para los niveles).
 *
 * <h2>El nivel alarga el modo, no lo abarata</h2>
 * Sostenerlo cuesta <b>1 de energia natural</b> siempre; lo que cambia es <b>cada cuanto</b>: 100 ticks en el
 * nivel 1 frente a 500 en el nivel 5. Es decir, con la carga llena el nivel 1 dura ~8 min y el nivel 5 ~42.
 *
 * <h2>Encenderlo exige mas energia cuanto menor es el nivel</h2>
 * El nivel 1 solo puede activarse con la carga <b>al maximo</b> (100); el 2 con 50, el 3 con 25 y el 4 con 1. Es
 * la curva de maestria: un sabio novato necesita reunir del todo para entrar en modo.
 */
public final class SageModeTable {

    /** Energia minima para <b>encender</b> el modo, por nivel 1..5 ({@code toggle_check}). */
    private static final int[] ACTIVATION_MINIMUM = {100, 50, 25, 1, 1};

    /** Ticks entre cada -1 de energia mientras esta activo, por nivel 1..5 ({@code sage_timer}). */
    private static final int[] DRAIN_INTERVAL = {100, 200, 300, 400, 500};

    /** Energia que cuesta cada pulso de mantenimiento. */
    public static final int DRAIN_PER_PULSE = 1;

    /** Uso del modo para optar al nivel 1..4 ({@code sage_use_72k}). */
    public static final int USE_FOR_EARLY_LEVELS = 72000;

    /** Uso del modo para optar al nivel 5 ({@code sage_use_144k}). */
    public static final int USE_FOR_MAX_LEVEL = 144000;

    private SageModeTable() {
    }

    /**
     * Requisitos <b>compuestos</b> para alcanzar un nivel de sabio ({@code sm_lvlreq_N}): hay que cumplir los
     * tres. El nivel 1 solo exige uso; del 2 en adelante tambien chakra maximo y Senjutsu.
     *
     * @param maxChakra chakra maximo minimo ({@code d.max_chakra})
     * @param senjutsu  nivel minimo de la habilidad Senjutsu ({@code d.senjutsu})
     * @param use       uso acumulado del modo minimo ({@code d.sage_use})
     */
    public record LevelRequirement(int maxChakra, int senjutsu, int use) {
    }

    /** Requisitos para <b>subir</b> al nivel dado (2..5); el 1 solo exige {@link #USE_FOR_EARLY_LEVELS} de uso. */
    private static final LevelRequirement[] LEVEL_REQUIREMENTS = {
            new LevelRequirement(0, 0, USE_FOR_EARLY_LEVELS),          // -> nivel 1
            new LevelRequirement(7000, 300, USE_FOR_EARLY_LEVELS),     // -> nivel 2
            new LevelRequirement(8000, 600, USE_FOR_EARLY_LEVELS),     // -> nivel 3
            new LevelRequirement(9000, 900, USE_FOR_EARLY_LEVELS),     // -> nivel 4
            new LevelRequirement(10000, 1200, USE_FOR_MAX_LEVEL)};     // -> nivel 5

    /** Energia minima para encender el modo en ese nivel (nivel fuera de rango se clampa). */
    public static int activationMinimum(int level) {
        return ACTIVATION_MINIMUM[clampLevelIndex(level)];
    }

    /** Ticks entre cobros de energia en ese nivel (nivel fuera de rango se clampa). */
    public static int drainInterval(int level) {
        return DRAIN_INTERVAL[clampLevelIndex(level)];
    }

    /** Requisitos para subir al nivel dado (1..5); {@code null} si el nivel esta fuera de rango. */
    public static LevelRequirement requirementFor(int targetLevel) {
        if (targetLevel < 1 || targetLevel > NaturalEnergyTable.MAX_SAGE_LEVEL) {
            return null;
        }
        return LEVEL_REQUIREMENTS[targetLevel - 1];
    }

    /**
     * Nivel resultante tras cumplir requisitos: sube <b>de uno en uno</b> si el siguiente nivel es alcanzable.
     * Decision pura.
     *
     * @param level     nivel actual 0..5
     * @param maxChakra chakra maximo del jugador
     * @param senjutsu  nivel de Senjutsu del jugador
     * @param use       uso acumulado del modo
     * @return el nivel nuevo, o el mismo si aun no toca subir
     */
    public static int levelAfter(int level, int maxChakra, int senjutsu, int use) {
        if (level >= NaturalEnergyTable.MAX_SAGE_LEVEL) {
            return level;
        }
        LevelRequirement next = requirementFor(level + 1);
        if (next == null) {
            return level;
        }
        boolean met = maxChakra >= next.maxChakra() && senjutsu >= next.senjutsu() && use >= next.use();
        return met ? level + 1 : level;
    }

    /** Indice 0..4 del nivel 1..5 (clampado). */
    private static int clampLevelIndex(int level) {
        return Math.min(Math.max(level, 1), NaturalEnergyTable.MAX_SAGE_LEVEL) - 1;
    }
}
