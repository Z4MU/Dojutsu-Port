package com.zamu.dojutsu.jutsu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.network.JutsuBookEntry;
import com.zamu.dojutsu.network.JutsuBookEntry.AffinityView;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Construye la vista del libro de jutsus (F11/DJT-235): las fichas de las tecnicas que el jugador
 * <b>conoce</b>, con los numeros del {@code JutsuRegistry}.
 *
 * <p>Vive en el servidor porque el registro es data-driven y solo el servidor lo tiene. Es <b>solo lectura</b>:
 * no muta nada, y quien coloca en los huecos es {@link JutsuSelectionService}.
 */
public final class JutsuBookService {

    /** Clave del dano dentro de {@code effectParams}. La declaran 79 de las 128 tecnicas del datapack. */
    private static final String DAMAGE_KEY = "damage";

    private JutsuBookService() {
    }

    /**
     * Fichas de lo que el jugador conoce, en orden estable.
     *
     * <p>Un jutsu conocido cuyo id ya no exista en el registro —datapack cambiado entre partidas— se
     * <b>omite</b> en vez de romper la vista: el estado sigue recordandolo, pero el libro no puede mostrar
     * numeros que no tiene.
     */
    public static List<JutsuBookEntry> entriesFor(PlayerNinjaData data) {
        JutsuKnowledge knowledge = data.jutsu();
        List<JutsuBookEntry> entries = new ArrayList<>(knowledge.known().size());
        for (String id : knowledge.known()) {
            JutsuRegistry.get(id).map(JutsuBookService::toEntry).ifPresent(entries::add);
        }
        return entries;
    }

    /** Aplana una entrada del registro a lo que el libro necesita pintar. */
    static JutsuBookEntry toEntry(JutsuEntry entry) {
        var definition = entry.definition();
        Optional<AffinityView> affinity = definition.affinityRequirement()
                .map(requirement -> new AffinityView(requirement.natureId(), requirement.amount()));
        return new JutsuBookEntry(entry.id(), definition.chakraCost(), definition.cooldownTicks(),
                definition.category(), affinity, damageOf(entry.effectParams()));
    }

    /**
     * Dano declarado en los parametros del efecto, o {@code 0} si esa familia no lo declara.
     *
     * <p>Se lee del JSON en vez de vivir en {@code JutsuDefinition} porque <b>no es un dato universal</b>:
     * un buff o una teleportacion no tienen dano, y subirlo al contrato obligaria a inventarle un valor a
     * medio catalogo. Aqui es informativo y el libro lo omite cuando es cero.
     */
    static float damageOf(JsonElement effectParams) {
        if (effectParams == null || !effectParams.isJsonObject()) {
            return 0F;
        }
        JsonObject object = effectParams.getAsJsonObject();
        if (!object.has(DAMAGE_KEY)) {
            return 0F;
        }
        try {
            return object.get(DAMAGE_KEY).getAsFloat();
        } catch (RuntimeException malformed) {
            return 0F; // parametro con otro tipo: el libro se dibuja igual, sin dano
        }
    }
}
