package com.zamu.dojutsu.domain.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Una combinacion de <b>drop-crafting</b> {@code [S23]} (F9/DJT-206): los items que hay que soltar juntos en el
 * suelo y lo que sale. Data-driven desde {@code data/dojutsu/drop_crafting/*.json}.
 *
 * <h2>Por que sigue existiendo el drop-crafting en un mod</h2>
 * No por NBT —eso lo resuelve una receta normal— sino por <b>capacidad</b>: una mascara pide 11 unidades y una
 * mesa 3x3 solo admite 9 ({@link CraftingTable}). Lo que si desaparece del datapack es el rodeo del
 * {@code knowledge_book} de las recetas de mesa.
 *
 * <h2>La cantidad de pila es parte de la receta, no un detalle</h2>
 * El datapack empareja por {@code nbt={Item:{id:...,Count:Nb}}}, o sea <b>id y cantidad exacta de la pila</b>.
 * No es decorativo: {@code anbu_1}, {@code anbu_2} y {@code anbu_3} llevan <b>los mismos cuatro ingredientes</b>
 * y solo se distinguen por cuanto tinte rojo hay (1, 4 y 2+verde 4). Aflojar la cantidad a "al menos N"
 * fusionaria tres mascaras en una.
 *
 * <p>Por eso {@link #match(List)} exige cantidad <b>exacta</b> por pila y consume una <b>entidad distinta</b>
 * por ingrediente. Lo que si se afloja es la <b>distancia</b> (RB-2), que vive en el servicio.
 *
 * <h2>Un resultado que no existe no rompe nada</h2>
 * El resultado se declara por <b>id de registro</b>, como en el catalogo de comercio: si el item aun no esta
 * implementado la receta se carga igual y solo se salta al emparejar. Asi las <b>9 combinaciones de huevos de
 * bestia</b> —cuyo item nunca se creo— quedan declaradas y <b>se encienden solas</b> el dia que exista, sin
 * tocar codigo.
 *
 * @param ingredients lo que hay que soltar; cada entrada = <b>una</b> entidad item con esa cantidad exacta
 * @param result      id de registro de lo que sale ({@code "dojutsu:mask_anbu_1"})
 * @param resultCount unidades del resultado
 */
public record DropRecipe(List<Ingredient> ingredients, String result, int resultCount) {

    /** Un ingrediente: un item soltado con una cantidad de pila exacta. */
    public record Ingredient(String item, int count) {

        public static final Codec<Ingredient> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("item").forGetter(Ingredient::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(Ingredient::count)
        ).apply(instance, Ingredient::new));

        public Ingredient {
            Objects.requireNonNull(item, "item");
            count = Math.max(count, 1);
        }
    }

    /**
     * Una entidad item candidata, vista como datos puros: que es y cuantas unidades lleva la pila. El servicio
     * traduce cada {@code ItemEntity} cercano a una de estas antes de emparejar.
     */
    public record Stack(String item, int count) {

        public Stack {
            Objects.requireNonNull(item, "item");
        }
    }

    public static final Codec<DropRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(DropRecipe::ingredients),
            Codec.STRING.fieldOf("result").forGetter(DropRecipe::result),
            Codec.INT.optionalFieldOf("result_count", 1).forGetter(DropRecipe::resultCount)
    ).apply(instance, DropRecipe::new));

    public DropRecipe {
        Objects.requireNonNull(ingredients, "ingredients");
        Objects.requireNonNull(result, "result");
        ingredients = List.copyOf(ingredients);
        resultCount = Math.max(resultCount, 1);
    }

    /**
     * Intenta casar la receta con las pilas que hay alrededor.
     *
     * @param nearby pilas candidatas, en el orden en que las vio el servicio
     * @return los <b>indices</b> de {@code nearby} que consume la receta (uno por ingrediente, sin repetir), o
     *         vacio si no casa
     */
    public Optional<List<Integer>> match(List<Stack> nearby) {
        List<Integer> chosen = new ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            int found = -1;
            for (int i = 0; i < nearby.size(); i++) {
                if (chosen.contains(i)) {
                    continue;
                }
                Stack candidate = nearby.get(i);
                if (candidate.item().equals(ingredient.item()) && candidate.count() == ingredient.count()) {
                    found = i;
                    break;
                }
            }
            if (found < 0) {
                return Optional.empty();
            }
            chosen.add(found);
        }
        return Optional.of(List.copyOf(chosen));
    }

    /**
     * Cuantos ingredientes pide. Se emparejan las recetas <b>de mas ingredientes primero</b> para que la mas
     * especifica gane cuando una es subconjunto de otra (por ejemplo {@code tobi_1}, que solo pide tablones de
     * acacia y lana negra).
     */
    public int specificity() {
        return ingredients.size();
    }
}
