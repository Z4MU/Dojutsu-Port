package com.zamu.dojutsu.jutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.JutsuCastEvent;
import com.zamu.dojutsu.domain.jutsu.AffinityRequirement;
import com.zamu.dojutsu.domain.jutsu.CastContext;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.jutsu.JutsuCastPipeline.CastOutcome;
import com.zamu.dojutsu.natures.NatureService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Tests del pipeline de casteo (F4/DJT-074): nucleo puro y adapter de jugador. */
class JutsuCastPipelineTest {

    private static final UUID CASTER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    /** great_fireball con los valores del datapack: 140 chakra, cd 100t, fire >= 170. */
    private static JutsuEntry greatFireball() {
        return new JutsuEntry(new JutsuDefinition("great_fireball", 140, 100,
                Optional.of(new AffinityRequirement("fire", 170)), Optional.empty(), 0, "fire", "projectile"),
                new JsonObject());
    }

    /** Caster fake configurable. */
    private static final class FakeCaster implements CastContext {
        int chakra = 200;
        boolean onCooldown;
        boolean hasFire = true;
        int fireAffinity = 170;

        @Override
        public int availableChakra() {
            return chakra;
        }

        @Override
        public boolean isOnCooldown(String jutsuId) {
            return onCooldown;
        }

        @Override
        public boolean hasNature(String natureId) {
            return "fire".equals(natureId) && hasFire;
        }

        @Override
        public int affinity(String natureId) {
            return "fire".equals(natureId) ? fireAffinity : 0;
        }
    }

    // ------------------------------------------------------------------
    // Nucleo puro
    // ------------------------------------------------------------------

    @Test
    void unCastExitosoConsumeElCosteExactoYEmiteElEvento() {
        AtomicInteger consumed = new AtomicInteger(-1);
        AtomicBoolean effectRan = new AtomicBoolean(false);
        List<JutsuCastEvent> events = new ArrayList<>();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(JutsuCastEvent.class, events::add);

        CastOutcome outcome = JutsuCastPipeline.cast(CASTER, greatFireball(), new FakeCaster(),
                amount -> { consumed.set(amount); return true; },
                () -> effectRan.set(true), bus);

        assertEquals(CastOutcome.SUCCESS, outcome);
        assertEquals(140, consumed.get());
        assertTrue(effectRan.get());
        assertEquals(1, events.size());
        assertEquals(CASTER, events.get(0).caster());
        assertEquals("great_fireball", events.get(0).jutsuId());
    }

    @Test
    void unFalloDePrecondicionNoConsumeNiEjecutaNiEmite() {
        FakeCaster caster = new FakeCaster();
        caster.chakra = 139; // insuficiente
        AtomicBoolean consumed = new AtomicBoolean(false);
        AtomicBoolean effectRan = new AtomicBoolean(false);
        List<JutsuCastEvent> events = new ArrayList<>();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(JutsuCastEvent.class, events::add);

        CastOutcome outcome = JutsuCastPipeline.cast(CASTER, greatFireball(), caster,
                amount -> { consumed.set(true); return true; },
                () -> effectRan.set(true), bus);

        assertEquals(CastOutcome.INSUFFICIENT_CHAKRA, outcome);
        assertFalse(consumed.get());
        assertFalse(effectRan.get());
        assertTrue(events.isEmpty());
    }

    @Test
    void sinEfectoImplementadoFallaSinCoste() {
        AtomicBoolean consumed = new AtomicBoolean(false);
        CastOutcome outcome = JutsuCastPipeline.cast(CASTER, greatFireball(), new FakeCaster(),
                amount -> { consumed.set(true); return true; },
                null, new DomainEventBus());
        assertEquals(CastOutcome.EFFECT_UNAVAILABLE, outcome);
        assertFalse(consumed.get());
    }

    @Test
    void unEfectoRotoNoSuprimeElEvento() {
        // Aislamiento de fallos: el chakra ya se pago; el datapack daba la XP antes del summon.
        List<JutsuCastEvent> events = new ArrayList<>();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(JutsuCastEvent.class, events::add);

        CastOutcome outcome = JutsuCastPipeline.cast(CASTER, greatFireball(), new FakeCaster(),
                amount -> true,
                () -> { throw new IllegalStateException("efecto roto"); }, bus);

        assertEquals(CastOutcome.SUCCESS, outcome);
        assertEquals(1, events.size());
    }

    @Test
    void elOrdenDePrecondicionesEsElDelDatapack() {
        // chakra -> cooldown -> posesion -> nivel de afinidad
        FakeCaster caster = new FakeCaster();
        caster.chakra = 0;
        caster.onCooldown = true;
        caster.hasFire = false;
        caster.fireAffinity = 0;
        assertEquals(CastOutcome.INSUFFICIENT_CHAKRA, evaluate(caster));
        caster.chakra = 200;
        assertEquals(CastOutcome.ON_COOLDOWN, evaluate(caster));
        caster.onCooldown = false;
        assertEquals(CastOutcome.MISSING_NATURE, evaluate(caster));
        caster.hasFire = true;
        assertEquals(CastOutcome.INSUFFICIENT_AFFINITY, evaluate(caster));
        caster.fireAffinity = 170;
        assertEquals(CastOutcome.SUCCESS, evaluate(caster));
    }

