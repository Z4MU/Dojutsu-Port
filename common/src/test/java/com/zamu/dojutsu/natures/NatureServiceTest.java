package com.zamu.dojutsu.natures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.NatureGrantedEvent;
import com.zamu.dojutsu.domain.event.NinjaXpChangedEvent;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests del servicio puro de naturalezas (DJT-051): grant/revoke/has, XP simetrica, idempotencia y eventos. */
class NatureServiceTest {

    private static final UUID PLAYER = UUID.nameUUIDFromBytes("test".getBytes());

    private PlayerNinjaData data;
    private DomainEventBus bus;
    private List<String> granted;
    private List<NinjaXpChangedEvent> xpChanges;

    @BeforeEach
    void setUp() {
        data = new PlayerNinjaData();
        bus = new DomainEventBus();
        granted = new ArrayList<>();
        xpChanges = new ArrayList<>();
        bus.subscribe(NatureGrantedEvent.class, e -> granted.add(e.natureId()));
        bus.subscribe(NinjaXpChangedEvent.class, xpChanges::add);
    }

    @Test
    void otorgarAnadeLaNaturalezaSumaXpYEmiteElEvento() {
        assertTrue(NatureService.grant(data, PLAYER, NatureType.FIRE, bus));

        assertTrue(NatureService.has(data, NatureType.FIRE));
        assertEquals(25, data.progression().ninjaXp()); // give/fire: ninja_xp += 25
        assertEquals(List.of("fire"), granted);
        assertEquals(1, xpChanges.size());
    }

    @Test
    void laXpPorNaturalezaNoEsUniforme() {
        // RA-4: valores exactos de natures/give/<nat>. [S03] resume "basicas = 25" y es FALSO.
        assertEquals(25, NatureType.FIRE.ninjaXp());
        assertEquals(25, NatureType.LIGHTNING.ninjaXp());
        assertEquals(20, NatureType.WIND.ninjaXp());
        assertEquals(20, NatureType.EARTH.ninjaXp());
        assertEquals(20, NatureType.WATER.ninjaXp());
        assertEquals(30, NatureType.YIN.ninjaXp());
        assertEquals(30, NatureType.YANG.ninjaXp());
        assertEquals(100, NatureType.YIN_YANG.ninjaXp());
    }

    @Test
    void otorgarNoFijaLaAfinidad() {
        // El 50 lo pone el onboarding (nature_select_main), NO el give. Prueba: join_uchiha hace
        // give/fire y luego aff_fire += 10; si el grant fijase 50, el Uchiha acabaria con 60.
        NatureService.grant(data, PLAYER, NatureType.FIRE, bus);
        assertEquals(0, data.natures().affinity(NatureType.FIRE).level());
    }

    @Test
    void otorgarDosVecesEsIdempotenteYNoRepiteXpNiEvento() {
        assertTrue(NatureService.grant(data, PLAYER, NatureType.WIND, bus));
        assertFalse(NatureService.grant(data, PLAYER, NatureType.WIND, bus));

        assertEquals(20, data.progression().ninjaXp()); // una sola vez
        assertEquals(List.of("wind"), granted);
    }

    @Test
    void reOtorgarNoReseteaLaAfinidad() {
        data.setNatures(data.natures().withAffinity(NatureType.WATER, new LeveledStat(50, 0)));
        NatureService.grant(data, PLAYER, NatureType.WATER, bus);
        NatureService.grant(data, PLAYER, NatureType.WATER, bus);

        assertEquals(50, data.natures().affinity(NatureType.WATER).level());
    }

    // --- XP de afinidad (DJT-078) ---

    @Test
    void laXpDeAfinidadSeAcumulaYAutoNivela() {
        // Banda 0-10: cada nivel cuesta 10 XP (XpCurve). +1 por cast -> 10 casts = 1 nivel.
        for (int i = 0; i < 10; i++) {
            NatureService.addAffinityXp(data, NatureType.FIRE, 1);
        }
        assertEquals(1, data.natures().affinity(NatureType.FIRE).level());
        assertEquals(0, data.natures().affinity(NatureType.FIRE).xp());
        // Un cast mas: nivel 1, 1 XP hacia el siguiente.
        NatureService.addAffinityXp(data, NatureType.FIRE, 1);
        assertEquals(1, data.natures().affinity(NatureType.FIRE).level());
        assertEquals(1, data.natures().affinity(NatureType.FIRE).xp());
    }

    @Test
    void laXpDeAfinidadIgnoraNoPositivaYYinYang() {
        NatureService.addAffinityXp(data, NatureType.FIRE, 0);
        NatureService.addAffinityXp(data, NatureType.FIRE, -5);
        assertEquals(0, data.natures().affinity(NatureType.FIRE).xp());
        // YIN_YANG es derivada (no almacena afinidad): no lanza, no hace nada.
        NatureService.addAffinityXp(data, NatureType.YIN_YANG, 100);
        assertEquals(0, data.natures().affinity(NatureType.YIN_YANG).level());
    }

