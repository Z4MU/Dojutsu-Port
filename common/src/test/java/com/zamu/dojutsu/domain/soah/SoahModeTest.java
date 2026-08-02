package com.zamu.dojutsu.domain.soah;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests del Sello de la Fuerza de un Centenar (F6/DJT-150). El modo es dominio puro, asi que el ciclo entero
 * —vetos, entrada a curar por umbral, devolucion por tramos y apagado al vaciarse— se prueba con un contexto
 * falso. Valores verificados del datapack ({@link SoahTable}).
 */
class SoahModeTest {

    /** Contexto de prueba: reserva y chakra en memoria. */
    private static final class FakeContext implements SoahModeContext {
        private boolean sealed = true;
        private int stored = 5_000;
        private int chakra = 20_000;
        private int maxChakra = SoahTable.REQUIRED_MAX_CHAKRA;
        private int medical = SoahTable.UNLOCK_MEDICAL;
        private int yin = SoahTable.REQUIRED_YIN_AFFINITY;
        private boolean cursedSeal;
        private int cooldown;
        private int auraTicks;
        private Boolean lastToggle;
        private Integer lastStoredReport;
        private int outOfChakraNotices;
        private SoahMode.BlockReason lastBlock;

        @Override public boolean hasSeal() { return sealed; }
        @Override public int storedChakra() { return stored; }
        @Override public int chakra() { return chakra; }
        @Override public int maxChakra() { return maxChakra; }
        @Override public int medicalLevel() { return medical; }
        @Override public int yinAffinity() { return yin; }
        @Override public boolean cursedSealActive() { return cursedSeal; }
        @Override public int cooldown() { return cooldown; }
        @Override public void setCooldown(int ticks) { cooldown = ticks; }

        @Override
        public void transferFromSeal(int amount) {
            int moved = Math.min(amount, stored);
            stored -= moved;
            chakra = Math.min(chakra + moved, maxChakra);
        }

        @Override public void emitAura() { auraTicks++; }
        @Override public void notifyActivated(int storedChakra) { lastToggle = true; }
        @Override public void notifyDeactivated() { lastToggle = false; }
        @Override public void notifyStored(int storedChakra) { lastStoredReport = storedChakra; }
        @Override public void notifyOutOfStoredChakra() { outOfChakraNotices++; }
        @Override public void notifyBlocked(SoahMode.BlockReason reason) { lastBlock = reason; }
    }

    @Test
    void sinElSelloNoHayNadaQueActivar() {
        FakeContext ctx = new FakeContext();
        ctx.sealed = false;
        SoahMode mode = new SoahMode();

        assertFalse(mode.activate(ctx));
        assertEquals(SoahMode.BlockReason.NO_SEAL, ctx.lastBlock);
    }

    @Test
    void elSelloMalditoLoExcluye() {
        FakeContext ctx = new FakeContext();
        ctx.cursedSeal = true;
        SoahMode mode = new SoahMode();

        assertFalse(mode.activate(ctx));
        assertEquals(SoahMode.BlockReason.CURSED_SEAL, ctx.lastBlock);
    }

    @Test
    void losTresRequisitosSeCompruebanEnOrden() {
        SoahMode mode = new SoahMode();

        FakeContext lowMedical = new FakeContext();
        lowMedical.medical = SoahTable.UNLOCK_MEDICAL - 1;
        assertFalse(mode.activate(lowMedical));
        assertEquals(SoahMode.BlockReason.LOW_MEDICAL, lowMedical.lastBlock);

        FakeContext lowYin = new FakeContext();
        lowYin.yin = SoahTable.REQUIRED_YIN_AFFINITY - 1;
        assertFalse(new SoahMode().activate(lowYin));
        assertEquals(SoahMode.BlockReason.LOW_YIN, lowYin.lastBlock);

        FakeContext lowChakra = new FakeContext();
        lowChakra.maxChakra = SoahTable.REQUIRED_MAX_CHAKRA - 1;
        assertFalse(new SoahMode().activate(lowChakra));
        assertEquals(SoahMode.BlockReason.LOW_MAX_CHAKRA, lowChakra.lastBlock);
    }

    @Test
    void conLaReservaVaciaNoSeEnciende() {
        FakeContext ctx = new FakeContext();
        ctx.stored = 0;
        SoahMode mode = new SoahMode();

        assertFalse(mode.activate(ctx));
        assertEquals(SoahMode.BlockReason.EMPTY_SEAL, ctx.lastBlock);
    }

