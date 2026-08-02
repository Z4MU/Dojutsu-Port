package com.zamu.dojutsu.natures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.KekkeiGenkaiGrantedEvent;
import com.zamu.dojutsu.domain.event.NatureGrantedEvent;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests del servicio de Kekkei Genkai (DJT-052): implicacion de componentes, XP, bonus e idempotencia. */
class KekkeiGenkaiServiceTest {

    private static final UUID PLAYER = UUID.nameUUIDFromBytes("test".getBytes());

    private PlayerNinjaData data;
    private DomainEventBus bus;
    private List<String> granted;

    /** Definiciones de prueba (las reales del datapack se verifican en {@link KekkeiGenkaiDefinitionTest}). */
    @BeforeEach
    void setUp() {
        data = new PlayerNinjaData();
        bus = new DomainEventBus();
        granted = new ArrayList<>();
        bus.subscribe(NatureGrantedEvent.class, e -> granted.add(e.natureId()));

        EnumMap<KekkeiGenkai, KekkeiGenkaiDefinition> defs = new EnumMap<>(KekkeiGenkai.class);
        defs.put(KekkeiGenkai.WOOD, new KekkeiGenkaiDefinition(KekkeiGenkai.WOOD,
                List.of(NatureType.EARTH, NatureType.WATER), 10, Map.of()));
        defs.put(KekkeiGenkai.ICE, new KekkeiGenkaiDefinition(KekkeiGenkai.ICE,
                List.of(NatureType.WIND, NatureType.WATER), 8, Map.of(NatureType.WATER, 10)));
        defs.put(KekkeiGenkai.JUGO_CLAN, new KekkeiGenkaiDefinition(KekkeiGenkai.JUGO_CLAN,
                List.of(), 20, Map.of()));
        KekkeiGenkaiRegistry.load(defs);
    }

    @AfterEach
    void tearDown() {
        KekkeiGenkaiRegistry.load(Map.of()); // el registro es estatico: no contaminar otros tests
    }

    @Test
    void otorgarWoodImplicaEarthYWater() {
        // give/kg/wood: unless has_earth -> give/earth ; unless has_water -> give/water
        assertTrue(KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.WOOD, bus));

        assertTrue(KekkeiGenkaiService.has(data, KekkeiGenkai.WOOD));
        assertTrue(NatureService.has(data, NatureType.EARTH));
        assertTrue(NatureService.has(data, NatureType.WATER));
        assertEquals(List.of("earth", "water"), granted); // cada componente emite su evento
    }

    @Test
    void otorgarUnKgEmiteSuEvento() {
        // KekkeiGenkaiGrantedEvent (DJT-055): lo consume LearnerService para entregar el learner del KG.
        List<String> kgGranted = new ArrayList<>();
        bus.subscribe(KekkeiGenkaiGrantedEvent.class,
                e -> kgGranted.add(e.kekkeiGenkaiId()));

        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.WOOD, bus);
        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.WOOD, bus); // idempotente: sin segundo evento

        assertEquals(List.of("wood"), kgGranted);
    }

    @Test
    void laXpEsLaDelKgMasLaDeCadaComponente() {
        // wood 10 + earth 20 + water 20 = 50 (cada give/<nat> aporta la suya, como en el datapack).
        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.WOOD, bus);
        assertEquals(50, data.progression().ninjaXp());
    }

    @Test
    void unComponenteYaPoseidoNoSeRepite() {
        NatureService.grant(data, PLAYER, NatureType.EARTH, bus); // +20
        granted.clear();

        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.WOOD, bus); // wood 10 + water 20

        assertEquals(50, data.progression().ninjaXp()); // 20 + 30, no 20 + 50
        assertEquals(List.of("water"), granted); // earth no se re-otorga
    }

    @Test
    void elBonusDeAfinidadSeAplica() {
        // give/kg/ice: aff_water += 10 (ademas de implicar wind y water).
        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.ICE, bus);

        assertEquals(10, data.natures().affinity(NatureType.WATER).level());
        assertEquals(0, data.natures().affinity(NatureType.WIND).level()); // solo la del bonus
    }

    @Test
    void elBonusDeAfinidadSeSumaAlNivelExistente() {
        data.setNatures(data.natures().withAffinity(NatureType.WATER, new LeveledStat(50, 3)));

        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.ICE, bus);

        assertEquals(60, data.natures().affinity(NatureType.WATER).level());
        assertEquals(3, data.natures().affinity(NatureType.WATER).xp()); // la XP no se toca
    }

    @Test
    void otorgarDosVecesEsIdempotente() {
        assertTrue(KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.ICE, bus));
        assertFalse(KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.ICE, bus));

        assertEquals(10, data.natures().affinity(NatureType.WATER).level()); // el bonus no se duplica
        assertEquals(48, data.progression().ninjaXp()); // ice 8 + wind 20 + water 20, una sola vez
    }

    @Test
    void unKgSinComponentesSoloDaSuXp() {
        assertTrue(KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.JUGO_CLAN, bus));

        assertEquals(20, data.progression().ninjaXp());
        assertTrue(data.natures().natures().isEmpty());
        assertTrue(granted.isEmpty());
    }

    @Test
    void sinDefinicionCargadaNoSeOtorga() {
        // LAVA no esta en el registro de este test: no debe otorgarse a medias.
        assertFalse(KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.LAVA, bus));

        assertFalse(KekkeiGenkaiService.has(data, KekkeiGenkai.LAVA));
        assertEquals(0, data.progression().ninjaXp());
        assertTrue(data.natures().natures().isEmpty());
    }

    @Test
    void revocarQuitaElKgYSuXpPeroNoLosComponentes() {
        // take/kg/<kg> no revoca las naturalezas-componente ni el bonus de afinidad.
        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.ICE, bus); // 8 + 20 + 20 = 48

        assertTrue(KekkeiGenkaiService.revoke(data, PLAYER, KekkeiGenkai.ICE, bus));

        assertFalse(KekkeiGenkaiService.has(data, KekkeiGenkai.ICE));
        assertEquals(40, data.progression().ninjaXp()); // -8 (solo la del KG)
        assertTrue(NatureService.has(data, NatureType.WIND)); // los componentes se conservan
        assertTrue(NatureService.has(data, NatureType.WATER));
        assertEquals(10, data.natures().affinity(NatureType.WATER).level()); // el bonus tambien
    }

    @Test
    void revocarLoQueNoSeTieneNoHaceNada() {
        assertFalse(KekkeiGenkaiService.revoke(data, PLAYER, KekkeiGenkai.WOOD, bus));
        assertEquals(0, data.progression().ninjaXp());
    }

    @Test
    void elBonusDeAfinidadSeClampaAlCap() {
        data.setNatures(data.natures().withAffinity(NatureType.WATER, new LeveledStat(495, 0)));

        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.ICE, bus); // +10 -> 505, clampado a 500

        assertEquals(500, data.natures().affinity(NatureType.WATER).level());
    }
}
