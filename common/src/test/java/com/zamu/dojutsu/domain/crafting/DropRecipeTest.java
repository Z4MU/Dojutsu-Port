package com.zamu.dojutsu.domain.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.crafting.DropRecipe.Ingredient;
import com.zamu.dojutsu.domain.crafting.DropRecipe.Stack;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests del emparejamiento del drop-crafting (F9/DJT-206). Verificado contra
 * {@code main/internal/crafting.mcfunction}.
 *
 * <p>El valor que se porta del reves con mas facilidad es la <b>cantidad de pila</b>: es lo unico que separa
 * las tres mascaras ANBU, asi que tiene test propio en las dos direcciones.
 */
class DropRecipeTest {

    /** {@code masks/anbu_1}: oak_planks 5 + black_wool 3 + white_dye 3 + red_dye 1. */
    private static final DropRecipe ANBU_1 = new DropRecipe(List.of(
            new Ingredient("minecraft:oak_planks", 5),
            new Ingredient("minecraft:black_wool", 3),
            new Ingredient("minecraft:white_dye", 3),
            new Ingredient("minecraft:red_dye", 1)), "dojutsu:mask_anbu_1", 1);

    /** {@code masks/anbu_2}: identica salvo el tinte rojo, que son 4. */
    private static final DropRecipe ANBU_2 = new DropRecipe(List.of(
            new Ingredient("minecraft:oak_planks", 5),
            new Ingredient("minecraft:black_wool", 3),
            new Ingredient("minecraft:white_dye", 3),
            new Ingredient("minecraft:red_dye", 4)), "dojutsu:mask_anbu_2", 1);

    /** {@code masks/tobi_1}: solo dos ingredientes, y es subconjunto de nada mas. */
    private static final DropRecipe TOBI_1 = new DropRecipe(List.of(
            new Ingredient("minecraft:acacia_planks", 5),
            new Ingredient("minecraft:black_wool", 3)), "dojutsu:mask_tobi_1", 1);

    private static List<Stack> anbuPile(int redDye) {
        return List.of(
                new Stack("minecraft:oak_planks", 5),
                new Stack("minecraft:black_wool", 3),
                new Stack("minecraft:white_dye", 3),
                new Stack("minecraft:red_dye", redDye));
    }

    @Test
    void lasTresMascarasAnbuSoloSeDistinguenPorLaCantidadDeTinte() {
        assertTrue(ANBU_1.match(anbuPile(1)).isPresent());
        assertTrue(ANBU_1.match(anbuPile(4)).isEmpty(), "1 tinte rojo no es 4: anbu_1 no debe casar");
        assertTrue(ANBU_2.match(anbuPile(4)).isPresent());
        assertTrue(ANBU_2.match(anbuPile(1)).isEmpty(), "4 tintes rojos no son 1: anbu_2 no debe casar");
    }

    @Test
    void laCantidadDePilaEsExactaNoUnMinimo() {
        // sobrarle una unidad a la pila la invalida, igual que el nbt={Count:5b} del datapack
        List<Stack> conTablonDeMas = List.of(
                new Stack("minecraft:oak_planks", 6),
                new Stack("minecraft:black_wool", 3),
                new Stack("minecraft:white_dye", 3),
                new Stack("minecraft:red_dye", 1));
        assertTrue(ANBU_1.match(conTablonDeMas).isEmpty());
    }

    @Test
    void devuelveLosIndicesDeLasEntidadesQueConsume() {
        // orden distinto al de la receta: el emparejamiento es por conjunto, no posicional
        List<Stack> desordenado = List.of(
                new Stack("minecraft:red_dye", 1),
                new Stack("minecraft:white_dye", 3),
                new Stack("minecraft:oak_planks", 5),
                new Stack("minecraft:black_wool", 3));
        Optional<List<Integer>> matched = ANBU_1.match(desordenado);
        assertTrue(matched.isPresent());
        assertEquals(List.of(2, 3, 1, 0), matched.get());
    }

    @Test
    void noReutilizaLaMismaEntidadParaDosIngredientes() {
        // una sola pila de lana negra no puede cubrir dos ingredientes de lana negra
        DropRecipe dobleLana = new DropRecipe(List.of(
                new Ingredient("minecraft:black_wool", 3),
                new Ingredient("minecraft:black_wool", 3)), "dojutsu:mask_tobi_1", 1);
        assertTrue(dobleLana.match(List.of(new Stack("minecraft:black_wool", 3))).isEmpty());
        assertTrue(dobleLana.match(List.of(
                new Stack("minecraft:black_wool", 3),
                new Stack("minecraft:black_wool", 3))).isPresent());
    }

    @Test
    void ignoraLosItemsQueSobranAlrededor() {
        // el datapack tampoco exige que no haya nada mas cerca: comprueba presencia, no exclusividad
        List<Stack> conBasura = List.of(
                new Stack("minecraft:dirt", 12),
                new Stack("minecraft:acacia_planks", 5),
                new Stack("minecraft:cobblestone", 1),
                new Stack("minecraft:black_wool", 3));
        assertTrue(TOBI_1.match(conBasura).isPresent());
    }

    @Test
    void laEspecificidadOrdenaLasRecetasDeMasIngredientesAMenos() {
        assertEquals(4, ANBU_1.specificity());
        assertEquals(2, TOBI_1.specificity());
        assertTrue(ANBU_1.specificity() > TOBI_1.specificity());
    }

    @Test
    void unaCantidadAbsurdaSeSanea() {
        assertEquals(1, new Ingredient("minecraft:diamond", 0).count());
        assertEquals(1, new DropRecipe(List.of(new Ingredient("minecraft:diamond", 1)), "dojutsu:dna", -3)
                .resultCount());
    }

    @Test
    void elResetDeAdnPideDiamanteMasVial() {
        // reset_dna: diamond 1 + el item de ADN; el resultado es un ADN sin identificar (mismo item, sin componente)
        DropRecipe reset = new DropRecipe(List.of(
                new Ingredient("minecraft:diamond", 1),
                new Ingredient("dojutsu:dna", 1)), "dojutsu:dna", 1);
        assertTrue(reset.match(List.of(
                new Stack("dojutsu:dna", 1),
                new Stack("minecraft:diamond", 1))).isPresent());
        assertFalse(reset.match(List.of(new Stack("dojutsu:dna", 1))).isPresent());
    }
}
