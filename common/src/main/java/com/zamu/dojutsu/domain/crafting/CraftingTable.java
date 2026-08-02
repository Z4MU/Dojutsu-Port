package com.zamu.dojutsu.domain.crafting;

import java.util.List;

/**
 * Constantes del <b>crafteo</b> {@code [S23]} (F9/DJT-206). Verificadas contra {@code recipes/*.json},
 * {@code crafting/*.mcfunction} y {@code main/internal/crafting.mcfunction}.
 *
 * <h2>Son dos sistemas, y el primero casi desaparece al portarlo</h2>
 * El datapack tiene <b>13 recetas de mesa</b> que producen todas un {@code knowledge_book}; un advancement
 * ({@code recipe_unlocked}) dispara una funcion que entrega el item de verdad con su NBT y borra el libro. El
 * rodeo existe porque <b>un datapack no puede producir NBT desde una receta</b>. Un mod si: aqui la receta
 * apunta al item real y <b>se van las 13 funciones {@code crafting/*} y sus 13 advancements</b>.
 *
 * <p>El segundo sistema es el <b>drop-crafting</b> ({@link DropRecipe}): 16 combinaciones que el datapack
 * resuelve emparejando <b>entidades item</b> en el suelo, porque no caben en una mesa 3x3.
 *
 * <h2>Lo que cabe en una mesa y lo que no</h2>
 * Una receta de mesa tiene <b>9 huecos</b> y cada hueco aporta <b>una</b> unidad. Las mascaras piden
 * {@code 5 tablones + 3 lana + 3 tinte} = 11 unidades: <b>no caben</b>, y por eso siguen siendo drop-crafting
 * aunque el mod pudiera darles NBT. La unica excepcion seria {@code tobi_1} (5+3 = 8), que se deja con sus
 * hermanas por coherencia.
 *
 * <h2>La puerta de habilidad que la ficha no contaba</h2>
 * Cuatro de las 13 recetas <b>no entregan el item sin mas</b>: comprueban una habilidad y, si falta,
 * <b>devuelven los ingredientes</b> con aviso rojo y yunque ({@code crafting/failed} +
 * {@code loot_tables/crafting/items_*}). Son {@code summoning_contract} ({@code d.fuinjutsu >= 300}),
 * {@code medical_scroll} ({@code d.medical >= 300}), {@code removal_scroll} ({@code d.fuinjutsu >= 1000}) y
 * {@code sage_scroll} (sabio + {@code d.senjutsu >= 300} + {@code d.sagechakra >= 50}, que ademas <b>gasta</b>
 * 50 de energia natural). Desde <b>F10/DJT-225 existen las cuatro</b>.
 *
 * <p>Los requisitos van en <b>escala del mod</b> (datapack ÷ 4, la convencion de DJT-082 calibrada en
 * {@code SoahTable}): el 300 del datapack es {@value #CONTRACT_FUINJUTSU} aqui. La <b>energia natural no se
 * reescala</b>: es un 0..100 en los dos sitios ({@code chakra/main} fija {@code d.max_sagechakra} a 100, igual
 * que {@code NaturalEnergyTable.MAX_CHARGE}), asi que el 50 del datapack es 50 aqui.
 *
 * <p><b>El pergamino de sabio cobra por adelantado</b>: es la unica de las cuatro puertas que ademas de exigir
 * <b>gasta</b> ({@value #SAGE_SCROLL_CHARGE_COST} de energia natural). Craftear un pergamino que restaura 100 te
 * cuesta 50 — sigue saliendo a cuenta, pero no es gratis, y si la tirada de habilidad falla no se cobra.
 */
public final class CraftingTable {

    /**
     * Fuinjutsu para craftear un contrato de invocacion en blanco, en escala del mod
     * ({@code crafting/summoning_contract}: datapack 300 ÷ 4). Coincide con
     * {@code SummonTier.LOW.useFuinjutsu()}: por debajo de esta cifra el contrato no sirve para invocar.
     */
    public static final int CONTRACT_FUINJUTSU = 75;

    /** Papel que devuelve {@code loot_tables/crafting/items_summoning_contract} por crafteo rechazado. */
    public static final int CONTRACT_REFUND_PAPER = 6;

    /** Tinta que devuelve {@code loot_tables/crafting/items_summoning_contract} por crafteo rechazado. */
    public static final int CONTRACT_REFUND_INK = 3;

