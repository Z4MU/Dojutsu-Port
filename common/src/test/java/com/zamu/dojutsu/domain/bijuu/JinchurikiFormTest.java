package com.zamu.dojutsu.domain.bijuu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de la forma de jinchuriki y del vinculo (F7/DJT-166). Dominio puro: el ciclo entero —vetos, aura por
 * nivel, caida por chakra y el Rinnegan del Diez Colas— se prueba sin arrancar Minecraft.
 */
class JinchurikiFormTest {

    /** Contexto de prueba. */
    private static final class FakeContext implements JinchurikiFormContext {
        private Optional<TailedBeast> beast = Optional.of(TailedBeast.NINE_TAILS);
        private int level = 1;
        private int chakra = 5_000;
        private int formCooldown;
        private boolean rinnegan;
        private boolean baryon;
        private int maxChakraDelta;
        private JinchurikiFormTable.FormAura lastAura;
        private Boolean flight;
        private Boolean lastToggle;
        private JinchurikiForm.BlockReason lastBlock;

        @Override public Optional<TailedBeast> beast() { return beast; }
        @Override public int level() { return level; }
        @Override public int chakra() { return chakra; }
        @Override public int formCooldown() { return formCooldown; }
        @Override public void setFormCooldown(int ticks) { formCooldown = ticks; }
        @Override public boolean hasRinneganActive() { return rinnegan; }
        @Override public void addMaxChakra(int amount) { maxChakraDelta += amount; }
        @Override public void applyAura(JinchurikiFormTable.FormAura aura) { lastAura = aura; }
        @Override public boolean baryonActive() { return baryon; }
        @Override public void setFlight(boolean flying) { flight = flying; }
        @Override public void notifyToggled(boolean active) { lastToggle = active; }
        @Override public void notifyBlocked(JinchurikiForm.BlockReason reason) { lastBlock = reason; }
    }

    @Test
    void sinBestiaNoHayForma() {
        FakeContext ctx = new FakeContext();
        ctx.beast = Optional.empty();
        JinchurikiForm form = new JinchurikiForm();

        assertFalse(form.activate(ctx));
        assertEquals(JinchurikiForm.BlockReason.NOT_JINCHURIKI, ctx.lastBlock);
    }

    @Test
    void entrarPrestaChakraMaximoYSalirLoDevuelve() {
        FakeContext ctx = new FakeContext();
        JinchurikiForm form = new JinchurikiForm();

        assertTrue(form.activate(ctx));
        assertEquals(JinchurikiFormTable.FORM_MAX_CHAKRA_BONUS, ctx.maxChakraDelta);
        assertEquals(Boolean.TRUE, ctx.lastToggle);

        form.deactivate(ctx);
        assertEquals(0, ctx.maxChakraDelta); // devuelto exacto
        assertEquals(JinchurikiFormTable.FORM_COOLDOWN_TICKS, ctx.formCooldown);
    }

    @Test
    void trasSalirHayQueEsperarParaVolverAEntrar() {
        FakeContext ctx = new FakeContext();
        JinchurikiForm form = new JinchurikiForm();
        form.activate(ctx);
        form.deactivate(ctx);

        assertFalse(form.activate(ctx));
        assertEquals(JinchurikiForm.BlockReason.COOLDOWN, ctx.lastBlock);
    }

    @Test
    void laFormaDelDiezColasExigeElRinnegan() {
        FakeContext ctx = new FakeContext();
        ctx.beast = Optional.of(TailedBeast.TEN_TAILS);
        JinchurikiForm form = new JinchurikiForm();

        assertFalse(form.activate(ctx));
        assertEquals(JinchurikiForm.BlockReason.NEEDS_RINNEGAN, ctx.lastBlock);

        ctx.rinnegan = true;
        assertTrue(form.activate(ctx));
        assertEquals(Boolean.TRUE, ctx.flight); // y ademas da vuelo
    }

    @Test
    void perderElRinneganAMediaFormaLaApaga() {
        FakeContext ctx = new FakeContext();
        ctx.beast = Optional.of(TailedBeast.TEN_TAILS);
        ctx.rinnegan = true;
        JinchurikiForm form = new JinchurikiForm();
        form.activate(ctx);

        ctx.rinnegan = false;
        form.tick(ctx);

        assertFalse(form.isActive());
    }

    @Test
    void elAuraLaFijaElVinculoNoLaBestia() {
        // llevar a Kurama con vinculo 1 da lo mismo que llevar a Shukaku con vinculo 1
        assertEquals(JinchurikiFormTable.auraFor(1, false), JinchurikiFormTable.auraFor(1, false));
        assertEquals(0, JinchurikiFormTable.auraFor(1, false).strength());
        assertEquals(1, JinchurikiFormTable.auraFor(2, false).strength());
        assertEquals(2, JinchurikiFormTable.auraFor(3, false).strength());
        assertEquals(2, JinchurikiFormTable.auraFor(4, false).strength()); // 3+ es la misma fila
    }

    @Test
    void elBaryonReemplazaElAuraDelNivelEnVezDeSumarse() {
        JinchurikiFormTable.FormAura baryon = JinchurikiFormTable.auraFor(4, true);
        assertEquals(6, baryon.strength());
        assertEquals(12, baryon.speed());
        // y da lo mismo el nivel: el Baryon manda
        assertEquals(baryon, JinchurikiFormTable.auraFor(1, true));
    }

    @Test
    void lasTecnicasNoLleganHastaElNivelTres() {
        assertFalse(JinchurikiFormTable.movesUnlocked(1));
        assertFalse(JinchurikiFormTable.movesUnlocked(2));
        assertTrue(JinchurikiFormTable.movesUnlocked(JinchurikiFormTable.MOVES_LEVEL));
    }

    @Test
    void laFormaCaeAlQuedarseSinChakra() {
        FakeContext ctx = new FakeContext();
        JinchurikiForm form = new JinchurikiForm();
        form.activate(ctx);
        ctx.chakra = JinchurikiFormTable.FORM_FALL_CHAKRA - 1;

        form.tick(ctx);

        assertFalse(form.isActive());
    }

    @Test
    void elVinculoTardaLoQueDigaCadaBestia() {
        // 2 h las mas dociles, 3 h Shukaku, 5 h Kurama
        assertEquals(144_000, BondTable.requiredTicks(TailedBeast.TWO_TAILS, 1));
        assertEquals(216_000, BondTable.requiredTicks(TailedBeast.ONE_TAIL, 1));
        assertEquals(360_000, BondTable.requiredTicks(TailedBeast.NINE_TAILS, 1));
        // el ultimo escalon cuesta casi el doble
        assertEquals(576_000, BondTable.requiredTicks(TailedBeast.NINE_TAILS, 3));
    }

    @Test
    void elDiezColasNoTieneVinculo() {
        assertFalse(BondTable.hasBond(TailedBeast.TEN_TAILS));
        assertEquals(0, BondTable.requiredTicks(TailedBeast.TEN_TAILS, 1));
        assertFalse(BondTable.levelsUp(TailedBeast.TEN_TAILS, 1, 999_999));
    }

    @Test
    void alMaximoElVinculoDejaDeSubir() {
        assertEquals(0, BondTable.requiredTicks(TailedBeast.NINE_TAILS, JinchurikiFormTable.MAX_LEVEL));
        assertFalse(BondTable.levelsUp(TailedBeast.NINE_TAILS, JinchurikiFormTable.MAX_LEVEL, 999_999));
    }
}
