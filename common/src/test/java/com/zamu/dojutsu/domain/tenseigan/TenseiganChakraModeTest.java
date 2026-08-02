package com.zamu.dojutsu.domain.tenseigan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests del Tenseigan Chakra Mode como modo activable (cierre de flecos de F5). El modo es dominio puro, asi que
 * el ciclo entero (veto, aura+vuelo, pulso, caida por umbral) se prueba con un contexto falso, sin arrancar MC.
 * Valores verificados del datapack ({@code tenseigan/chakra_mode/main}: 10 chakra/200t, cae si {@code < 19}).
 */
class TenseiganChakraModeTest {

    private static final class FakeContext implements TenseiganChakraModeContext {
        private int chakra = 100;
        private int pulseTimer;
        private int auraTicks;
        private Boolean flight;
        private Boolean lastToggle;

        @Override public int chakra() { return chakra; }

        @Override
        public boolean consumeChakra(int amount) {
            if (chakra < amount) {
                return false;
            }
            chakra -= amount;
            return true;
        }

        @Override public int pulseTimer() { return pulseTimer; }
        @Override public void setPulseTimer(int ticks) { pulseTimer = ticks; }
        @Override public void applyAura() { auraTicks++; }
        @Override public void setFlight(boolean allowed) { flight = allowed; }
        @Override public void notifyToggled(boolean active) { lastToggle = active; }
    }

    @Test
    void noSeActivaSinChakraParaSostenerlo() {
        FakeContext ctx = new FakeContext();
        ctx.chakra = TenseiganChakraMode.SUSTAIN_THRESHOLD - 1;
        TenseiganChakraMode mode = new TenseiganChakraMode();

        assertFalse(mode.activate(ctx));
        assertFalse(mode.isActive());
        assertEquals(TenseiganChakraMode.SUSTAIN_THRESHOLD - 1, ctx.chakra, "activar no cobra chakra");
    }

    @Test
    void activarConcedeVueloYNoCobra() {
        FakeContext ctx = new FakeContext();
        TenseiganChakraMode mode = new TenseiganChakraMode();

        assertTrue(mode.activate(ctx));
        assertEquals(Boolean.TRUE, ctx.flight);
        assertEquals(Boolean.TRUE, ctx.lastToggle);
        assertEquals(100, ctx.chakra);
    }

    @Test
    void elPrimerTickCobraElPulsoYAplicaElAura() {
        FakeContext ctx = new FakeContext();
        TenseiganChakraMode mode = new TenseiganChakraMode();
        mode.activate(ctx);

        mode.tick(ctx);

        assertEquals(100 - TenseiganChakraMode.CHAKRA_PER_PULSE, ctx.chakra);
        assertEquals(TenseiganChakraMode.PULSE_TICKS, ctx.pulseTimer);
        assertEquals(1, ctx.auraTicks);
    }

    @Test
    void entreDosPulsosNoSeCobraPeroSiSeAplicaElAura() {
        FakeContext ctx = new FakeContext();
        TenseiganChakraMode mode = new TenseiganChakraMode();
        mode.activate(ctx);
        mode.tick(ctx); // primer pulso
        int afterFirst = ctx.chakra;

        mode.tick(ctx); // dentro del intervalo

        assertEquals(afterFirst, ctx.chakra);
        assertEquals(TenseiganChakraMode.PULSE_TICKS - 1, ctx.pulseTimer);
        assertEquals(2, ctx.auraTicks);
    }

    @Test
    void caeYRetiraElVueloCuandoElChakraBajaDelUmbral() {
        FakeContext ctx = new FakeContext();
        ctx.chakra = TenseiganChakraMode.SUSTAIN_THRESHOLD; // 19: activa, pero el pulso lo dejara por debajo
        TenseiganChakraMode mode = new TenseiganChakraMode();
        mode.activate(ctx);

        mode.tick(ctx); // cobra 10 -> chakra 9
        mode.tick(ctx); // 9 < 19 -> cae

        assertFalse(mode.isActive(), "cae al bajar del umbral de sostenimiento");
        assertEquals(Boolean.FALSE, ctx.flight, "onDeactivate retira el vuelo");
        assertEquals(Boolean.FALSE, ctx.lastToggle);
    }

    @Test
    void alternarloRetiraElVuelo() {
        FakeContext ctx = new FakeContext();
        TenseiganChakraMode mode = new TenseiganChakraMode();

        assertTrue(mode.toggle(ctx));
        assertEquals(Boolean.TRUE, ctx.flight);
        assertFalse(mode.toggle(ctx));
        assertEquals(Boolean.FALSE, ctx.flight);
    }

    @Test
    void elModoApagadoNoHaceNadaAlTickear() {
        FakeContext ctx = new FakeContext();
        TenseiganChakraMode mode = new TenseiganChakraMode();

        mode.tick(ctx);

        assertEquals(100, ctx.chakra);
        assertEquals(0, ctx.auraTicks);
    }
}
