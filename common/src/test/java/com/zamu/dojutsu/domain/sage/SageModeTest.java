package com.zamu.dojutsu.domain.sage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests del modo sabio (F6/DJT-136). El modo es dominio puro, asi que el <b>ciclo entero</b> (vetos de
 * activacion, aura, drenaje por pulsos, caida por energia o chakra) se prueba con un contexto falso, sin
 * arrancar Minecraft. Valores verificados del datapack ({@link SageModeTable}).
 */
class SageModeTest {

    /** Contexto de prueba: energia y banderas en memoria. */
    private static final class FakeContext implements SageModeContext {
        private boolean sage = true;
        private int level = 1;
        private int energy = 100;
        private boolean chakra = true;
        private boolean seal;
        private int drainTimer;
        private int auraTicks;
        private Boolean lastToggle;
        private SageMode.BlockReason lastBlock;

        @Override public boolean isSage() { return sage; }
        @Override public int level() { return level; }
        @Override public int naturalEnergy() { return energy; }

        @Override
        public boolean consumeNaturalEnergy(int amount) {
            if (energy < amount) {
                return false;
            }
            energy -= amount;
            return true;
        }

        @Override public boolean hasChakra() { return chakra; }
        @Override public boolean cursedSealActive() { return seal; }
        @Override public int drainTimer() { return drainTimer; }
        @Override public void setDrainTimer(int ticks) { drainTimer = ticks; }
        @Override public void applyAura() { auraTicks++; }
        @Override public void notifyToggled(boolean active) { lastToggle = active; }
        @Override public void notifyBlocked(SageMode.BlockReason reason) { lastBlock = reason; }
    }

    @Test
    void noSeActivaSiAunNoEsSabio() {
        FakeContext ctx = new FakeContext();
        ctx.sage = false;
        SageMode mode = new SageMode();

        assertFalse(mode.activate(ctx));
        assertEquals(SageMode.BlockReason.NOT_SAGE, ctx.lastBlock);
    }

    @Test
    void elSelloMalditoExcluyeElModoSabio() {
        // toggle_check: si hay un sello activo, el modo sabio no puede encenderse
        FakeContext ctx = new FakeContext();
        ctx.seal = true;
        SageMode mode = new SageMode();

        assertFalse(mode.activate(ctx));
        assertEquals(SageMode.BlockReason.CURSED_SEAL, ctx.lastBlock);
    }

    @Test
    void elNivelUnoExigeLaCargaAlMaximo() {
        FakeContext ctx = new FakeContext();
        ctx.level = 1;
        ctx.energy = NaturalEnergyTable.MAX_CHARGE - 1;
        SageMode mode = new SageMode();

        assertFalse(mode.activate(ctx));
        assertEquals(SageMode.BlockReason.NOT_ENOUGH_ENERGY, ctx.lastBlock);

        ctx.energy = NaturalEnergyTable.MAX_CHARGE;
        assertTrue(mode.activate(ctx));
        assertEquals(Boolean.TRUE, ctx.lastToggle);
    }

    @Test
    void losNivelesAltosSeActivanConMuchaMenosEnergia() {
        // ACTIVATION_MINIMUM: 100, 50, 25, 1, 1
        assertEquals(100, SageModeTable.activationMinimum(1));
        assertEquals(50, SageModeTable.activationMinimum(2));
        assertEquals(25, SageModeTable.activationMinimum(3));
        assertEquals(1, SageModeTable.activationMinimum(4));
        assertEquals(1, SageModeTable.activationMinimum(5));
    }

    @Test
    void elNivelAlargaElModoNoLoAbarata() {
        // el coste es siempre 1, lo que cambia es cada cuanto: 100, 200, 300, 400, 500
        assertEquals(100, SageModeTable.drainInterval(1));
        assertEquals(200, SageModeTable.drainInterval(2));
        assertEquals(300, SageModeTable.drainInterval(3));
        assertEquals(400, SageModeTable.drainInterval(4));
        assertEquals(500, SageModeTable.drainInterval(5));
        assertEquals(1, SageModeTable.DRAIN_PER_PULSE);
    }

    @Test
    void gastaUnaEnergiaAlVencerElIntervalo() {
        FakeContext ctx = new FakeContext();
        SageMode mode = new SageMode();
        mode.activate(ctx);
        int start = ctx.energy;

        // activar deja el timer en drainInterval; hacen falta esos ticks + 1 para el primer cobro
        for (int i = 0; i < SageModeTable.drainInterval(1) + 1; i++) {
            mode.tick(ctx);
        }

        assertEquals(start - SageModeTable.DRAIN_PER_PULSE, ctx.energy);
        assertTrue(ctx.auraTicks > 0, "el aura se aplica cada tick");
    }

    @Test
    void caeSiSeQuedaSinChakra() {
        FakeContext ctx = new FakeContext();
        SageMode mode = new SageMode();
        mode.activate(ctx);

        ctx.chakra = false; // force_deactivate_1
        mode.tick(ctx);

        assertFalse(mode.isActive());
        assertEquals(Boolean.FALSE, ctx.lastToggle);
    }

    @Test
    void caeAlAgotarLaEnergiaNatural() {
        FakeContext ctx = new FakeContext();
        SageMode mode = new SageMode();
        mode.activate(ctx);
        ctx.energy = 0;      // force_deactivate
        ctx.drainTimer = 0;  // toca cobrar ya

        mode.tick(ctx);

        assertFalse(mode.isActive());
    }

    @Test
    void elModoApagadoNoHaceNadaAlTickear() {
        FakeContext ctx = new FakeContext();
        SageMode mode = new SageMode();

        mode.tick(ctx);

        assertEquals(100, ctx.energy);
        assertEquals(0, ctx.auraTicks);
    }

    // --- subida de nivel (requisitos compuestos) ---

    @Test
    void subirDeNivelExigeChakraMaximoSenjutsuYUso() {
        // 1->2: max_chakra>=7000, senjutsu>=300, sage_use>=72000
        assertEquals(1, SageModeTable.levelAfter(1, 6999, 300, 72000), "chakra insuficiente");
        assertEquals(1, SageModeTable.levelAfter(1, 7000, 299, 72000), "senjutsu insuficiente");
        assertEquals(1, SageModeTable.levelAfter(1, 7000, 300, 71999), "uso insuficiente");
        assertEquals(2, SageModeTable.levelAfter(1, 7000, 300, 72000), "los tres cumplidos");
    }

    @Test
    void elNivelCincoExigeElDobleDeUso() {
        // 4->5: max_chakra>=10000, senjutsu>=1200, sage_use>=144000
        assertEquals(4, SageModeTable.levelAfter(4, 10000, 1200, 143999));
        assertEquals(5, SageModeTable.levelAfter(4, 10000, 1200, 144000));
        assertEquals(SageModeTable.USE_FOR_MAX_LEVEL, 144000);
    }

    @Test
    void enElNivelMaximoYaNoSube() {
        assertEquals(NaturalEnergyTable.MAX_SAGE_LEVEL,
                SageModeTable.levelAfter(NaturalEnergyTable.MAX_SAGE_LEVEL, 99999, 99999, 999999));
    }

    @Test
    void elPrimerNivelSoloExigeUso() {
        // 0->1: sage_use >= 72000, sin requisitos de chakra ni senjutsu
        assertEquals(1, SageModeTable.levelAfter(0, 0, 0, SageModeTable.USE_FOR_EARLY_LEVELS));
        assertEquals(0, SageModeTable.levelAfter(0, 99999, 99999, SageModeTable.USE_FOR_EARLY_LEVELS - 1));
    }
}
