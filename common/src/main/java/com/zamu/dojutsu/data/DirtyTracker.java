package com.zamu.dojutsu.data;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Registra que campos observables ({@link ObservableField}) han cambiado desde el ultimo sync, para enviar
 * <b>deltas</b> (solo lo cambiado) en vez del snapshot completo.
 *
 * <p>Es estado <b>transitorio</b> por jugador (no se persiste): vive mientras el jugador esta conectado. Las
 * puertas de escritura de {@link PlayerNinjaData} lo marcan; el sync lo lee y lo limpia.
 */
public final class DirtyTracker {

    private final EnumSet<ObservableField> dirty = EnumSet.noneOf(ObservableField.class);

    /** Marca un campo como cambiado. */
    public void markDirty(ObservableField field) {
        dirty.add(Objects.requireNonNull(field, "field"));
    }

    /** Indica si un campo esta marcado como cambiado. */
    public boolean isDirty(ObservableField field) {
        return dirty.contains(field);
    }

    /** Indica si no hay ningun campo cambiado. */
    public boolean isEmpty() {
        return dirty.isEmpty();
    }

    /** Copia inmutable de los campos cambiados. */
    public Set<ObservableField> dirtyFields() {
        return Set.copyOf(dirty);
    }

    /** Limpia todas las marcas (tras enviar el sync). */
    public void clear() {
        dirty.clear();
    }
}
