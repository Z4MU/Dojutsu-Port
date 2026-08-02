package com.zamu.dojutsu.domain.eightgates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de las Ocho Puertas (F6/DJT-145). El modo es dominio puro, asi que la <b>escalera entera</b> —abrir de
 * la 1 a la 8, los vetos, la fatiga que se acelera al subir y el cierre forzado— se prueba con un contexto
 * falso, sin arrancar Minecraft. Valores verificados del datapack ({@link EightGatesTable}, {@link Gate}).
 */
class EightGatesModeTest {

    /** Contexto de prueba: fatiga y temporizadores en memoria. */
    private static final class FakeContext implements EightGatesModeContext {
        private int taijutsu = EightGatesTable.MASTERY_TAIJUTSU - 1; // ocho puertas, sin maestria
        private int strain;
        private int strainTimer;
        private int openCooldown;
        private int closeCooldown;
        private Gate lastApplied;
        private Gate lastOpened;
        private Integer closedAtStrain;
        private EightGatesMode.BlockReason lastBlock;

        @Override public int taijutsuLevel() { return taijutsu; }
        @Override public int strain() { return strain; }
        @Override public void addStrain(int amount) { strain = Math.min(strain + amount, 100); }
        @Override public int strainTimer() { return strainTimer; }
        @Override public void setStrainTimer(int ticks) { strainTimer = ticks; }
        @Override public int openCooldown() { return openCooldown; }
        @Override public void setOpenCooldown(int ticks) { openCooldown = ticks; }
        @Override public int closeCooldown() { return closeCooldown; }
        @Override public void setCloseCooldown(int ticks) { closeCooldown = ticks; }
        @Override public void applyGateEffects(Gate gate) { lastApplied = gate; }
        @Override public void notifyOpened(Gate gate) { lastOpened = gate; }
        @Override public void notifyClosed(int strainAtClose) { closedAtStrain = strainAtClose; }
        @Override public void notifyBlocked(EightGatesMode.BlockReason reason) { lastBlock = reason; }

        /** Abre una puerta saltandose el veto entre aperturas (el test no quiere esperar 20-180 ticks). */
        private boolean openIgnoringCooldown(EightGatesMode mode) {
            openCooldown = 0;
            return mode.openNext(this);
        }
    }

    @Test
    void sinTaijutsuSuficienteNoAbreNinguna() {
        FakeContext ctx = new FakeContext();
        ctx.taijutsu = EightGatesTable.ACTIVATION_TAIJUTSU - 1;
        EightGatesMode mode = new EightGatesMode();

        assertFalse(mode.openNext(ctx));
        assertFalse(mode.isActive());
        assertEquals(EightGatesMode.BlockReason.LOW_TAIJUTSU, ctx.lastBlock);
    }

    @Test
    void laPrimeraAperturaEnciendeElModoEnLaPuertaDeApertura() {
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();

        assertTrue(mode.openNext(ctx));
        assertTrue(mode.isActive());
        assertEquals(1, mode.stage());
        assertEquals(Gate.OPENING, ctx.lastOpened);
    }

    @Test
    void esUnaEscaleraDeOchoEscalones() {
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();

        for (int expected = 1; expected <= Gate.values().length; expected++) {
            assertTrue(ctx.openIgnoringCooldown(mode), "no abrio la puerta " + expected);
            assertEquals(expected, mode.stage());
        }
        // en la cima ya no hay mas: no se apaga, solo avisa
        assertFalse(ctx.openIgnoringCooldown(mode));
        assertTrue(mode.isActive());
        assertEquals(EightGatesMode.BlockReason.NO_MORE_GATES, ctx.lastBlock);
    }

    @Test
    void elTaijutsuLimitaHastaDondeLlegaLaEscalera() {
        FakeContext ctx = new FakeContext();
        ctx.taijutsu = Gate.LIFE.requiredTaijutsu(); // justo para la 3ª
        EightGatesMode mode = new EightGatesMode();

        assertTrue(ctx.openIgnoringCooldown(mode));
        assertTrue(ctx.openIgnoringCooldown(mode));
        assertTrue(ctx.openIgnoringCooldown(mode));
        assertFalse(ctx.openIgnoringCooldown(mode));
        assertEquals(3, mode.stage());
        assertEquals(EightGatesMode.BlockReason.NO_MORE_GATES, ctx.lastBlock);
    }

