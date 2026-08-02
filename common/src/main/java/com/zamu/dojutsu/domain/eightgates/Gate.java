package com.zamu.dojutsu.domain.eightgates;

import java.util.Locale;
import java.util.Optional;

/**
 * Las ocho <b>Puertas Internas</b> {@code [S11]} (F6/DJT-145): la escalera de potenciacion fisica del taijutsu.
 * Verificadas en {@code eight_gates/run} (efectos y ganancia de fatiga), {@code eight_gates/open/gate_of_*}
 * (orden) y {@code unlocks/eg_req_N} (requisito de taijutsu).
 *
 * <p><b>Los efectos son {@code MobEffect}, no atributos.</b> El datapack refresca {@code strength}/{@code speed}/
 * {@code jump_boost} con duracion 1 tick cada tick que la puerta esta abierta; no toca ningun atributo. Eso
 * <b>disuelve el riesgo S11-R4</b> ("atributos pegados"): un efecto que caduca solo no puede quedarse pegado. Es
 * el mismo principio que el {@code AbilityAnchor} de F4: caducidad por construccion.
 *
 * <p>La escalada es brutal y deliberada: de Fuerza I sin mas (Apertura) a <b>Fuerza VIII + Velocidad XXV +
 * Salto X</b> en la Puerta de la Muerte — pagado con fatiga cuatro veces mas rapida.
 *
 * @param stage       valor de {@code d.eg_stage} (1..8) y orden de apertura
 * @param strength    amplificador de fuerza mientras esta abierta ({@link #NONE} = no la da)
 * @param speed       amplificador de velocidad ({@link #NONE} = no la da)
 * @param jumpBoost   amplificador de salto ({@link #NONE} = no lo da)
 * @param taijutsu    nivel de taijutsu que la desbloquea, en <b>escala del mod</b> (datapack / 4)
 * @param strainGain  fatiga por pulso mientras es la puerta abierta
 * @param strainGainMastered idem con las Ocho Puertas dominadas ({@code d.eg_mastery})
 */
public enum Gate {

    /** 1ª, Apertura: solo fuerza. La primera prueba, casi inofensiva. */
    OPENING(1, 0, -1, -1, 75, 1, 1),
    /** 2ª, Curacion: entra la velocidad, y entra de golpe (VII). */
    HEALING(2, 1, 6, -1, 125, 1, 1),
    /** 3ª, Vida: aparece el salto. */
    LIFE(3, 2, 8, 4, 175, 1, 1),
    /** 4ª, Dolor: la fatiga <b>dobla</b> su ritmo a partir de aqui. */
    PAIN(4, 3, 10, 6, 225, 2, 2),
    /** 5ª, Limite: Velocidad XVI. */
    LIMIT(5, 4, 15, 6, 275, 2, 2),
    /** 6ª, Vision: Velocidad XXI. */
    VIEW(6, 5, 20, 7, 325, 2, 2),
    /** 7ª, Maravilla: dominar las puertas <b>abarata</b> la fatiga a partir de aqui (3 → 2). */
    WONDER(7, 6, 22, 8, 375, 3, 2),
    /** 8ª, Muerte: el techo absoluto y la fatiga mas rapida (4, o 3 dominada). Habilita Night Guy. */
    DEATH(8, 7, 24, 9, 425, 4, 3);

    /** La puerta no otorga ese efecto. */
    public static final int NONE = -1;

    private final int stage;
    private final int strength;
    private final int speed;
    private final int jumpBoost;
    private final int taijutsu;
    private final int strainGain;
    private final int strainGainMastered;

    Gate(int stage, int strength, int speed, int jumpBoost, int taijutsu, int strainGain, int strainGainMastered) {
        this.stage = stage;
        this.strength = strength;
        this.speed = speed;
        this.jumpBoost = jumpBoost;
        this.taijutsu = taijutsu;
        this.strainGain = strainGain;
        this.strainGainMastered = strainGainMastered;
    }

    /** Valor de {@code d.eg_stage} (1..8). */
    public int stage() {
        return stage;
    }

    /** Id data-driven en minusculas ({@code "opening"}, {@code "death"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Amplificador de fuerza, o {@link #NONE}. */
    public int strength() {
        return strength;
    }

    /** Amplificador de velocidad, o {@link #NONE}. */
    public int speed() {
        return speed;
    }

    /** Amplificador de salto, o {@link #NONE}. */
    public int jumpBoost() {
        return jumpBoost;
    }

    /** Nivel de taijutsu (escala del mod) que la desbloquea. */
    public int requiredTaijutsu() {
        return taijutsu;
    }

    /** Fatiga que suma cada pulso mientras esta es la puerta abierta. */
    public int strainGain(boolean mastered) {
        return mastered ? strainGainMastered : strainGain;
    }

    /** La puerta de una etapa {@code d.eg_stage}, o vacio si esta fuera de 1..8 (0 = puertas cerradas). */
    public static Optional<Gate> forStage(int stage) {
        for (Gate gate : values()) {
            if (gate.stage == stage) {
                return Optional.of(gate);
            }
        }
        return Optional.empty();
    }
}
