package com.zamu.dojutsu.domain.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.resource.Resource;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests del framework de modo activable (DJT-012) con un modo fake que recorre el ciclo. */
class ToggleableModeTest {

    /** Contexto fake: expone un recurso (chakra) que el coste por tick consume. */
    private static final class FakeContext implements ModeContext {
        final Resource chakra;

        FakeContext(Resource chakra) {
            this.chakra = chakra;
        }
    }

    /** Modo fake que registra cada hook invocado y cobra un coste por tick del recurso del contexto. */
    private static final class FakeMode extends ToggleableMode<FakeContext> {
        final List<String> log = new ArrayList<>();
        final int costPerTick;
        boolean canActivate = true;

        FakeMode(int costPerTick) {
            this.costPerTick = costPerTick;
        }

        @Override
        protected boolean onActivate(FakeContext ctx) {
            log.add("activate");
            return canActivate;
        }

        @Override
        protected void onTick(FakeContext ctx) {
            log.add("tick");
        }

        @Override
        protected boolean onCost(FakeContext ctx) {
            log.add("cost");
            return ctx.chakra.consume(costPerTick);
        }

        @Override
        protected void onPunish(FakeContext ctx) {
            log.add("punish");
        }

        @Override
        protected void onDeactivate(FakeContext ctx) {
            log.add("deactivate");
        }
    }

    /** Modo sin overrides: usa los hooks por defecto (gratis, sin efectos, sin veto). */
    private static final class DefaultMode extends ToggleableMode<FakeContext> {
    }

    @Test
    void nuevoModoEstaInactivo() {
        assertFalse(new FakeMode(1).isActive());
    }

    @Test
    void activarCambiaElEstadoYLlamaOnActivateUnaVez() {
        FakeMode m = new FakeMode(1);
        FakeContext ctx = new FakeContext(Resource.full(100));
        assertTrue(m.activate(ctx));
        assertTrue(m.isActive());
        assertEquals(List.of("activate"), m.log);
    }

    @Test
    void activarUnModoYaActivoNoHaceNada() {
        FakeMode m = new FakeMode(1);
        FakeContext ctx = new FakeContext(Resource.full(100));
        m.activate(ctx);
        assertFalse(m.activate(ctx), "ya activo: no re-activa");
        assertEquals(List.of("activate"), m.log, "onActivate no se repite");
    }

    @Test
    void onActivatePuedeVetarLaActivacion() {
        FakeMode m = new FakeMode(1);
        m.canActivate = false; // p. ej. sin recurso suficiente
        assertFalse(m.activate(new FakeContext(Resource.empty(100))));
        assertFalse(m.isActive());
        assertEquals(List.of("activate"), m.log);
    }

    @Test
    void tickEnModoInactivoNoHaceNada() {
        FakeMode m = new FakeMode(1);
        m.tick(new FakeContext(Resource.full(100)));
        assertTrue(m.log.isEmpty());
    }

    @Test
    void cicloCompletoCastigaYDesactivaCuandoNoPuedePagar() {
        FakeMode m = new FakeMode(4);
        FakeContext ctx = new FakeContext(new Resource(10, 100)); // alcanza para 2 ticks
        m.activate(ctx);
        m.tick(ctx); // 10 -> 6
        m.tick(ctx); // 6 -> 2
        m.tick(ctx); // no puede pagar 4 con 2: castigo + apagado
        assertFalse(m.isActive());
        assertEquals(2, ctx.chakra.current(), "el coste impago no resta");
        assertEquals(List.of(
                "activate",
                "tick", "cost",
                "tick", "cost",
                "tick", "cost", "punish", "deactivate"), m.log);
    }

    @Test
    void desactivarManualLimpiaYEsIdempotente() {
        FakeMode m = new FakeMode(1);
        FakeContext ctx = new FakeContext(Resource.full(100));
        m.activate(ctx);
        m.deactivate(ctx);
        assertFalse(m.isActive());
        m.deactivate(ctx); // ya inactivo: no-op
        assertEquals(List.of("activate", "deactivate"), m.log);
    }

    @Test
    void toggleAlternaElEstado() {
        FakeMode m = new FakeMode(1);
        FakeContext ctx = new FakeContext(Resource.full(100));
        assertTrue(m.toggle(ctx), "off -> on");
        assertFalse(m.toggle(ctx), "on -> off");
        assertEquals(List.of("activate", "deactivate"), m.log);
    }

    @Test
    void costeCeroConHooksPorDefectoNoCastigaNunca() {
        DefaultMode m = new DefaultMode();
        FakeContext ctx = new FakeContext(Resource.empty(100));
        assertTrue(m.activate(ctx));
        for (int i = 0; i < 100; i++) {
            m.tick(ctx);
        }
        assertTrue(m.isActive(), "modo gratuito sigue activo");
    }
}
