package com.zamu.dojutsu.domain.ai;

import java.util.Optional;

/**
 * Decision de combate de un shinobi NPC {@code [S17]} (F8/DJT-185): que hace en su turno.
 *
 * <h2>Este es el unico sistema del proyecto que hay que DISEÑAR, no portar</h2>
 * La ficha lo avisaba (<b>RM-5</b>) y el datapack lo confirma: {@code shinobi/ai/get_move} tiene <b>seis
 * ramas</b> y solo dos hacen algo — lanzar un kunai y un {@code great_fireball}. Las otras cuatro son
 * literalmente {@code say do nothing (0)} y {@code say cast stronger jutsu}. Y {@code ai/get_jutsu}, que lista 30
 * jutsus, <b>no se llama desde ningun sitio</b>: es codigo muerto con un {@code say} pegado a cada cast. Portarlo
 * literalmente seria portar un depurador a medias.
 *
 * <h2>Lo que SI dice el datapack, y se respeta</h2>
 * Aunque el codigo este a medias, el <b>ritmo</b> esta decidido y es el mismo que el de las bestias
 * (F7/DJT-161), lo que no puede ser casualidad:
 * <ul>
 *   <li>Decide cada {@value #DECISION_INTERVAL_TICKS} ticks, y <b>solo</b> con un jugador a
 *       {@value #ACTIVATION_RADIUS} bloques.</li>
 *   <li>Tira el mismo dado {@code ai_rng} de <b>0 a 5</b>, y <b>la mitad de los turnos no hace nada</b>
 *       ({@code say do nothing} en el 0, el 4 y el 5).</li>
 *   <li>El 1 lanza kunai, el 2 un jutsu "debil" y el 3 uno "mas fuerte" — eso lo dicen los propios
 *       {@code say}, asi que la <b>intencion</b> esta declarada aunque el codigo falte.</li>
 * </ul>
 *
 * <h2>Lo genuinamente nuevo: la distancia</h2>
 * El datapack no mira a que distancia esta el objetivo, asi que un shinobi pegado a ti te lanzaria una bola de
 * fuego a los pies. Aqui la banda de distancia <b>filtra</b> la accion que salio del dado: de cerca pelea a mano,
 * a media distancia tira kunai y de lejos lanza jutsus. El dado sigue mandando <b>cuando</b> actua; la distancia
 * decide <b>que</b> tiene sentido.
 */
public final class ShinobiCombatPlan {

    /** Ticks entre decisiones ({@code ai/main}: {@code d.ai_timer 60}). */
    public static final int DECISION_INTERVAL_TICKS = 60;

    /** Radio en el que un jugador despierta la IA ({@code ai/main}: {@code @p[distance=0..32]}). */
    public static final double ACTIVATION_RADIUS = 32.0;

    /** Valor minimo de la tirada ({@code ai_rng}: 0..5, la misma tabla que las bestias). */
    public static final int ROLL_MIN = 0;

    /** Valor maximo de la tirada. */
    public static final int ROLL_MAX = 5;

    /** Resultados distintos de la tirada (6). */
    public static final int ROLL_OUTCOMES = ROLL_MAX - ROLL_MIN + 1;

    /** Hasta aqui pelea con las manos (<b>diseño nuevo</b>). */
    public static final double MELEE_RANGE = 4.0;

    /** Hasta aqui tira arrojadizas; mas alla, jutsus (<b>diseño nuevo</b>). */
    public static final double THROWING_RANGE = 16.0;

    /** Que va a hacer el shinobi este turno. */
    public enum Action {
        /** Nada: se toma el turno libre. Es lo mas frecuente, y a proposito. */
        IDLE,
        /** Acercarse y pegar (lo resuelve el goal de melee vanilla). */
        MELEE,
        /** Lanzar un kunai. */
        THROW_KUNAI,
        /** Lanzar el jutsu mas debil que sepa. */
        WEAK_JUTSU,
        /** Lanzar el mas fuerte que sepa. */
        STRONG_JUTSU
    }

    private ShinobiCombatPlan() {
    }

    /** Banda de distancia a la que esta el objetivo. */
    public enum Band {
        /** Encima: pelea de cerca. */
        MELEE,
        /** Media: arrojadizas. */
        THROWING,
        /** Lejos (pero dentro del radio de activacion): jutsus. */
        RANGED
    }

    /** Banda a la que corresponde una distancia. */
    public static Band bandFor(double distance) {
        if (distance <= MELEE_RANGE) {
            return Band.MELEE;
        }
        if (distance <= THROWING_RANGE) {
            return Band.THROWING;
        }
        return Band.RANGED;
    }

    /**
     * Elige la accion del turno.
     *
     * <p>Primero manda el <b>dado del datapack</b> (que decide si actua y con que intencion) y despues la
     * <b>banda de distancia</b> filtra lo que tiene sentido: la intencion "jutsu" a bocajarro se convierte en
     * pegar, y la de "kunai" desde lejos, en jutsu.
     *
     * @param level    nivel de amenaza; fija que jutsus sabe
     * @param roll     tirada {@value #ROLL_MIN}..{@value #ROLL_MAX}
     * @param distance distancia al objetivo
     */
    public static Action choose(ThreatLevel level, int roll, double distance) {
        Band band = bandFor(distance);
        Action intent = switch (roll) {
            case 1 -> Action.THROW_KUNAI;
            case 2 -> Action.WEAK_JUTSU;
            case 3 -> Action.STRONG_JUTSU;
            default -> Action.IDLE; // 0, 4 y 5: la mitad de los turnos, como el datapack
        };
        if (intent == Action.IDLE) {
            return Action.IDLE;
        }
        return adapt(intent, band, level);
    }

    /** Adapta la intencion a la distancia y a lo que el shinobi sabe hacer. */
    private static Action adapt(Action intent, Band band, ThreatLevel level) {
        if (band == Band.MELEE) {
            return Action.MELEE; // a bocajarro no se lanzan bolas de fuego a los pies
        }
        boolean jutsu = intent == Action.WEAK_JUTSU || intent == Action.STRONG_JUTSU;
        if (jutsu && level.jutsus().isEmpty()) {
            return Action.THROW_KUNAI; // un academico no sabe ninguno: tira kunai
        }
        if (band == Band.THROWING) {
            return intent; // media distancia: vale todo
        }
        // lejos: un kunai no llega, asi que se convierte en jutsu (o en nada si no sabe)
        if (intent == Action.THROW_KUNAI) {
            return level.jutsus().isEmpty() ? Action.IDLE : Action.WEAK_JUTSU;
        }
        return intent;
    }

    /** Id del jutsu que corresponde a una accion, o vacio si la accion no lanza ninguno. */
    public static Optional<String> jutsuFor(Action action, ThreatLevel level) {
        return switch (action) {
            case WEAK_JUTSU -> level.weakJutsu();
            case STRONG_JUTSU -> level.strongJutsu();
            default -> Optional.empty();
        };
    }
}
