package com.zamu.dojutsu.clans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.chakra.RegenService;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.SkillLevelChangedEvent;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.resource.RegenFactor;
import com.zamu.dojutsu.natures.KekkeiGenkaiDefinition;
import com.zamu.dojutsu.natures.KekkeiGenkaiRegistry;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests del join de clan (DJT-057) con las definiciones <b>reales</b> del datapack del mod: bonus exactos, una
 * sola vez, Namikaze determinista, Sarutobi aleatorio con azar inyectado, y factores de regen enchufados.
 */
class ClanServiceTest {

    private static final UUID PLAYER = UUID.nameUUIDFromBytes("test".getBytes());
    private static final Gson GSON = new Gson();

    private PlayerNinjaData data;
    private DomainEventBus bus;

    @BeforeEach
    void setUp() {
        data = new PlayerNinjaData();
        bus = new DomainEventBus();
        // KG reales necesarios por los joins (kaguya -> shikotsumyaku; namikaze -> swift).
        EnumMap<KekkeiGenkai, KekkeiGenkaiDefinition> kgs = new EnumMap<>(KekkeiGenkai.class);
        kgs.put(KekkeiGenkai.SHIKOTSUMYAKU, new KekkeiGenkaiDefinition(KekkeiGenkai.SHIKOTSUMYAKU,
                List.of(), 10, Map.of()));
        kgs.put(KekkeiGenkai.SWIFT, new KekkeiGenkaiDefinition(KekkeiGenkai.SWIFT,
                List.of(NatureType.WIND, NatureType.LIGHTNING), 9, Map.of()));
        KekkeiGenkaiRegistry.load(kgs);
    }

    @AfterEach
    void tearDown() {
        KekkeiGenkaiRegistry.load(Map.of());
        ClanRegistry.load(Map.of()); // registros estaticos: no contaminar otros tests
        RegenService.clearFactorProvidersForTests();
    }

