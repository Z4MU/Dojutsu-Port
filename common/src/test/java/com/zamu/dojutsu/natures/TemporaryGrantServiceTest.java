package com.zamu.dojutsu.natures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de los prestamos de naturalezas/KG (DJT-053): procedencia (no caducidad), y sobre todo que retirar un
 * prestamo <b>nunca</b> se lleva lo permanente ni lo prestado por otra fuente.
 */
class TemporaryGrantServiceTest {

    private static final UUID PLAYER = UUID.nameUUIDFromBytes("test".getBytes());
    private static final String ONE_TAIL = "one_tail";
    private static final String NINE_TAILS = "nine_tails";

    private PlayerNinjaData data;
    private DomainEventBus bus;

    @BeforeEach
    void setUp() {
        data = new PlayerNinjaData();
        bus = new DomainEventBus();
        EnumMap<KekkeiGenkai, KekkeiGenkaiDefinition> defs = new EnumMap<>(KekkeiGenkai.class);
        defs.put(KekkeiGenkai.MAGNET, new KekkeiGenkaiDefinition(KekkeiGenkai.MAGNET,
                List.of(NatureType.WIND, NatureType.EARTH), 9, Map.of()));
        KekkeiGenkaiRegistry.load(defs);
    }

    @AfterEach
    void tearDown() {
        KekkeiGenkaiRegistry.load(Map.of());
    }

    @Test
    void prestarUnaNaturalezaLaOtorgaYLaMarca() {
        assertTrue(TemporaryGrantService.grantNature(data, PLAYER, NatureType.WIND, ONE_TAIL, bus));

        assertTrue(NatureService.has(data, NatureType.WIND)); // la tiene de verdad, no es un set aparte
        assertTrue(data.natures().temporary().isTemporary(NatureType.WIND));
        assertEquals(ONE_TAIL, data.natures().temporary().sourceOf(NatureType.WIND).orElseThrow());
        assertEquals(20, data.progression().ninjaXp());
    }

    @Test
    void prestarLoQueYaEsPropioNoHaceNada() {
        // 'unless has_<nat>' del datapack: la fuente no pisa lo tuyo, asi no se lo lleva al marcharse.
        NatureService.grant(data, PLAYER, NatureType.WIND, bus);

        assertFalse(TemporaryGrantService.grantNature(data, PLAYER, NatureType.WIND, ONE_TAIL, bus));

        assertFalse(data.natures().temporary().isTemporary(NatureType.WIND));
        assertEquals(20, data.progression().ninjaXp()); // sin XP repetida
    }

    @Test
    void retirarLaFuenteNoTocaLoPermanente() {
        // EL caso critico del ticket: wind es propia, earth prestada. Al irse la bestia, wind se queda.
        NatureService.grant(data, PLAYER, NatureType.WIND, bus);
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.EARTH, ONE_TAIL, bus);

        assertEquals(1, TemporaryGrantService.revokeAllFrom(data, PLAYER, ONE_TAIL, bus));

