package com.zamu.dojutsu.domain.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests del ADN (F8/DJT-170): las tres familias, la apuesta del implante y las dos veces que el azar no manda.
 * Verificado contra {@code items/dna/implant_dna} y {@code get_id}.
 */
class DnaTableTest {

    /** Generador con una tirada fijada. */
    private static final class ScriptedRandom extends Random {
        private final int value;

        private ScriptedRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return value % bound;
        }
    }

    @Test
    void sonVeintiseisRepartidosEnTresFamilias() {
        assertEquals(26, DnaType.values().length);
        long natures = 0;
        long kekkeiGenkai = 0;
        long hashirama = 0;
        for (DnaType type : DnaType.values()) {
            if (type.isHashiramaCells()) {
                hashirama++;
            } else if (type.nature().isPresent()) {
                natures++;
            } else if (type.kekkeiGenkai().isPresent()) {
                kekkeiGenkai++;
            }
        }
        assertEquals(7, natures);
        assertEquals(18, kekkeiGenkai);
        assertEquals(1, hashirama); // 7 + 18 + 1 = 26, exacto
    }

    @Test
    void lasCelulasDeHashiramaNoSonNiNaturalezaNiKekkeiGenkai() {
        assertTrue(DnaType.HASHIRAMA_CELLS.nature().isEmpty());
        assertTrue(DnaType.HASHIRAMA_CELLS.kekkeiGenkai().isEmpty());
        assertTrue(DnaType.HASHIRAMA_CELLS.isHashiramaCells());
    }

    @Test
    void losNumerosDelDatapackNoSeRepiten() {
        Set<Integer> ids = new HashSet<>();
        for (DnaType type : DnaType.values()) {
            assertTrue(ids.add(type.datapackId()), "id repetido: " + type.datapackId());
            assertTrue(type.datapackId() >= 1 && type.datapackId() <= 26);
        }
    }

    @Test
    void elImplanteSoloPrendeConElUno() {
        assertTrue(DnaTable.rollImplant(new ScriptedRandom(0)));  // tirada 1
        assertFalse(DnaTable.rollImplant(new ScriptedRandom(1))); // tirada 2
        assertFalse(DnaTable.rollImplant(new ScriptedRandom(2))); // tirada 3
    }

    @Test
    void elCuerpoQueYaAceptoElLinajeNoTiraDados() {
        // Madera con celulas de Hashirama dentro
        assertTrue(DnaTable.isGuaranteed(DnaType.WOOD, true, null));
        assertFalse(DnaTable.isGuaranteed(DnaType.WOOD, false, null));
        // celulas de Hashirama siendo Senju
        assertTrue(DnaTable.isGuaranteed(DnaType.HASHIRAMA_CELLS, false, DnaTable.SENJU_CLAN));
        assertFalse(DnaTable.isGuaranteed(DnaType.HASHIRAMA_CELLS, false, "uchiha"));
        // y no se contagia a otros KG
        assertFalse(DnaTable.isGuaranteed(DnaType.ICE, true, DnaTable.SENJU_CLAN));
    }

    @Test
    void unImplanteGarantizadoPrendeConLaPeorTirada() {
        assertEquals(DnaTable.ImplantOutcome.SUCCESS, DnaTable.attemptImplant(
                DnaType.WOOD, DnaTable.IMPLANT_MEDICAL, false, true, null, new ScriptedRandom(2)));
    }

    @Test
    void loQueYaTienesNiPrendeNiFalla() {
        // se comprueba ANTES del dado: no gasta el intento (ni el item)
        assertEquals(DnaTable.ImplantOutcome.ALREADY_OWNED, DnaTable.attemptImplant(
                DnaType.ICE, DnaTable.IMPLANT_MEDICAL, true, false, null, new ScriptedRandom(0)));
    }

    @Test
    void sinMedicinaSuficienteNoSeIntenta() {
        assertEquals(DnaTable.ImplantOutcome.LOW_MEDICAL, DnaTable.attemptImplant(
                DnaType.ICE, DnaTable.IMPLANT_MEDICAL - 1, false, false, null, new ScriptedRandom(0)));
        // identificar pide menos que implantar
        assertTrue(DnaTable.IDENTIFY_MEDICAL < DnaTable.IMPLANT_MEDICAL);
        assertTrue(DnaTable.canIdentify(DnaTable.IDENTIFY_MEDICAL));
        assertFalse(DnaTable.canImplant(DnaTable.IDENTIFY_MEDICAL));
    }

    @Test
    void laIdentificacionPuedeSalirCualquieraDeLosVeintiseis() {
        Set<DnaType> seen = new HashSet<>();
        for (int i = 0; i < DnaType.values().length; i++) {
            seen.add(DnaTable.rollIdentification(new ScriptedRandom(i)));
        }
        assertEquals(DnaType.values().length, seen.size());
    }

    @Test
    void losTiposSeResuelvenPorIdYPorNumero() {
        assertEquals(java.util.Optional.of(DnaType.WOOD), DnaType.byId("wood"));
        assertEquals(java.util.Optional.of(DnaType.HASHIRAMA_CELLS), DnaType.byDatapackId(18));
        assertTrue(DnaType.byId("sharingan").isEmpty()); // estado tolerante
        assertTrue(DnaType.byDatapackId(27).isEmpty());
    }
}
