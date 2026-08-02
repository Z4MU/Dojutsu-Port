package com.zamu.dojutsu.domain.susanoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests del Susanoo como modo activable (DJT-120). El modo es dominio puro, asi que el <b>ciclo entero</b>
 * (veto de activacion, aura por tick, cobro por pulsos, caida por chakra agotado) se prueba con un contexto
 * falso, sin arrancar Minecraft. Los valores son los verificados del datapack ({@link SusanooCostTable}).
 */
class SusanooModeTest {

    /** Contexto de prueba: chakra y fatiga en memoria, y registro de lo aplicado. */
    private static final class FakeContext implements SusanooContext {
        private int level = 1;
        private boolean eternal;
        private int chakra = 100;
        private int strain;
        private int pulseTimer;
        private SusanooTier lastAura;
        private int auraTicks;
        private Boolean lastToggle;

        @Override public int level() { return level; }
        @Override public boolean eternal() { return eternal; }
        @Override public int pulseTimer() { return pulseTimer; }
        @Override public void setPulseTimer(int ticks) { pulseTimer = ticks; }
        @Override public int chakra() { return chakra; }

        @Override
        public boolean consumeChakra(int amount) {
            if (chakra < amount) {
                return false;
            }
            chakra -= amount;
            return true;
        }

        @Override public void addStrain(int amount) { strain += amount; }

        @Override
        public void applyAura(SusanooTier tier) {
            lastAura = tier;
            auraTicks++;
        }

        @Override public void notifyToggled(boolean active) { lastToggle = active; }
    }

    @Test
    void noSeActivaSinElChakraMinimo() {
        FakeContext ctx = new FakeContext();
        ctx.chakra = SusanooCostTable.ACTIVATION_CHAKRA_GATE - 1;
        SusanooMode mode = new SusanooMode();

        assertFalse(mode.activate(ctx));
        assertFalse(mode.isActive());
        assertEquals(SusanooCostTable.ACTIVATION_CHAKRA_GATE - 1, ctx.chakra, "activar no cobra chakra");
    }

    @Test
    void activarNoCobraChakraEsSoloUnaPuerta() {
        FakeContext ctx = new FakeContext();
        ctx.chakra = SusanooCostTable.ACTIVATION_CHAKRA_GATE;
        SusanooMode mode = new SusanooMode();

        assertTrue(mode.activate(ctx));
        assertTrue(mode.isActive());
        assertEquals(SusanooCostTable.ACTIVATION_CHAKRA_GATE, ctx.chakra);
        assertEquals(Boolean.TRUE, ctx.lastToggle);
    }

    @Test
    void elPrimerTickCobraElPulsoDelNivelYRearmaElTemporizador() {
        FakeContext ctx = new FakeContext();
        SusanooMode mode = new SusanooMode();
        mode.activate(ctx);

        mode.tick(ctx);

        SusanooTier tier = SusanooCostTable.tier(1); // Ribcage: 6 chakra, +3 fatiga, cada 20t
        assertEquals(100 - tier.chakraPerPulse(), ctx.chakra);
        assertEquals(tier.strainPerPulse(), ctx.strain);
        assertEquals(tier.pulseTicks(), ctx.pulseTimer);
        assertEquals(tier, ctx.lastAura, "el aura del nivel se aplica cada tick");
    }

    @Test
    void entreDosPulsosNoSeCobraPeroSiSeAplicaElAura() {
        FakeContext ctx = new FakeContext();
        SusanooMode mode = new SusanooMode();
        mode.activate(ctx);
        mode.tick(ctx); // cobra el primer pulso
        int afterFirst = ctx.chakra;

        mode.tick(ctx); // dentro del intervalo: solo decrementa

        assertEquals(afterFirst, ctx.chakra);
        assertEquals(SusanooCostTable.tier(1).pulseTicks() - 1, ctx.pulseTimer);
        assertEquals(2, ctx.auraTicks, "el aura no depende del pulso");
    }

    @Test
    void cobraDeNuevoAlVencerElIntervalo() {
        FakeContext ctx = new FakeContext();
        SusanooMode mode = new SusanooMode();
        mode.activate(ctx);
        SusanooTier tier = SusanooCostTable.tier(1);

        // 1 tick cobra y deja el temporizador en pulseTicks; hacen falta esos ticks de descuento y uno mas
        // para volver a cobrar (descontar-y-luego-cobrar, como el susanoo_time del datapack).
        int ticksHastaElSegundoPulso = 1 + tier.pulseTicks() + 1;
        for (int i = 0; i < ticksHastaElSegundoPulso; i++) {
            mode.tick(ctx);
        }

        assertEquals(100 - tier.chakraPerPulse() * 2, ctx.chakra);
        assertEquals(tier.strainPerPulse() * 2, ctx.strain);
    }

    @Test
    void seApagaSoloCuandoElChakraNoAlcanzaElPulso() {
        FakeContext ctx = new FakeContext();
        ctx.chakra = SusanooCostTable.ACTIVATION_CHAKRA_GATE; // 5: pasa la puerta pero no paga los 6 del nivel 1
        SusanooMode mode = new SusanooMode();
        mode.activate(ctx);

        mode.tick(ctx);

        assertFalse(mode.isActive(), "el framework apaga el modo que no puede sostenerse");
        assertEquals(5, ctx.chakra, "el cobro es todo-o-nada: no deja el chakra a medias");
        assertEquals(Boolean.FALSE, ctx.lastToggle);
    }

    @Test
    void elOjoEternoNoPagaChakraYEspaciaElPulso() {
        FakeContext ctx = new FakeContext();
        ctx.eternal = true;
        SusanooMode mode = new SusanooMode();
        mode.activate(ctx);

        mode.tick(ctx);

        assertEquals(100, ctx.chakra, "el EMS no paga chakra por sostenerlo");
        assertEquals(SusanooCostTable.ETERNAL_STRAIN_PER_PULSE, ctx.strain);
        assertEquals(SusanooCostTable.ETERNAL_PULSE_TICKS, ctx.pulseTimer);
    }

    @Test
    void elNivelAltoEsMasBaratoDeSostener() {
        FakeContext ctx = new FakeContext();
        ctx.level = SusanooCostTable.MAX_LEVEL;
        SusanooMode mode = new SusanooMode();
        mode.activate(ctx);

        mode.tick(ctx);

        // Perfect: 1 chakra cada 80t, frente a los 6 cada 20t del Ribcage
        assertEquals(99, ctx.chakra);
        assertEquals(80, ctx.pulseTimer);
    }

    @Test
    void alternarLoApagaYLoVuelveAEncender() {
        FakeContext ctx = new FakeContext();
        SusanooMode mode = new SusanooMode();

        assertTrue(mode.toggle(ctx));
        assertFalse(mode.toggle(ctx));
        assertEquals(Boolean.FALSE, ctx.lastToggle);
        assertTrue(mode.toggle(ctx));
    }

    @Test
    void elModoApagadoNoHaceNadaAlTickear() {
        FakeContext ctx = new FakeContext();
        SusanooMode mode = new SusanooMode();

        mode.tick(ctx);

        assertEquals(100, ctx.chakra);
        assertEquals(0, ctx.auraTicks);
    }
}
