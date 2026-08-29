package com.zamu.dojutsu.jutsu;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import java.util.Objects;
import java.util.Optional;

/**
 * Seleccion del jutsu que lanza la tecla de cast (F11/DJT-231). Hermano de {@code EyeMenuService.select}: el
 * cliente manda la <b>intencion</b> (id <b>por nombre</b>, nunca un indice de su lista) y aqui se decide.
 *
 * <p><b>Por que existe una capa entre el payload y el estado.</b> {@link JutsuKnowledge} ya garantiza la
 * invariante "no puedes tener seleccionado lo que no conoces" descartando en el constructor. Eso protege el
 * dato, pero <b>silenciosamente</b>: el handler no sabria si la seleccion prendio o se cayo, y no podria
 * distinguir "no lo conoces" de "ya lo tenias" para responder al jugador. Este servicio devuelve ese resultado
 * tipado; el estado sigue siendo la ultima palabra.
 */
public final class JutsuSelectionService {

    /** Resultado de un intento de seleccion, para que el handler responda al jugador y sepa si sincronizar. */
    public enum SelectOutcome {
        /** Seleccion aplicada: el estado cambio y hay que empujar el delta. */
        SELECTED,
        /** El jugador no conoce ese jutsu: se rechaza sin efecto (payload manipulado o UI desfasada). */
        NOT_KNOWN,
        /** Ya estaba seleccionado: sin cambio, sin sync. */
        UNCHANGED,
        /** Seleccion retirada. */
        CLEARED
    }

    private JutsuSelectionService() {
    }

    /**
     * Selecciona un jutsu aprendido. No comprueba chakra ni enfriamiento a proposito: <b>seleccionar no es
     * lanzar</b>: el jugador elige su tecnica y la lanza cuando puede, y las puertas siguen aplicandose en
     * {@link JutsuCastPipeline} en el momento del cast.
     */
    public static SelectOutcome select(PlayerNinjaData data, String jutsuId) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(jutsuId, "jutsuId");
        JutsuKnowledge knowledge = data.jutsu();
        if (!knowledge.knows(jutsuId)) {
            return SelectOutcome.NOT_KNOWN;
        }
        if (knowledge.selectedJutsu().filter(jutsuId::equals).isPresent()) {
            return SelectOutcome.UNCHANGED;
        }
        data.setJutsu(knowledge.withSelected(jutsuId));
        return SelectOutcome.SELECTED;
    }

    /** Retira la seleccion (la rueda permite dejar el hueco vacio). */
    public static SelectOutcome clear(PlayerNinjaData data) {
        Objects.requireNonNull(data, "data");
        JutsuKnowledge knowledge = data.jutsu();
        if (knowledge.selectedJutsu().isEmpty()) {
            return SelectOutcome.UNCHANGED;
        }
        data.setJutsu(knowledge.withoutSelected());
        return SelectOutcome.CLEARED;
    }

    /** Jutsu que lanzaria la tecla de cast ahora mismo, ya validado contra los conocidos. */
    public static Optional<String> selected(PlayerNinjaData data) {
        return data.jutsu().selectedJutsu();
    }
}