    /** Carga la definicion real de {@code data/dojutsu/clans/<id>.json}. */
    private static ClanDefinition clan(String id) {
        try (InputStream in = ClanServiceTest.class.getResourceAsStream("/data/dojutsu/clans/" + id + ".json")) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return ClanDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar el clan " + id, e);
        }
    }

    @Test
    void unirseAUchihaAplicaSusBonusExactos() {
        assertTrue(ClanService.join(data, PLAYER, clan("uchiha"), new Random(1L), bus));

        assertTrue(data.clan().isIn("uchiha"));
        assertTrue(data.clan().initialized());
        assertTrue(data.natures().has(NatureType.FIRE));
        assertEquals(10, data.natures().affinity(NatureType.FIRE).level()); // aff_fire += 10 (no 50, ni 60)
        assertEquals(10, data.progression().skill(Skill.GENJUTSU).level()); // genjutsu += 10
        assertEquals(150, data.chakra().maxChakra());
        assertEquals(150, data.chakra().chakra()); // d.chakra = d.max_chakra (relleno)
        assertEquals(55, data.progression().ninjaXp()); // give/fire 25 + join 30
    }

    @Test
    void reJoinNoReAplicaNada() {
        ClanService.join(data, PLAYER, clan("uchiha"), new Random(1L), bus);
        int xp = data.progression().ninjaXp();

        assertFalse(ClanService.join(data, PLAYER, clan("uchiha"), new Random(1L), bus)); // mismo clan
        assertFalse(ClanService.join(data, PLAYER, clan("hyuga"), new Random(1L), bus)); // otro clan

        assertEquals(xp, data.progression().ninjaXp());
        assertEquals(10, data.natures().affinity(NatureType.FIRE).level());
        assertTrue(data.clan().isIn("uchiha")); // sigue en el suyo
    }

    @Test
    void namikazeEsDeterminista() {
        // Correccion S04-R3: Lightning +25 SIEMPRE (en el datapack dependia del temporal d.tmp no garantizado).
        for (long seed = 0; seed < 20; seed++) {
            PlayerNinjaData fresh = new PlayerNinjaData();
            assertTrue(ClanService.join(fresh, PLAYER, clan("namikaze"), new Random(seed), bus));
            assertEquals(25, fresh.natures().affinity(NatureType.LIGHTNING).level(), "semilla " + seed);
            assertEquals(0, fresh.natures().affinity(NatureType.WIND).level(), "semilla " + seed);
            // El KG swift implica wind y lightning como naturalezas.
            assertTrue(fresh.natures().has(KekkeiGenkai.SWIFT), "semilla " + seed);
            assertTrue(fresh.natures().has(NatureType.WIND), "semilla " + seed);
            assertTrue(fresh.natures().has(NatureType.LIGHTNING), "semilla " + seed);
        }
    }

    @Test
    void sarutobiConservaSuAzarIntencionado() {
        // join_sarutobi: roll 1..2 de random_aff_namikazi, Wind solo con el 1 (50%). Azar inyectado.
        RandomGenerator siempreAcierta = new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        RandomGenerator nuncaAcierta = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };

        PlayerNinjaData conWind = new PlayerNinjaData();
        ClanService.join(conWind, PLAYER, clan("sarutobi"), siempreAcierta, bus);
        assertTrue(conWind.natures().has(NatureType.WIND));
        assertTrue(conWind.natures().has(NatureType.FIRE));

        PlayerNinjaData sinWind = new PlayerNinjaData();
        ClanService.join(sinWind, PLAYER, clan("sarutobi"), nuncaAcierta, bus);
        assertFalse(sinWind.natures().has(NatureType.WIND));
        assertTrue(sinWind.natures().has(NatureType.FIRE));
    }

    @Test
    void leeNoTieneNaturalezasYSuChakraBajaA50() {
        // El caso "sin afinidad": taijutsu +30, maxChakra 50 (MENOR que el default 100: se clampa).
        data.setChakra(data.chakra().withChakra(100)); // chakra lleno antes del join

        assertTrue(ClanService.join(data, PLAYER, clan("lee"), new Random(1L), bus));

        assertTrue(data.natures().natures().isEmpty());
        assertEquals(30, data.progression().skill(Skill.TAIJUTSU).level());
        assertEquals(50, data.chakra().maxChakra());
        assertEquals(50, data.chakra().chakra()); // relleno al (nuevo) maximo
        assertEquals(90, data.progression().ninjaXp());
    }

    @Test
    void kaguyaRecibeSuKekkeiGenkai() {
        assertTrue(ClanService.join(data, PLAYER, clan("kaguya"), new Random(1L), bus));

        assertTrue(data.natures().has(KekkeiGenkai.SHIKOTSUMYAKU));
        assertEquals(10, data.progression().skill(Skill.KENJUTSU).level());
        assertEquals(30, data.progression().ninjaXp()); // shikotsumyaku 10 + join 20
    }

    @Test
    void losBonusDeHabilidadEmitenEventoParaLosDesbloqueos() {
        List<SkillLevelChangedEvent> events = new ArrayList<>();
        bus.subscribe(SkillLevelChangedEvent.class, events::add);

        ClanService.join(data, PLAYER, clan("hatake"), new Random(1L), bus);

        // taijutsu 10, kenjutsu 10, genjutsu 5: tres eventos (los hitos data-driven reaccionan a ellos).
        assertEquals(3, events.size());
        assertTrue(events.stream().anyMatch(e -> e.skillId().equals("kenjutsu") && e.newLevel() == 10));
    }

    @Test
    void losFactoresDeRegenSeDerivanDelClan() {
        // Deuda de F2 saldada: el clan DECLARA su factor en datos y clans lo aporta como proveedor
        // (asi chakra no depende de clans: sin ciclo de modulos).
        ClanService.registerRegenFactors();
        ClanRegistry.load(Map.of("otsutsuki", clan("otsutsuki"), "uzumaki", clan("uzumaki"),
                "uchiha", clan("uchiha")));

        PlayerNinjaData otsutsuki = new PlayerNinjaData();
        otsutsuki.setClan(otsutsuki.clan().withClan("otsutsuki"));
        assertEquals(Set.of(RegenFactor.CLAN_OTSUTSUKI), RegenService.factorsFor(otsutsuki));

        PlayerNinjaData uzumaki = new PlayerNinjaData();
        uzumaki.setClan(uzumaki.clan().withClan("uzumaki"));
        assertEquals(Set.of(RegenFactor.CLAN_UZUMAKI), RegenService.factorsFor(uzumaki));

        PlayerNinjaData uchiha = new PlayerNinjaData();
        uchiha.setClan(uchiha.clan().withClan("uchiha"));
        assertEquals(Set.of(), RegenService.factorsFor(uchiha)); // sin factor declarado -> regen estandar
    }

    @Test
    void elAdnDeHashiramaYElJugoSeDerivanDelPerfil() {
        PlayerNinjaData conAdn = new PlayerNinjaData();
        conAdn.setNatures(conAdn.natures().withHashiramaDna(true));
        assertEquals(Set.of(RegenFactor.HASHIRAMA_DNA), RegenService.factorsFor(conAdn));

        PlayerNinjaData conJugo = new PlayerNinjaData();
        conJugo.setNatures(conJugo.natures().withKekkeiGenkai(KekkeiGenkai.JUGO_CLAN));
        assertTrue(RegenService.hasJugoKekkeiGenkai(conJugo));
        assertFalse(RegenService.hasJugoKekkeiGenkai(new PlayerNinjaData()));
    }

    @Test
    void resetDejaLosCuatroSubEstadosEnDefaultsSinResiduos() {
        // Corrige S04-R2: el reset_clan del datapack olvidaba naturalezas/KG/jutsus (y kenjutsu, cc,
        // max_chakra y skill_points). Aqui: defaults atomicos -> imposible olvidar un campo.
        ClanService.join(data, PLAYER, clan("kaguya"), new Random(1L), bus); // KG + kenjutsu + chakra 100
        data.setProgression(data.progression().withSkillPoints(7).withUnlock("izanagi"));
        data.setNatures(data.natures().withHashiramaDna(true));

        ClanService.reset(data);

        assertFalse(data.clan().hasClan());
        assertFalse(data.clan().initialized());
        assertTrue(data.natures().natures().isEmpty());
        assertTrue(data.natures().kekkeiGenkai().isEmpty());
        assertFalse(data.natures().hashiramaDna());
        assertEquals(0, data.progression().skill(Skill.KENJUTSU).level());
        assertEquals(0, data.progression().skillPoints());
        assertEquals(0, data.progression().ninjaXp());
        assertTrue(data.progression().unlocked().isEmpty());
        assertEquals(100, data.chakra().maxChakra()); // de vuelta al default
    }

    @Test
    void resetSinClanYRepetidoSonInofensivos() {
        ClanService.reset(data); // sin clan
        ClanService.reset(data); // repetido
        assertFalse(data.clan().hasClan());
        assertEquals(100, data.chakra().maxChakra());
    }

    @Test
    void trasElResetSePuedeUnirAOtroClanConBonusCorrectos() {
        // El caso borde de la ficha ("unirse tras reset") y la razon de ser de S04-R2: sin arrastre.
        ClanService.join(data, PLAYER, clan("uchiha"), new Random(1L), bus);
        ClanService.reset(data);

        assertTrue(ClanService.join(data, PLAYER, clan("hyuga"), new Random(1L), bus));

        assertTrue(data.clan().isIn("hyuga"));
        assertFalse(data.natures().has(NatureType.FIRE)); // nada del Uchiha arrastrado
        assertEquals(0, data.natures().affinity(NatureType.FIRE).level());
        assertEquals(0, data.progression().skill(Skill.GENJUTSU).level());
        assertEquals(10, data.progression().skill(Skill.TAIJUTSU).level()); // el bonus del nuevo clan
        assertEquals(120, data.chakra().maxChakra());
        assertEquals(20, data.progression().ninjaXp()); // solo la del hyuga (sin residuo de 55)
    }

    @Test
    void elContadorDeJutsuDeClanAvanzaUnoPorTickYSeCapa() {
        // DJT-061: la correccion de la doble ejecucion se observa en que advance() suma EXACTAMENTE 1.
        ClanRegistry.load(Map.of("nara", clan("nara"), "uchiha", clan("uchiha")));
        data.setClan(data.clan().withClan("nara"));

        for (int i = 0; i < 100; i++) {
            assertTrue(ClanJutsuUnlockService.advance(data));
        }
        assertEquals(100, data.clan().jutsuTime()); // 100 ticks -> 100, no 200 (una vez por tick)
        assertFalse(ClanJutsuUnlockService.reachedThreshold(data));

        // En el umbral (78000, predicate new_nara_jutsu) se capa: no se pierde progreso ni desborda.
        data.setClan(data.clan().withJutsuTime(77_999));
        assertTrue(ClanJutsuUnlockService.advance(data));
        assertTrue(ClanJutsuUnlockService.reachedThreshold(data));
        assertFalse(ClanJutsuUnlockService.advance(data)); // capado: la entrega (y el reset) es F4
        assertEquals(78_000, data.clan().jutsuTime());
    }

    @Test
    void sinClanOClanSinDesbloqueoNoAcumula() {
        ClanRegistry.load(Map.of("uchiha", clan("uchiha")));

        assertFalse(ClanJutsuUnlockService.advance(data)); // sin clan

        data.setClan(data.clan().withClan("uchiha")); // Uchiha no declara jutsuUnlockTicks
        assertFalse(ClanJutsuUnlockService.advance(data));
        assertEquals(0, data.clan().jutsuTime());
    }

    @Test
    void elJoinYElResetDejanElContadorACero() {
        ClanRegistry.load(Map.of("nara", clan("nara")));
        ClanService.join(data, PLAYER, clan("nara"), new Random(1L), bus);
        for (int i = 0; i < 50; i++) {
            ClanJutsuUnlockService.advance(data);
        }
        assertEquals(50, data.clan().jutsuTime());

        ClanService.reset(data);
        assertEquals(0, data.clan().jutsuTime()); // defaults atomicos: el contador tambien
    }

    @Test
    void elPotentialDojutsuNoSeCopiaAlEstado() {
        // Derivado, nunca cacheado: el despertar (F5) lo leera de ClanRegistry via la pertenencia.
        ClanService.join(data, PLAYER, clan("uchiha"), new Random(1L), bus);
        assertEquals(List.of("sharingan"), clan("uchiha").potentialDojutsu());
        // No hay campo de dojutsu potencial en PlayerNinjaData: nada que pueda desincronizarse.
    }
}