    /**
     * Medicina para craftear un Pergamino Medico, en escala del mod ({@code crafting/medical_scroll}: datapack
     * 300 ÷ 4).
     */
    public static final int MEDICAL_SCROLL_MEDICAL = 75;

    /**
     * Fuinjutsu para craftear un Pergamino de Retirada, en escala del mod ({@code crafting/removal_scroll}:
     * datapack 1000 ÷ 4). Es la puerta <b>mas alta</b> de las cuatro: la mitad del tope de habilidad.
     */
    public static final int REMOVAL_SCROLL_FUINJUTSU = 250;

    /**
     * Senjutsu para craftear un Pergamino de Sabio, en escala del mod ({@code crafting/sage_scroll}: datapack
     * 300 ÷ 4). Ademas hay que <b>ser</b> sabio y tener {@value #SAGE_SCROLL_CHARGE} de energia natural.
     */
    public static final int SAGE_SCROLL_SENJUTSU = 75;

    /** Energia natural minima para craftear un Pergamino de Sabio (0..100, sin reescalar). */
    public static final int SAGE_SCROLL_CHARGE = 50;

    /** Energia natural que <b>gasta</b> el crafteo del Pergamino de Sabio ({@code remove @s d.sagechakra 50}). */
    public static final int SAGE_SCROLL_CHARGE_COST = 50;

    /** Devolucion de {@code items_medical_scroll}: papel, tinta, manzana dorada, esmeralda. */
    public static final List<Refund> MEDICAL_SCROLL_REFUND = List.of(
            new Refund("minecraft:paper", 3), new Refund("minecraft:ink_sac", 3),
            new Refund("minecraft:golden_apple", 1), new Refund("minecraft:emerald", 2));

    /** Devolucion de {@code items_removal_scroll}: papel, tinta, diamante, tinte morado. */
    public static final List<Refund> REMOVAL_SCROLL_REFUND = List.of(
            new Refund("minecraft:paper", 2), new Refund("minecraft:ink_sac", 2),
            new Refund("minecraft:diamond", 1), new Refund("minecraft:purple_dye", 4));

    /** Devolucion de {@code items_sage_scroll}: papel, tinta brillante, manzana dorada, bloque de esmeralda. */
    public static final List<Refund> SAGE_SCROLL_REFUND = List.of(
            new Refund("minecraft:paper", 3), new Refund("minecraft:glow_ink_sac", 3),
            new Refund("minecraft:golden_apple", 1), new Refund("minecraft:emerald_block", 2));

    /**
     * Una pila devuelta por un crafteo rechazado. Las cuatro loot tables {@code crafting/items_*} son cuatro
     * pools de una entrada con {@code set_count} fijo: el "azar" del loot table no lo es.
     *
     * @param itemId id de registro del item ({@code "minecraft:paper"})
     * @param count  unidades a devolver
     */
    public record Refund(String itemId, int count) {
    }

    private CraftingTable() {
    }

    /** Indica si ese fuinjutsu (escala del mod) basta para craftear un contrato en blanco. */
    public static boolean canCraftContract(int fuinjutsu) {
        return fuinjutsu >= CONTRACT_FUINJUTSU;
    }

    /** Indica si esa medicina (escala del mod) basta para craftear un Pergamino Medico. */
    public static boolean canCraftMedicalScroll(int medical) {
        return medical >= MEDICAL_SCROLL_MEDICAL;
    }

    /** Indica si ese fuinjutsu (escala del mod) basta para craftear un Pergamino de Retirada. */
    public static boolean canCraftRemovalScroll(int fuinjutsu) {
        return fuinjutsu >= REMOVAL_SCROLL_FUINJUTSU;
    }

    /**
     * Indica si se puede craftear un Pergamino de Sabio. Las tres condiciones son <b>and</b>, y el datapack las
     * comprueba con tres {@code tag @s add d.cannot_craft} encadenados: basta que falle una.
     *
     * @param sage     si ya es sabio ({@code sage_mode/is_sage})
     * @param senjutsu senjutsu en escala del mod
     * @param charge   energia natural actual (0..100)
     */
    public static boolean canCraftSageScroll(boolean sage, int senjutsu, int charge) {
        return sage && senjutsu >= SAGE_SCROLL_SENJUTSU && charge >= SAGE_SCROLL_CHARGE;
    }
}