    private static CastOutcome evaluate(FakeCaster caster) {
        return JutsuCastPipeline.cast(CASTER, greatFireball(), caster,
                amount -> true, () -> { }, new DomainEventBus());
    }

    @Test
    void sinObjetivoFallaSinCosteTrasLasPuertas() {
        // "Nothing nearby" (DJT-076): canApply se evalua tras las puertas y ANTES de cobrar.
        AtomicBoolean consumed = new AtomicBoolean(false);
        List<JutsuCastEvent> events = new ArrayList<>();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(JutsuCastEvent.class, events::add);
        CastOutcome outcome = JutsuCastPipeline.cast(CASTER, greatFireball(), new FakeCaster(),
                amount -> { consumed.set(true); return true; }, () -> { }, () -> false, () -> { }, bus);
        assertEquals(CastOutcome.NO_TARGET, outcome);
        assertFalse(consumed.get());
        assertTrue(events.isEmpty());
    }

    @Test
    void siElSinkRechazaElPagoNoHayCastNiEvento() {
        List<JutsuCastEvent> events = new ArrayList<>();
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(JutsuCastEvent.class, events::add);
        CastOutcome outcome = JutsuCastPipeline.cast(CASTER, greatFireball(), new FakeCaster(),
                amount -> false, () -> { }, bus);
        assertEquals(CastOutcome.INSUFFICIENT_CHAKRA, outcome);
        assertTrue(events.isEmpty());
    }

    @Test
    void elCooldownSoloSeAplicaEnUnCastExitoso() {
        AtomicBoolean cdApplied = new AtomicBoolean(false);
        // Exito: se aplica.
        JutsuCastPipeline.cast(CASTER, greatFireball(), new FakeCaster(),
                amount -> true, () -> { }, () -> true, () -> cdApplied.set(true), new DomainEventBus());
        assertTrue(cdApplied.get());

        // Fallo de precondicion: NO se aplica.
        FakeCaster broke = new FakeCaster();
        broke.chakra = 0;
        AtomicBoolean cdApplied2 = new AtomicBoolean(false);
        JutsuCastPipeline.cast(CASTER, greatFireball(), broke,
                amount -> true, () -> { }, () -> true, () -> cdApplied2.set(true), new DomainEventBus());
        assertFalse(cdApplied2.get());
    }

    // ------------------------------------------------------------------
    // PlayerCastContext sobre estado real (con ChakraService/NatureService)
    // ------------------------------------------------------------------

    @Test
    void elAdapterDeJugadorReflejaElEstadoReal() {
        PlayerNinjaData data = new PlayerNinjaData();
        ChakraService.setMax(data, 200, true);
        NatureService.grant(data, CASTER, NatureType.FIRE, new DomainEventBus());
        NatureService.setAffinityLevel(data, NatureType.FIRE, 170);

        PlayerCastContext ctx = new PlayerCastContext(data, 0L);
        assertEquals(200, ctx.availableChakra());
        assertTrue(ctx.hasNature("fire"));
        assertEquals(170, ctx.affinity("fire"));
        assertFalse(ctx.hasNature("water"));
        assertEquals(0, ctx.affinity("water"));
        assertFalse(ctx.hasNature("naturaleza_inventada")); // id desconocido: nunca crashea
        assertEquals(0, ctx.affinity("naturaleza_inventada"));
        assertFalse(ctx.isOnCooldown("great_fireball"));
    }

    @Test
    void elContextoDeJugadorLeeElCooldownAlTickDeMundo() {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setJutsu(data.jutsu().withCooldown("great_fireball", 1000L, 100)); // vence en 1100
        assertTrue(new PlayerCastContext(data, 1000L).isOnCooldown("great_fireball"));
        assertTrue(new PlayerCastContext(data, 1099L).isOnCooldown("great_fireball"));
        assertFalse(new PlayerCastContext(data, 1100L).isOnCooldown("great_fireball"));
    }

    @Test
    void laNaturalezaRevocadaConservaAfinidadPeroBloqueaElCast() {
        // El caso real que motiva la puerta MISSING_NATURE (regla 16, check_affinity).
        PlayerNinjaData data = new PlayerNinjaData();
        ChakraService.setMax(data, 200, true);
        DomainEventBus bus = new DomainEventBus();
        NatureService.grant(data, CASTER, NatureType.FIRE, bus);
        NatureService.setAffinityLevel(data, NatureType.FIRE, 170);
        NatureService.revoke(data, CASTER, NatureType.FIRE, bus);

        PlayerCastContext ctx = new PlayerCastContext(data, 0L);
        assertEquals(170, ctx.affinity("fire")); // el nivel se conserva (DJT-051)
        assertFalse(ctx.hasNature("fire"));      // pero la naturaleza no se posee

        CastOutcome outcome = JutsuCastPipeline.cast(CASTER, greatFireball(), ctx,
                amount -> ChakraService.consume(data, amount), () -> { }, bus);
        assertEquals(CastOutcome.MISSING_NATURE, outcome);
        assertEquals(200, data.chakra().chakra()); // sin coste
    }
}