        assertTrue(NatureService.has(data, NatureType.WIND)); // intacta
        assertFalse(NatureService.has(data, NatureType.EARTH)); // devuelta
        assertEquals(20, data.progression().ninjaXp()); // solo queda la de wind
        assertTrue(data.natures().temporary().isEmpty());
    }

    @Test
    void retirarUnaFuenteNoTocaLoPrestadoPorOtra() {
        // Mejora sobre el datapack: remove_tmp borraba TODOS los prestamos (bug con AllowMultJinchuriki).
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.WIND, ONE_TAIL, bus);
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.FIRE, NINE_TAILS, bus);

        TemporaryGrantService.revokeAllFrom(data, PLAYER, ONE_TAIL, bus);

        assertFalse(NatureService.has(data, NatureType.WIND));
        assertTrue(NatureService.has(data, NatureType.FIRE)); // sigue prestada por la otra bestia
        assertEquals(NINE_TAILS, data.natures().temporary().sourceOf(NatureType.FIRE).orElseThrow());
        assertEquals(25, data.progression().ninjaXp());
    }

    @Test
    void retirarUnaFuenteDesconocidaNoHaceNada() {
        NatureService.grant(data, PLAYER, NatureType.WIND, bus);

        assertEquals(0, TemporaryGrantService.revokeAllFrom(data, PLAYER, "no_existe", bus));

        assertTrue(NatureService.has(data, NatureType.WIND));
        assertEquals(20, data.progression().ninjaXp());
    }

    @Test
    void prestarUnKgImplicaSusComponentes() {
        // sealing/one_tail: presta wind y earth y luego el KG magnet.
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.WIND, ONE_TAIL, bus);
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.EARTH, ONE_TAIL, bus);
        assertTrue(TemporaryGrantService.grantKekkeiGenkai(data, PLAYER, KekkeiGenkai.MAGNET, ONE_TAIL, bus));

        assertTrue(data.natures().temporary().isTemporary(KekkeiGenkai.MAGNET));
        assertEquals(49, data.progression().ninjaXp()); // wind 20 + earth 20 + magnet 9
    }

    @Test
    void retirarLaFuenteDevuelveNaturalezasYKg() {
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.WIND, ONE_TAIL, bus);
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.EARTH, ONE_TAIL, bus);
        TemporaryGrantService.grantKekkeiGenkai(data, PLAYER, KekkeiGenkai.MAGNET, ONE_TAIL, bus);

        assertEquals(3, TemporaryGrantService.revokeAllFrom(data, PLAYER, ONE_TAIL, bus));

        assertTrue(data.natures().natures().isEmpty());
        assertTrue(data.natures().kekkeiGenkai().isEmpty());
        assertTrue(data.natures().temporary().isEmpty());
        assertEquals(0, data.progression().ninjaXp()); // el jugador queda como estaba
    }

    @Test
    void unKgPropioNoSePrestaNiSePierde() {
        KekkeiGenkaiService.grant(data, PLAYER, KekkeiGenkai.MAGNET, bus); // propio: wind+earth+magnet = 49

        assertFalse(TemporaryGrantService.grantKekkeiGenkai(data, PLAYER, KekkeiGenkai.MAGNET, ONE_TAIL, bus));
        TemporaryGrantService.revokeAllFrom(data, PLAYER, ONE_TAIL, bus);

        assertTrue(KekkeiGenkaiService.has(data, KekkeiGenkai.MAGNET));
        assertEquals(49, data.progression().ninjaXp());
    }

    @Test
    void unComponenteNoPrestadoAntesQuedaPermanente() {
        // Contrato para F7: prestar un KG NO presta implicitamente sus componentes; el sellado del datapack
        // los presta uno a uno ANTES del KG (ver sealing/ten_tails). Si no, se quedan como propios.
        TemporaryGrantService.grantKekkeiGenkai(data, PLAYER, KekkeiGenkai.MAGNET, ONE_TAIL, bus);

        assertEquals(Set.of(), data.natures().temporary().naturesFrom(ONE_TAIL)); // ningun componente prestado
        assertTrue(data.natures().temporary().isTemporary(KekkeiGenkai.MAGNET));

        TemporaryGrantService.revokeAllFrom(data, PLAYER, ONE_TAIL, bus);

        assertTrue(NatureService.has(data, NatureType.WIND)); // se quedan: nadie los presto
        assertTrue(NatureService.has(data, NatureType.EARTH));
        assertFalse(KekkeiGenkaiService.has(data, KekkeiGenkai.MAGNET));
    }

    @Test
    void losPrestamosSobrevivenAlRoundTripNbt() {
        // "recargar el mundo con un prestamo a medias": el estado es persistente, no un timer en memoria.
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.WIND, ONE_TAIL, bus);
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.EARTH, ONE_TAIL, bus);
        TemporaryGrantService.grantKekkeiGenkai(data, PLAYER, KekkeiGenkai.MAGNET, ONE_TAIL, bus);
        TemporaryGrantService.grantNature(data, PLAYER, NatureType.FIRE, NINE_TAILS, bus);

        PlayerNinjaData decoded = roundTrip(data);

        assertEquals(ONE_TAIL, decoded.natures().temporary().sourceOf(NatureType.WIND).orElseThrow());
        assertEquals(ONE_TAIL, decoded.natures().temporary().sourceOf(KekkeiGenkai.MAGNET).orElseThrow());
        assertEquals(Set.of(NatureType.WIND, NatureType.EARTH), decoded.natures().temporary().naturesFrom(ONE_TAIL));
        assertEquals(Set.of(NatureType.FIRE), decoded.natures().temporary().naturesFrom(NINE_TAILS));
    }

    private static PlayerNinjaData roundTrip(PlayerNinjaData original) {
        return PlayerNinjaData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,
                PlayerNinjaData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, original).getOrThrow())
                .getOrThrow();
    }
}
