package com.zamu.dojutsu.domain.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.summoning.SummonTier;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests de las constantes del crafteo (F9/DJT-206; las tres puertas de pergamino, F10/DJT-225). Verificado
 * contra {@code crafting/{summoning_contract,medical_scroll,removal_scroll,sage_scroll}.mcfunction} y
 * {@code loot_tables/crafting/items_*.json}.
 */
class CraftingTableTest {

    @Test
    void laPuertaDelContratoEsEl300DelDatapackEnEscalaDelMod() {
        // crafting/summoning_contract: "unless score @s d.fuinjutsu matches 300.." -> 300 ÷ 4
        assertEquals(75, CraftingTable.CONTRACT_FUINJUTSU);
        assertFalse(CraftingTable.canCraftContract(74));
        assertTrue(CraftingTable.canCraftContract(75));
        assertTrue(CraftingTable.canCraftContract(400));
    }

    @Test
    void laPuertaCoincideConElFuinjutsuQueHaceUtilElContrato() {
        // por debajo de esa cifra el contrato tampoco sirve para invocar: es la misma frontera
        assertEquals(SummonTier.LOW.useFuinjutsu(), CraftingTable.CONTRACT_FUINJUTSU);
    }

    @Test
    void elReembolsoDevuelveExactamenteLosIngredientes() {
        // items_summoning_contract.json: 6 de papel + 3 de tinta = la receta shapeless entera
        assertEquals(6, CraftingTable.CONTRACT_REFUND_PAPER);
        assertEquals(3, CraftingTable.CONTRACT_REFUND_INK);
    }

    @Test
    void elPergaminoMedicoPideMedicina300DelDatapack() {
        // crafting/medical_scroll: "unless score @s d.medical matches 300.." -> 300 ÷ 4
        assertEquals(75, CraftingTable.MEDICAL_SCROLL_MEDICAL);
        assertFalse(CraftingTable.canCraftMedicalScroll(74));
        assertTrue(CraftingTable.canCraftMedicalScroll(75));
    }

    @Test
    void elPergaminoDeRetiradaEsLaPuertaMasAltaDeLasCuatro() {
        // crafting/removal_scroll: "unless score @s d.fuinjutsu matches 1000.." -> 1000 ÷ 4 = la mitad del tope
        assertEquals(250, CraftingTable.REMOVAL_SCROLL_FUINJUTSU);
        assertFalse(CraftingTable.canCraftRemovalScroll(249));
        assertTrue(CraftingTable.canCraftRemovalScroll(250));
        assertTrue(CraftingTable.REMOVAL_SCROLL_FUINJUTSU > CraftingTable.CONTRACT_FUINJUTSU);
    }

    @Test
    void elPergaminoDeSabioPideLasTresCosasALaVez() {
        // crafting/sage_scroll: is_sage AND d.senjutsu >= 300 (÷4) AND d.sagechakra >= 50 (sin reescalar)
        assertEquals(75, CraftingTable.SAGE_SCROLL_SENJUTSU);
        assertEquals(50, CraftingTable.SAGE_SCROLL_CHARGE);
        assertTrue(CraftingTable.canCraftSageScroll(true, 75, 50));
        assertFalse(CraftingTable.canCraftSageScroll(false, 500, 100), "sin ser sabio no vale ningun senjutsu");
        assertFalse(CraftingTable.canCraftSageScroll(true, 74, 100), "falta senjutsu");
        assertFalse(CraftingTable.canCraftSageScroll(true, 500, 49), "falta energia natural");
    }

    @Test
    void soloElPergaminoDeSabioCobraAlCraftear() {
        // el unico de los cuatro con "scoreboard players remove @s d.sagechakra 50" en la rama de exito, y cobra
        // exactamente lo mismo que exige
        assertEquals(CraftingTable.SAGE_SCROLL_CHARGE, CraftingTable.SAGE_SCROLL_CHARGE_COST);
    }

    @Test
    void cadaReembolsoDevuelveExactamenteSuReceta() {
        assertEquals(List.of(
                        new CraftingTable.Refund("minecraft:paper", 3),
                        new CraftingTable.Refund("minecraft:ink_sac", 3),
                        new CraftingTable.Refund("minecraft:golden_apple", 1),
                        new CraftingTable.Refund("minecraft:emerald", 2)),
                CraftingTable.MEDICAL_SCROLL_REFUND);
        assertEquals(List.of(
                        new CraftingTable.Refund("minecraft:paper", 2),
                        new CraftingTable.Refund("minecraft:ink_sac", 2),
                        new CraftingTable.Refund("minecraft:diamond", 1),
                        new CraftingTable.Refund("minecraft:purple_dye", 4)),
                CraftingTable.REMOVAL_SCROLL_REFUND);
        assertEquals(List.of(
                        new CraftingTable.Refund("minecraft:paper", 3),
                        new CraftingTable.Refund("minecraft:glow_ink_sac", 3),
                        new CraftingTable.Refund("minecraft:golden_apple", 1),
                        new CraftingTable.Refund("minecraft:emerald_block", 2)),
                CraftingTable.SAGE_SCROLL_REFUND);
    }
}
