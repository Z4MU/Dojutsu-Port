package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;

/**
 * Sub-estado de onboarding del ninja {@code [S01]}: en que fase del flujo de creacion esta el jugador
 * (seleccionar clan -> seleccionar afinidad -> finalizado).
 *
 * <p>Es un <b>value object inmutable</b> y sirve de ejemplo del patron de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}: expone {@link #DEFAULT} y {@link #CODEC}, y se serializa
 * como un compound propio (sus claves viven bajo su propia rama del NBT). Se reemplaza entero (withers),
 * nunca se muta en sitio.
 *
 * @param phase fase actual del onboarding
 */
public record OnboardingState(Phase phase) {

    /** Estado por defecto de un jugador nuevo: aun no ha empezado el onboarding. */
    public static final OnboardingState DEFAULT = new OnboardingState(Phase.NOT_STARTED);

    /** Codec del sub-estado (compound tolerante: fase ausente -> NOT_STARTED). */
    public static final Codec<OnboardingState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Phase.CODEC.optionalFieldOf("phase", Phase.NOT_STARTED).forGetter(OnboardingState::phase)
    ).apply(instance, OnboardingState::new));

    public OnboardingState {
        Objects.requireNonNull(phase, "phase");
    }

    /** Indica si el onboarding esta completado. */
    public boolean completed() {
        return phase == Phase.COMPLETED;
    }

    /** Devuelve una copia con otra fase (inmutable). */
    public OnboardingState withPhase(Phase next) {
        return new OnboardingState(next);
    }

    /** Fases del flujo de creacion del ninja ({@code join_main} del datapack). */
    public enum Phase {
        NOT_STARTED,
        SELECTING_CLAN,
        SELECTING_AFFINITY,
        COMPLETED;

        /** Codec tolerante: un nombre desconocido (NBT corrupto) cae a {@link #NOT_STARTED}. */
        static final Codec<Phase> CODEC = Codec.STRING.xmap(Phase::byName, Phase::name);

        private static Phase byName(String name) {
            for (Phase phase : values()) {
                if (phase.name().equals(name)) {
                    return phase;
                }
            }
            return NOT_STARTED;
        }
    }
}