    @Test
    void elVetoEntreAperturasImpideEncadenar() {
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();
        mode.openNext(ctx); // deja el veto puesto

        assertTrue(ctx.openCooldown > 0);
        assertFalse(mode.openNext(ctx));
        assertEquals(1, mode.stage());
        assertEquals(EightGatesMode.BlockReason.OPEN_COOLDOWN, ctx.lastBlock);
    }

    @Test
    void trasCerrarHayQueEsperarParaVolverAAbrir() {
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();
        mode.openNext(ctx);

        mode.deactivate(ctx);

        assertFalse(mode.isActive());
        assertEquals(0, mode.stage());
        assertEquals(EightGatesTable.CLOSE_COOLDOWN_TICKS, ctx.closeCooldown);
        ctx.openCooldown = 0;
        assertFalse(mode.openNext(ctx));
        assertEquals(EightGatesMode.BlockReason.CLOSE_COOLDOWN, ctx.lastBlock);
    }

    @Test
    void laFatigaSubeMasRapidoCuantoMasArribaEstas() {
        // la 1ª suma 1 por pulso; la de la Muerte suma 4 (el cuadruple) sin maestria
        assertEquals(1, Gate.OPENING.strainGain(false));
        assertEquals(2, Gate.PAIN.strainGain(false));
        assertEquals(4, Gate.DEATH.strainGain(false));
        // dominar las puertas solo abarata las dos ultimas
        assertEquals(1, Gate.OPENING.strainGain(true));
        assertEquals(2, Gate.WONDER.strainGain(true));
        assertEquals(3, Gate.DEATH.strainGain(true));
    }

    @Test
    void elPrimerMordiscoDeFatigaLlegaAlVencerSuRetardo() {
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();
        mode.openNext(ctx);
        int delay = ctx.strainTimer;

        for (int i = 0; i < delay; i++) {
            mode.tick(ctx);
        }
        assertEquals(0, ctx.strain); // durante el retardo solo se aplican efectos

        mode.tick(ctx);
        assertEquals(1, ctx.strain);
        assertEquals(EightGatesTable.STRAIN_PULSE_TICKS, ctx.strainTimer); // luego, ritmo fijo
        assertEquals(Gate.OPENING, ctx.lastApplied);
    }

    @Test
    void alLlegarAlLimiteDeFatigaLasPuertasSeCierranSolas() {
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();
        mode.openNext(ctx);
        ctx.strain = EightGatesTable.FORCE_CLOSE_AT;

        mode.tick(ctx);

        assertFalse(mode.isActive());
        assertEquals(0, mode.stage());
        assertEquals(EightGatesTable.FORCE_CLOSE_AT, ctx.closedAtStrain); // el cierre forzado SI cobra
    }

    @Test
    void elCierreSeguroNoCobraNiDejaVeto() {
        // safe_deactivate (respawn/reset): la fatiga ya se limpia aparte, castigar dos veces seria injusto
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();
        mode.openNext(ctx);
        ctx.strain = 99;

        mode.closeSafely(ctx);

        assertFalse(mode.isActive());
        assertEquals(0, mode.stage());
        assertEquals(0, ctx.closeCooldown);
        assertEquals(null, ctx.closedAtStrain);
    }

    @Test
    void unCierreSeguroNoContaminaElSiguienteCierreNormal() {
        FakeContext ctx = new FakeContext();
        EightGatesMode mode = new EightGatesMode();
        mode.openNext(ctx);
        mode.closeSafely(ctx);
        ctx.closeCooldown = 0;
        ctx.openCooldown = 0;
        mode.openNext(ctx);

        mode.deactivate(ctx);

        assertEquals(EightGatesTable.CLOSE_COOLDOWN_TICKS, ctx.closeCooldown); // la marca se consumio
        assertEquals(0, ctx.closedAtStrain);
    }
}
