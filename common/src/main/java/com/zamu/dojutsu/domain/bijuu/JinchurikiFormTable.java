package com.zamu.dojutsu.domain.bijuu;

/**
 * Numeros de la <b>forma de jinchuriki</b> y del <b>Modo Baryon</b> {@code [S09]} (F7/DJT-166). Dominio puro y
 * verificado contra {@code jinchuriki/form/activate}, {@code deactivate}, {@code toggle}, {@code effects},
 * {@code main} y {@code baryon_mode/*}.
 *
 * <h2>El aura la fija el vinculo, no la bestia</h2>
 * {@code effects.mcfunction} reparte fuerza/velocidad/salto/resistencia/resistencia al fuego <b>por nivel de
 * jinchuriki</b>, no por bestia: llevar al Nueve Colas con vinculo 1 te da exactamente lo mismo que llevar a
 * Shukaku con vinculo 1. Lo que cambia entre bestias es el <b>color de las particulas</b> y las <b>tecnicas</b>.
 * Por eso el aura es una tabla de cuatro filas y no de diez.
 *
 * <h2>Las tecnicas llegan tarde</h2>
 * {@code form/main} solo ejecuta el bloque de tecnicas de la bestia con {@code jinchuriki_lvl >= }
 * {@value #MOVES_LEVEL}. Los primeros dos niveles de forma son <b>solo aura</b>: la potencia llega antes que el
 * arsenal.
 *
 * <h2>El Baryon te cuesta la bestia</h2>
 * No es un modo caro, es un modo <b>terminal</b>: dura {@value #BARYON_DURATION_TICKS} ticks (10 minutos) y al
 * agotarse {@code out_of_time} <b>extrae la bestia</b> — la despedida ("This is farewell...") es literal. Se
 * modela aparte en {@code BaryonOutcome} para que el servicio no pueda olvidarlo.
 */
public final class JinchurikiFormTable {

    /** Chakra maximo que suma entrar en forma ({@code form/activate}: {@code max_chakra += 100}). */
    public static final int FORM_MAX_CHAKRA_BONUS = 100;

    /** Chakra por debajo del cual la forma cae sola ({@code main}: {@code unless chakra >= 10}). */
    public static final int FORM_FALL_CHAKRA = 10;

    /** Ticks de veto tras salir de la forma ({@code toggle}: {@code d.jin_formcd = 60}). */
    public static final int FORM_COOLDOWN_TICKS = 60;

    /** Nivel de vinculo maximo ({@code main}: {@code lvl_5p → set 4}). */
    public static final int MAX_LEVEL = 4;

    /** Nivel a partir del cual la forma habilita las tecnicas de la bestia ({@code form/main}). */
    public static final int MOVES_LEVEL = 3;

    /** Nivel necesario para el Modo Baryon ({@code baryon_mode/toggle}: {@code lvl >= 4}). */
    public static final int BARYON_LEVEL = 4;

    /** Chakra minimo para encender el Baryon ({@code baryon_mode/toggle}: {@code chakra >= 200}). */
    public static final int BARYON_MIN_CHAKRA = 200;

    /** Ticks que dura el Baryon antes de llevarse la bestia ({@code d.baryon_use = 12000}). */
    public static final int BARYON_DURATION_TICKS = 12_000;

    /** Veto tras <b>encender</b> el Baryon ({@code d.baryon_cd = 100}). */
    public static final int BARYON_COOLDOWN_ON = 100;

    /** Veto tras <b>apagarlo</b> ({@code d.baryon_cd = 1200}). Doce veces mas largo. */
    public static final int BARYON_COOLDOWN_OFF = 1_200;

    /** Aura de la forma: amplificadores de fuerza, velocidad, salto, resistencia y resistencia al fuego. */
    public record FormAura(int strength, int speed, int jumpBoost, int resistance, int fireResistance) {
    }

    /** Aura de los niveles 0 y 1 ({@code jinchuriki_lvl_0_1}). */
    private static final FormAura LEVEL_0_1 = new FormAura(0, 4, 1, 0, 0);

    /** Aura del nivel 2. */
    private static final FormAura LEVEL_2 = new FormAura(1, 5, 3, 1, 1);

    /** Aura del nivel 3 en adelante. */
    private static final FormAura LEVEL_3_PLUS = new FormAura(2, 6, 5, 2, 2);

    /** Aura del Modo Baryon: sustituye a la del nivel, no se suma ({@code unless in_baryon_mode}). */
    private static final FormAura BARYON = new FormAura(6, 12, 10, 6, 6);

    private JinchurikiFormTable() {
    }

    /**
     * Aura que corresponde a este momento. El Baryon <b>reemplaza</b> al aura del nivel (en el datapack, cada
     * linea del aula normal lleva {@code unless in_baryon_mode}), no se acumula con ella.
     */
    public static FormAura auraFor(int level, boolean baryonActive) {
        if (baryonActive) {
            return BARYON;
        }
        if (level >= MOVES_LEVEL) {
            return LEVEL_3_PLUS;
        }
        if (level == 2) {
            return LEVEL_2;
        }
        return LEVEL_0_1;
    }

    /** Indica si ese nivel de vinculo ya habilita las tecnicas de la bestia dentro de la forma. */
    public static boolean movesUnlocked(int level) {
        return level >= MOVES_LEVEL;
    }

    /** Indica si la forma debe caer por falta de chakra. */
    public static boolean fallsForChakra(int chakra) {
        return chakra < FORM_FALL_CHAKRA;
    }
}
