package com.zamu.dojutsu.world.crafting;

import com.zamu.dojutsu.domain.crafting.DropRecipe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Registro de combinaciones de drop-crafting cargadas desde {@code data/dojutsu/drop_crafting/*.json}
 * ({@link DropCraftingReloadListener}). Mismo patron que {@code TradeRegistry}.
 *
 * <p>Se guardan <b>ordenadas de mas ingredientes a menos</b>: si una receta es subconjunto de otra, gana la mas
 * especifica. Ordenar aqui —y no en cada tick— es lo que permite que el servicio se limite a recorrer la lista.
 */
public final class DropCraftingRegistry {

    /** Recetas cargadas, ordenadas por especificidad descendente. */
    private static volatile List<Entry> recipes = List.of();

    /** Una receta con el id del fichero del que salio (para el log). */
    public record Entry(String id, DropRecipe recipe) {
    }

    private DropCraftingRegistry() {
    }

    /** Reemplaza el contenido del registro (carga y {@code /reload}). */
    public static void load(Map<String, DropRecipe> loaded) {
        List<Entry> sorted = new ArrayList<>(loaded.size());
        loaded.forEach((id, recipe) -> sorted.add(new Entry(id, recipe)));
        sorted.sort(Comparator.comparingInt((Entry entry) -> entry.recipe().specificity()).reversed()
                .thenComparing(Entry::id));
        recipes = List.copyOf(sorted);
    }

    /** Recetas cargadas, de mas especifica a menos. */
    public static List<Entry> all() {
        return recipes;
    }

    /** Numero de recetas cargadas (log de arranque y comando de QA). */
    public static int size() {
        return recipes.size();
    }
}
