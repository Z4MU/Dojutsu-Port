package com.zamu.dojutsu.domain.cursedseal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests del <b>sello maldito</b> (F6/DJT-140). El modo es dominio puro, asi que el ciclo entero —incluida la
 * <b>muerte del portador</b>, que ningun otro modo tiene— se prueba con un contexto falso, sin arrancar Minecraft.
 * Valores verificados del datapack ({@link CursedSealTable}).
 */
class CursedSealModeTest {

    /** Contexto de prueba: chakra y timers en memoria. */
    private static final class FakeContext implements CursedSealModeContext {
        private Optional<CursedSealType> seal = Optional.of(CursedSealType.HEAVEN);
        private int level = 1;
        private int chakra = 1000;
        private int drainTimer;
        private int auraTicks;
        private int lastAuraLevel;
        private int consumedTimes;
        private Boolean lastToggle;

        @Override public Optional<CursedSealType> seal() { return seal; }
        @Override public int level() { return level; }

        @Override
        public boolean consumeChakra(int amount) {
            if (chakra < amount) {
                return false;
            }
            chakra -= amount;
            return true;
        }

        @Override public int drainTimer() { return drainTimer; }
        @Override public void setDrainTimer(int ticks) { drainTimer = ticks; }

        @Override
        public void applyAura(CursedSealType type, int auraLevel) {
            auraTicks++;
            lastAuraLevel = auraLevel;
        }

        @Override public void consumeBearer() { consumedTimes++; }
        @Override public void notifyToggled(boolean active) { lastToggle = active; }
    }

    @Test
    void noSeActivaSinSello() {
        FakeContext ctx = new FakeContext();
        ctx.seal = Optional.empty();
        CursedSealMode mode = new CursedSealMode();

        assertFalse(mode.activate(ctx));
        assertFalse(mode.isActive());
    }

    @Test
    void seActivaConSelloYAvisa() {
        FakeContext ctx = new FakeContext();
        CursedSealMode mode = new CursedSealMode();

        assertTrue(mode.activate(ctx));
        assertTrue(mode.isActive());
        assertEquals(Boolean.TRUE, ctx.lastToggle);
        assertEquals(CursedSealTable.DRAIN_INTERVAL_TICKS, ctx.drainTimer);
    }

    @Test
    void aplicaElAuraDelNivelActivoCadaTick() {
        FakeContext ctx = new FakeContext();
        ctx.level = 2;
        CursedSealMode mode = new CursedSealMode();
        mode.activate(ctx);

        mode.tick(ctx);
        mode.tick(ctx);

        assertEquals(2, ctx.auraTicks);
        assertEquals(2, ctx.lastAuraLevel);
    }

    @Test
    void cobraSoloAlVencerElIntervalo() {
        FakeContext ctx = new FakeContext();
        CursedSealMode mode = new CursedSealMode();
        mode.activate(ctx); // deja el timer en 40

        for (int i = 0; i < CursedSealTable.DRAIN_INTERVAL_TICKS; i++) {
            mode.tick(ctx);
        }
        assertEquals(1000, ctx.chakra); // los 40 primeros ticks solo descuentan el timer

        mode.tick(ctx); // el tick 41 encuentra el timer a 0 y cobra
        assertEquals(1000 - CursedSealTable.CHAKRA_COST_LEVEL_1, ctx.chakra);
        assertEquals(CursedSealTable.DRAIN_INTERVAL_TICKS, ctx.drainTimer);
    }

    @Test
    void madurarAbarataElMantenimiento() {
        FakeContext ctx = new FakeContext();
        ctx.level = CursedSealTable.MAX_LEVEL;
        CursedSealMode mode = new CursedSealMode();
        mode.activate(ctx);
        for (int i = 0; i <= CursedSealTable.DRAIN_INTERVAL_TICKS; i++) {
            mode.tick(ctx);
        }

        // nivel 2 cuesta 8, no 12: madurar el sello lo hace mas barato de sostener
        assertEquals(1000 - CursedSealTable.CHAKRA_COST_LEVEL_2, ctx.chakra);
        assertTrue(CursedSealTable.CHAKRA_COST_LEVEL_2 < CursedSealTable.CHAKRA_COST_LEVEL_1);
    }

    @Test
    void sinChakraElSelloConsumeAlPortadorYSeApaga() {
        FakeContext ctx = new FakeContext();
        ctx.chakra = CursedSealTable.CHAKRA_COST_LEVEL_1 - 1;
        CursedSealMode mode = new CursedSealMode();
        mode.activate(ctx);
        for (int i = 0; i <= CursedSealTable.DRAIN_INTERVAL_TICKS; i++) {
            mode.tick(ctx);
        }

        assertEquals(1, ctx.consumedTimes); // punish: el sello mata a su portador
        assertFalse(mode.isActive());
    }

    @Test
    void apagarloAvisaYNoConsumeAlPortador() {
        FakeContext ctx = new FakeContext();
        CursedSealMode mode = new CursedSealMode();
        mode.activate(ctx);

        mode.deactivate(ctx);

        assertFalse(mode.isActive());
        assertEquals(Boolean.FALSE, ctx.lastToggle);
        assertEquals(0, ctx.consumedTimes); // apagarlo a voluntad no cuesta la vida
    }
}
