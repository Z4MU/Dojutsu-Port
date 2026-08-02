package com.zamu.dojutsu.domain.items;

import java.util.Locale;
import java.util.Optional;

/**
 * Los tres <b>pergaminos de un solo uso</b> {@code [S14]} (F10/DJT-225). Verificados en
 * {@code items/medical_scroll/restore.mcfunction}, {@code items/sage_scroll/restore.mcfunction} y
 * {@code jinchuriki/removal.mcfunction} (el de retirada vive bajo {@code jinchuriki/}, no bajo {@code items/}).
 *
 * <h2>Los tres se usan igual y se gastan igual, pero fallan distinto</h2>
 * Clic derecho, efecto, y {@code clear ... 1} salvo en creativo. La diferencia esta en la <b>puerta</b>:
 * <ul>
 *   <li>{@link #MEDICAL} no tiene ninguna — el filtro esta en el <b>crafteo</b> ({@code d.medical >= 300}).</li>
 *   <li>{@link #SAGE} si la tiene: si no eres sabio avisa en rojo y suena un yunque, y <b>no</b> se gasta.</li>
 *   <li>{@link #REMOVAL} la tiene implicita: si no eres jinchuriki no hay nada que quitar y no se gasta.</li>
 * </ul>
 *
 * <h2>El pergamino medico tambien cura los ojos</h2>
 * {@code medical_scroll/restore} hace dos cosas, no una: llena el chakra <b>y</b> pone
 * {@code d.eye_strain} a 0. Es la unica via, aparte de dormir, para quitarse de encima la fatiga ocular.
 */
public enum ScrollType {

    /** Pergamino Medico: llena el chakra y borra la fatiga ocular. */
    MEDICAL,
    /** Pergamino de Retirada de Bestia con Cola: te extrae la bestia sellada. */
    REMOVAL,
    /** Pergamino de Sabio: llena la energia natural. Solo si ya eres sabio. */
    SAGE;

    /** Id data-driven y path del item ({@code "medical"} -> {@code dojutsu:medical_scroll}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Path del item registrado ({@code "medical_scroll"}). */
    public String itemPath() {
        return id() + "_scroll";
    }

    /** Resuelve un pergamino por su id; vacio si no existe (estado tolerante). */
    public static Optional<ScrollType> byId(String id) {
        for (ScrollType type : values()) {
            if (type.id().equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