    @Test
    void encenderVetaPocoYApagarVetaMucho() {
        FakeContext ctx = new FakeContext();
        SoahMode mode = new SoahMode();

        assertTrue(mode.activate(ctx));
        assertEquals(SoahTable.ACTIVATE_COOLDOWN_TICKS, ctx.cooldown);
        assertEquals(Boolean.TRUE, ctx.lastToggle);

        mode.deactivate(ctx);
        assertEquals(SoahTable.DEACTIVATE_COOLDOWN_TICKS, ctx.cooldown);
        assertTrue(SoahTable.DEACTIVATE_COOLDOWN_TICKS > SoahTable.ACTIVATE_COOLDOWN_TICKS);
    }

    @Test
    void conElChakraAltoElSelloNoTocaLaReserva() {
        FakeContext ctx = new FakeContext();
        SoahMode mode = new SoahMode();
        mode.activate(ctx);
        int stored = ctx.stored;

        mode.tick(ctx);

        assertEquals(stored, ctx.stored); // por encima del umbral no devuelve nada
        assertFalse(mode.healing());
        assertEquals(1, ctx.auraTicks);
    }

    @Test
    void alBajarDelUmbralElSelloEmpiezaADevolver() {
        FakeContext ctx = new FakeContext();
        SoahMode mode = new SoahMode();
        mode.activate(ctx);
        ctx.chakra = SoahTable.HEAL_TRIGGER_CHAKRA - 1;

        mode.tick(ctx);

        assertTrue(mode.healing());
        assertEquals(SoahTable.HEAL_TRIGGER_CHAKRA - 1 + SoahTable.HEAL_TIER_LARGE, ctx.chakra);
        assertEquals(5_000 - SoahTable.HEAL_TIER_LARGE, ctx.stored);
    }

    @Test
    void noParaDeCurarHastaDejarteAlMaximo() {
        FakeContext ctx = new FakeContext();
        ctx.stored = SoahTable.RECHARGE_AMOUNT; // reserva de una recarga entera: da de sobra para llenar
        SoahMode mode = new SoahMode();
        mode.activate(ctx);
        ctx.chakra = SoahTable.HEAL_TRIGGER_CHAKRA - 1;

        // una vez arrancada, la cura sigue aunque el chakra ya supere el umbral de entrada
        for (int i = 0; i < 200; i++) {
            mode.tick(ctx);
        }

        assertEquals(ctx.maxChakra, ctx.chakra);
        assertFalse(mode.healing()); // stop_heals
        assertEquals(ctx.stored, ctx.lastStoredReport); // informa de lo que queda
        assertTrue(mode.isActive()); // curar hasta el tope no lo apaga
    }

    @Test
    void devuelveElTramoGrandePrimeroYLaGotaAlFinal() {
        assertEquals(SoahTable.HEAL_TIER_LARGE, SoahTable.healTier(20_000));
        assertEquals(SoahTable.HEAL_TIER_LARGE, SoahTable.healTier(SoahTable.HEAL_TIER_LARGE));
        assertEquals(SoahTable.HEAL_TIER_MEDIUM, SoahTable.healTier(SoahTable.HEAL_TIER_LARGE - 1));
        assertEquals(SoahTable.HEAL_TIER_SMALL, SoahTable.healTier(SoahTable.HEAL_TIER_MEDIUM - 1));
        assertEquals(0, SoahTable.healTier(0));
    }

    @Test
    void alVaciarseLaReservaSeApagaYExplicaComoRecargar() {
        FakeContext ctx = new FakeContext();
        ctx.stored = SoahTable.HEAL_TIER_SMALL;
        SoahMode mode = new SoahMode();
        mode.activate(ctx);
        ctx.chakra = SoahTable.HEAL_TRIGGER_CHAKRA - 1;

        mode.tick(ctx); // consume la ultima gota
        assertEquals(0, ctx.stored);

        mode.tick(ctx); // el siguiente tick encuentra la reserva vacia
        assertFalse(mode.isActive());
        assertEquals(1, ctx.outOfChakraNotices);
    }

    @Test
    void sinChakraElSelloNoSeSostiene() {
        FakeContext ctx = new FakeContext();
        SoahMode mode = new SoahMode();
        mode.activate(ctx);
        ctx.chakra = 0;
        ctx.stored = 0; // sin reserva no puede rellenarte

        mode.tick(ctx);

        assertFalse(mode.isActive());
    }
}
