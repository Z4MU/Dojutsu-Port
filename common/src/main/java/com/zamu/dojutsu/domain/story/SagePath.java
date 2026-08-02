package com.zamu.dojutsu.domain.story;

import java.util.Locale;
import java.util.Optional;

/**
 * Los tres caminos que ofrece un Sabio {@code [S19]} (F9/DJT-200). Verificados en
 * {@code hagoromo/{yin_path,yang_path,no_path}}.
 *
 * <h2>Auditoría de S19-R4: esto es exactamente lo que da cada camino</h2>
 * <ul>
 *   <li><b>Yin</b> — chakra de los Seis Caminos + <b>despierta el ojo del sabio</b> (Rinnegan con Hagoromo,
 *       Tenseigan con Hamura) + {@value StoryTable#PATH_NINJA_XP} ninja_xp.</li>
 *   <li><b>Yang</b> — chakra de los Seis Caminos + naturaleza <b>Yin-Yang</b> + <b>Modo Sabio de los Seis
 *       Caminos</b> + {@value StoryTable#PATH_NINJA_XP} ninja_xp.</li>
 *   <li><b>Ninguno</b> — nada, y <b>vuelve a cero</b> ({@code no_path} pone {@code d.hagoromo 0}): rechazarlo no
 *       lo cierra, te lo volveran a ofrecer.</li>
 * </ul>
 *
 * <h2>Y la eleccion no es definitiva</h2>
 * {@code gain_other_half} existe: mas adelante se puede recibir <b>la otra mitad</b> del chakra del sabio. O sea
 * que Yin y Yang no son exclusivos para siempre — son el orden en que los consigues.
 */
public enum SagePath {

    /** Mitad Yin: el poder espiritual. Despierta el ojo del sabio. */
    YIN,
    /** Mitad Yang: el poder vital. Da Yin-Yang y el Modo Sabio de los Seis Caminos. */
    YANG,
    /** Rechazarlo. No cierra la puerta: el sabio volvera a ofrecerlo. */
    NONE;

    /** Id data-driven en minusculas. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Indica si este camino concede el chakra de los Seis Caminos. */
    public boolean grantsSixPaths() {
        return this != NONE;
    }

    /** La otra mitad de este camino, o vacio si es el rechazo ({@code gain_other_half}). */
    public Optional<SagePath> otherHalf() {
        return switch (this) {
            case YIN -> Optional.of(YANG);
            case YANG -> Optional.of(YIN);
            case NONE -> Optional.empty();
        };
    }

    /** Resuelve un camino por su id; vacio si no existe (estado tolerante). */
    public static Optional<SagePath> byId(String id) {
        for (SagePath path : values()) {
            if (path.id().equals(id)) {
                return Optional.of(path);
            }
        }
        return Optional.empty();
    }
}
