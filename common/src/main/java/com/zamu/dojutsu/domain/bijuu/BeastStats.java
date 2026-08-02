package com.zamu.dojutsu.domain.bijuu;

/**
 * Estadisticas y recompensas de las Bestias con Cola {@code [S09]} (F7/DJT-160). Dominio puro y
 * <b>verificado contra el datapack</b> ({@code tailed_beasts/spawn/spawn_tb_entity}, {@code tailed_beasts/main},
 * {@code tailed_beasts/death}).
 *
 * <h2>Las diez comparten cuerpo</h2>
 * {@code spawn_tb_entity} es una sola funcion para las diez: {@value #MAX_HEALTH} de vida, {@value #ARMOR} de
 * armadura, {@value #FOLLOW_RANGE} de alcance de vision y resistencia al fuego permanente. Lo que cambia entre
 * bestias es la identidad y la tecnica insignia, no el cuerpo — por eso son un enum y no diez entidades.
 *
 * <h2>Solo pelean si hay alguien delante</h2>
 * La IA solo corre con un jugador a {@value #AI_ACTIVATION_RADIUS} bloques o menos ({@code main}); el temporizador
 * ni siquiera baja si no hay nadie. El Diez Colas ataca <b>cada {@value #AI_INTERVAL_TEN_TAILS} ticks</b> frente a
 * los {@value #AI_INTERVAL_TICKS} del resto.
 *
 * <h2>Matarla reparte, y reparte por colas</h2>
 * {@code death} da a <b>todos</b> los jugadores a {@value #REWARD_RADIUS} bloques {@link #ninjaXpReward} de
 * ninja_xp y un orbe de {@link #experienceReward} de experiencia, ambos crecientes con el numero de colas. Nadie
 * "roba" la recompensa: se la lleva todo el que estaba cerca.
 */
public final class BeastStats {

    /** Vida base de cualquier bestia ({@code max_health base set 1024}). */
    public static final double MAX_HEALTH = 1024.0;

    /** Armadura base ({@code armor base set 30}). */
    public static final double ARMOR = 30.0;

    /** Alcance con el que detecta objetivos ({@code follow_range base set 64}). */
    public static final double FOLLOW_RANGE = 64.0;

    /**
     * Velocidad base ({@code movement_speed base set 1}).
     *
     * <p><b>Divergencia consciente:</b> 1.0 es ~4 veces la de un ravager vanilla (0.3) y ~8 la de un jugador
     * andando; una bestia asi es literalmente inescapable. Se porta como <b>atributo declarado</b> igual que el
     * datapack, pero queda anotado: si en balance resulta injugable, es esta constante.
     */
    public static final double MOVEMENT_SPEED = 1.0;

    /** Daño base del golpe cuerpo a cuerpo (el del ravager que el datapack reskinea). */
    public static final double ATTACK_DAMAGE = 12.0;

    /** Resistencia de empuje: una bestia no sale volando de un golpe. */
    public static final double KNOCKBACK_RESISTANCE = 1.0;

    /** Radio dentro del cual un jugador despierta la IA de la bestia ({@code main}). */
    public static final double AI_ACTIVATION_RADIUS = 32.0;

    /** Ticks entre decisiones de la IA ({@code d.tbai_timer = 60}). */
    public static final int AI_INTERVAL_TICKS = 60;

    /** Ticks entre decisiones del Diez Colas: ataca la mitad de espaciado ({@code d.tbai_timer = 40}). */
    public static final int AI_INTERVAL_TEN_TAILS = 40;

    /** Radio dentro del cual se reparte la recompensa al morir ({@code death}: {@code @a[distance=0..20]}). */
    public static final double REWARD_RADIUS = 20.0;

    /** Radio en el que se muestra la barra de jefe ({@code spawn/*}: {@code @a[distance=0..100]}). */
    public static final double BOSS_BAR_RADIUS = 100.0;

    /**
     * Ancho de la caja de colision de una bestia <b>de una cola</b>, en bloques: el de la malla del ravager que
     * se usa prestada. No es un numero del datapack — alli la bestia <b>era</b> un ravager y heredaba su caja.
     */
    public static final float BASE_WIDTH = 1.95F;

    /** Alto de la caja de colision de una bestia de una cola, en bloques. */
    public static final float BASE_HEIGHT = 2.2F;

    /** Escala de una bestia de una cola. */
    public static final float BASE_SCALE = 1.4F;

    /** Cuanto crece la bestia por cada cola adicional. */
    public static final float SCALE_PER_TAIL = 0.2F;

    /** Base de ninja_xp al morir ({@code death}: {@code 90 + 10 * colas}). */
    private static final int NINJA_XP_BASE = 90;

    /** Ninja_xp extra por cola. */
    private static final int NINJA_XP_PER_TAIL = 10;

    /** Base de experiencia vanilla al morir ({@code death}: {@code 1000 + 100 * colas}). */
    private static final int EXPERIENCE_BASE = 1000;

    /** Experiencia extra por cola. */
    private static final int EXPERIENCE_PER_TAIL = 100;

    private BeastStats() {
    }

    /**
     * Escala visual <b>y de colision</b> de una bestia (F10, hallazgo de la verificacion en partida).
     *
     * <h2>Por que esto es una funcion y no un numero en el renderer</h2>
     * El renderer dibujaba la bestia a esta escala, pero la caja de colision se quedaba en el
     * {@code sized(2.0, 2.6)} del {@code EntityType}, que es fijo. Un Nueve Colas se veia a escala <b>3,0</b>
     * —casi seis bloques de ancho— con una caja de dos bloques pegada a sus pies: se le golpeaba el cuerpo y
     * <b>no recibia daño</b>, porque ahi no habia nada que golpear.
     *
     * <p>La causa no era el numero: era tenerlo <b>en dos sitios</b> y aplicar solo uno. Ahora vive aqui y lo
     * leen los dos ({@code TailedBeastEntity.getDimensions} y {@code TailedBeastRenderer.scale}).
     */
    public static float renderScale(TailedBeast beast) {
        return BASE_SCALE + SCALE_PER_TAIL * (beast.tails() - 1);
    }

    /** Ancho de la caja de colision de esa bestia, en bloques. */
    public static float width(TailedBeast beast) {
        return BASE_WIDTH * renderScale(beast);
    }

    /** Alto de la caja de colision de esa bestia, en bloques. */
    public static float height(TailedBeast beast) {
        return BASE_HEIGHT * renderScale(beast);
    }

    /** Ticks entre decisiones de la IA de esa bestia. */
    public static int aiIntervalTicks(TailedBeast beast) {
        return beast.isTenTails() ? AI_INTERVAL_TEN_TAILS : AI_INTERVAL_TICKS;
    }

    /** Ninja_xp que recibe cada jugador cercano al morir la bestia. */
    public static int ninjaXpReward(TailedBeast beast) {
        return NINJA_XP_BASE + NINJA_XP_PER_TAIL * beast.tails();
    }

    /** Experiencia vanilla del orbe que recibe cada jugador cercano. */
    public static int experienceReward(TailedBeast beast) {
        return EXPERIENCE_BASE + EXPERIENCE_PER_TAIL * beast.tails();
    }
}