    @Test
    void laXpDeAfinidadEnElCapNoProgresa() {
        data.setNatures(data.natures().withAffinity(NatureType.EARTH,
                new LeveledStat(com.zamu.dojutsu.domain.progression.XpCurve.MAX_LEVEL, 0)));
        NatureService.addAffinityXp(data, NatureType.EARTH, 50);
        assertEquals(com.zamu.dojutsu.domain.progression.XpCurve.MAX_LEVEL,
                data.natures().affinity(NatureType.EARTH).level());
        assertEquals(0, data.natures().affinity(NatureType.EARTH).xp());
    }

    @Test
    void revocarQuitaLaNaturalezaYRestaLaMismaXp() {
        // take/<nat> es exactamente simetrico a give/<nat> (verificado uno a uno en el datapack).
        NatureService.grant(data, PLAYER, NatureType.YIN, bus);
        assertEquals(30, data.progression().ninjaXp());

        assertTrue(NatureService.revoke(data, PLAYER, NatureType.YIN, bus));

        assertFalse(NatureService.has(data, NatureType.YIN));
        assertEquals(0, data.progression().ninjaXp());
    }

    @Test
    void revocarLoQueNoSeTieneNoHaceNada() {
        assertFalse(NatureService.revoke(data, PLAYER, NatureType.EARTH, bus));
        assertEquals(0, data.progression().ninjaXp());
        assertTrue(xpChanges.isEmpty());
    }

    @Test
    void revocarNoBorraLaAfinidad() {
        // take/* del datapack tampoco resetea d.aff_<nat>: no se pierde el progreso al revocar.
        data.setNatures(data.natures().withAffinity(NatureType.EARTH, new LeveledStat(40, 5)));
        NatureService.grant(data, PLAYER, NatureType.EARTH, bus);
        NatureService.revoke(data, PLAYER, NatureType.EARTH, bus);

        assertEquals(new LeveledStat(40, 5), data.natures().affinity(NatureType.EARTH));
    }

    @Test
    void laXpNinjaNuncaBajaDeCero() {
        // El datapack restaria a negativo; aqui hay suelo en 0 (la ninja_xp alimenta el rango).
        NatureService.grant(data, PLAYER, NatureType.YIN_YANG, bus); // +100
        NatureService.revoke(data, PLAYER, NatureType.YIN_YANG, bus); // -100 -> 0
        NatureService.grant(data, PLAYER, NatureType.WIND, bus); // +20
        NatureService.revoke(data, PLAYER, NatureType.WIND, bus); // -20 -> 0

        assertEquals(0, data.progression().ninjaXp());
    }

    @Test
    void otorgarYangFuncionaSinRamaRota() {
        // [S03-R1]: en el datapack, get_random_kg llamaba a natures/give/kg/yang (inexistente) y la tirada
        // 1..2 no hacia nada. Aqui Yang es un NatureType normal y su grant funciona como cualquier otro.
        assertTrue(NatureService.grant(data, PLAYER, NatureType.YANG, bus));

        assertTrue(NatureService.has(data, NatureType.YANG));
        assertEquals(30, data.progression().ninjaXp());
        assertEquals(List.of("yang"), granted);
    }

    @Test
    void otorgarYinYYangDaAfinidadYinYangDerivada() {
        data.setNatures(data.natures()
                .withAffinity(NatureType.YIN, new LeveledStat(50, 0))
                .withAffinity(NatureType.YANG, new LeveledStat(30, 0)));
        NatureService.grant(data, PLAYER, NatureType.YIN, bus);
        NatureService.grant(data, PLAYER, NatureType.YANG, bus);

        assertEquals(40, data.natures().affinity(NatureType.YIN_YANG).level()); // (50+30)/2
    }

    @Test
    void elIdDelEventoEsEstableYEnMinusculas() {
        assertEquals("fire", NatureService.id(NatureType.FIRE));
        assertEquals("yin_yang", NatureService.id(NatureType.YIN_YANG));
    }

    @Test
    void otorgarVariasNaturalezasAcumulaXp() {
        NatureService.grant(data, PLAYER, NatureType.FIRE, bus); // +25
        NatureService.grant(data, PLAYER, NatureType.WIND, bus); // +20
        NatureService.grant(data, PLAYER, NatureType.YIN, bus); // +30

        assertEquals(75, data.progression().ninjaXp());
        assertEquals(List.of("fire", "wind", "yin"), granted);
        assertTrue(data.natures().natures().containsAll(
                List.of(NatureType.FIRE, NatureType.WIND, NatureType.YIN)));
    }
}
