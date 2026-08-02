package com.zamu.dojutsu.core.onboarding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.clans.ClanDefinition;
import com.zamu.dojutsu.clans.ClanRegistry;
import com.zamu.dojutsu.clans.ClanService;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.OnboardingState.Phase;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.natures.KekkeiGenkaiDefinition;
import com.zamu.dojutsu.natures.KekkeiGenkaiRegistry;
import com.zamu.dojutsu.natures.NatureRollRegistry;
import com.zamu.dojutsu.natures.NatureRollTable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de la maquina de estados de la creacion del ninja (DJT-059), con las definiciones <b>reales</b> de
 * clanes y la tabla real de RNG del datapack del mod, y el azar <b>controlado</b> por una cola de valores.
 */
class OnboardingControllerTest {

    private static final UUID PLAYER = UUID.nameUUIDFromBytes("test".getBytes());
    private static final Gson GSON = new Gson();

    private PlayerNinjaData data;
    private DomainEventBus bus;

    @BeforeEach
    void setUp() {
        data = new PlayerNinjaData();
        bus = new DomainEventBus();
        // Clanes reales del datapack del mod.
        Map<String, ClanDefinition> clans = new HashMap<>();
        for (String id : List.of("uchiha", "lee", "hyuga", "senju", "nara")) {
            clans.put(id, loadClan(id));
        }
        ClanRegistry.load(clans);
        // KG reales que pueden salir en los tests (wood, el tramo r=2 del roll).
        EnumMap<KekkeiGenkai, KekkeiGenkaiDefinition> kgs = new EnumMap<>(KekkeiGenkai.class);
        KekkeiGenkaiDefinition wood = loadKekkeiGenkai("wood");
        kgs.put(wood.kekkeiGenkai(), wood);
        KekkeiGenkaiRegistry.load(kgs);
        // Tabla de RNG real (data/dojutsu/rng/nature_roll.json).
        NatureRollRegistry.load(loadRollTable());
    }

    @AfterEach
    void tearDown() {
        ClanRegistry.load(Map.of());
        KekkeiGenkaiRegistry.load(Map.of());
        NatureRollRegistry.load(NatureRollTable.EMPTY); // registros estaticos: no contaminar
    }

    private static ClanDefinition loadClan(String id) {
        try (InputStream in = OnboardingControllerTest.class
                .getResourceAsStream("/data/dojutsu/clans/" + id + ".json")) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return ClanDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar el clan " + id, e);
        }
    }

    private static KekkeiGenkaiDefinition loadKekkeiGenkai(String id) {
        try (InputStream in = OnboardingControllerTest.class
                .getResourceAsStream("/data/dojutsu/kekkei_genkai/" + id + ".json")) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return KekkeiGenkaiDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar el kekkei genkai " + id, e);
        }
    }

    private static NatureRollTable loadRollTable() {
        try (InputStream in = OnboardingControllerTest.class
                .getResourceAsStream("/data/dojutsu/rng/nature_roll.json")) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return NatureRollTable.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar nature_roll.json", e);
        }
    }

    /** Azar controlado: devuelve los valores en orden (modulo el bound pedido). */
    private static RandomGenerator fixed(int... values) {
        return new Random() {
            private int index;

            @Override
            public int nextInt(int bound) {
                int value = values[index++ % values.length];
                return value % bound;
            }
        };
    }

    /** Sin premio de KG (49 -> nada), sin 2a afinidad (5 de 10), sin genjutsu (3 de 7). */
    private static RandomGenerator quietRolls() {
        return fixed(49, 5, 3);
    }

    @Test
    void elFlujoCompletoUchihaConAfinidadElegida() {
        OnboardingController.refresh(data); // login de un jugador nuevo
        assertEquals(Phase.SELECTING_CLAN, data.onboarding().phase());

        assertTrue(OnboardingController.chooseClan(data, PLAYER, "uchiha", quietRolls(), bus));
        assertEquals(Phase.SELECTING_AFFINITY, data.onboarding().phase());
        assertTrue(data.clan().isIn("uchiha"));

        assertTrue(OnboardingController.chooseAffinity(data, PLAYER, "water", quietRolls(), bus));
        assertEquals(Phase.COMPLETED, data.onboarding().phase());
        assertTrue(data.natures().has(NatureType.WATER));
        assertEquals(50, data.natures().affinity(NatureType.WATER).level()); // la elegida arranca en 50
        assertEquals(75, data.progression().ninjaXp()); // uchiha 55 (fire 25 + join 30) + water 20
    }

    @Test
    void laAfinidadElegidaSeFijaA50NoSeSuma() {
        // join_uchiha: aff_fire += 10; nature_select: SET aff_fire 50 -> queda 50, no 60.
        OnboardingController.refresh(data);
        OnboardingController.chooseClan(data, PLAYER, "uchiha", quietRolls(), bus);

        OnboardingController.chooseAffinity(data, PLAYER, "fire", quietRolls(), bus);

        assertEquals(50, data.natures().affinity(NatureType.FIRE).level());
    }

    @Test
    void leeSaltaLaAfinidadYElRollDeKg() {
        OnboardingController.refresh(data);

        // El azar nunca se consulta: si lo hiciera, fixed() sin valores petaria... se usa uno explosivo.
        RandomGenerator explosive = new Random() {
            @Override
            public int nextInt(int bound) {
                throw new AssertionError("Lee no debe tirar ningun roll (join_finish: unless is_lee)");
            }
        };
        assertTrue(OnboardingController.chooseClan(data, PLAYER, "lee", explosive, bus));

        assertEquals(Phase.COMPLETED, data.onboarding().phase()); // directo, sin fase de afinidad
        assertTrue(data.natures().natures().isEmpty()); // sin naturaleza, sin KG
        assertEquals(30, data.progression().skill(Skill.TAIJUTSU).level());
        assertEquals(90, data.progression().ninjaXp());
    }

    @Test
    void unPayloadDeFaseIncorrectaSeRechazaSinEfecto() {
        OnboardingController.refresh(data);

        // Afinidad antes de clan: rechazada.
        assertFalse(OnboardingController.chooseAffinity(data, PLAYER, "fire", quietRolls(), bus));
        assertEquals(Phase.SELECTING_CLAN, data.onboarding().phase());

        OnboardingController.chooseClan(data, PLAYER, "uchiha", quietRolls(), bus);

        // Segundo clan en fase de afinidad: rechazado (y el join tampoco re-aplicaria).
        assertFalse(OnboardingController.chooseClan(data, PLAYER, "hyuga", quietRolls(), bus));
        assertTrue(data.clan().isIn("uchiha"));

        OnboardingController.chooseAffinity(data, PLAYER, "water", quietRolls(), bus);

        // Todo repetido tras COMPLETED: sin efecto.
        assertFalse(OnboardingController.chooseAffinity(data, PLAYER, "fire", quietRolls(), bus));
        assertFalse(OnboardingController.chooseClan(data, PLAYER, "hyuga", quietRolls(), bus));
    }

    @Test
    void unaEleccionInvalidaSeRechaza() {
        OnboardingController.refresh(data);
        assertFalse(OnboardingController.chooseClan(data, PLAYER, "konoha", quietRolls(), bus)); // no existe

        OnboardingController.chooseClan(data, PLAYER, "uchiha", quietRolls(), bus);
        assertFalse(OnboardingController.chooseAffinity(data, PLAYER, "yin", quietRolls(), bus)); // no basica
        assertFalse(OnboardingController.chooseAffinity(data, PLAYER, "plasma", quietRolls(), bus));
        assertEquals(Phase.SELECTING_AFFINITY, data.onboarding().phase()); // sigue esperando
    }

    @Test
    void laDesconexionAMitadReanudaEnLaFaseCorrecta() {
        OnboardingController.refresh(data);
        OnboardingController.chooseClan(data, PLAYER, "uchiha", quietRolls(), bus);
        assertEquals(Phase.SELECTING_AFFINITY, data.onboarding().phase());

        // "Relogin": refresh no debe tocar una fase intermedia (la fase persiste en el agregado).
        assertFalse(OnboardingController.refresh(data));
        assertEquals(Phase.SELECTING_AFFINITY, data.onboarding().phase());

        // Y los bonus del clan no se re-aplican al continuar (initialized).
        assertEquals(10, data.natures().affinity(NatureType.FIRE).level());
    }

    @Test
    void elResetDeClanReembarca() {
        // La pregunta abierta de DJT-058, decidida aqui: COMPLETED sin clan -> SELECTING_CLAN de nuevo.
        OnboardingController.refresh(data);
        OnboardingController.chooseClan(data, PLAYER, "lee", quietRolls(), bus);
        assertEquals(Phase.COMPLETED, data.onboarding().phase());

        ClanService.reset(data);
        assertTrue(OnboardingController.refresh(data));
        assertEquals(Phase.SELECTING_CLAN, data.onboarding().phase());

        // Y puede elegir otro clan con bonus correctos (el reset atomico evito el arrastre).
        assertTrue(OnboardingController.chooseClan(data, PLAYER, "hyuga", quietRolls(), bus));
        assertTrue(data.clan().isIn("hyuga"));
    }

    @Test
    void elClanAleatorioEsUniformeSobreLosCargados() {
        // join_random (DJT-060): uniforme sobre los ids ORDENADOS [hyuga, lee, nara, senju, uchiha].
        OnboardingController.refresh(data);

        // nextInt(5)=4 -> uchiha; uchiha no salta la afinidad -> SELECTING_AFFINITY.
        assertTrue(OnboardingController.chooseClan(data, PLAYER, "random", fixed(4), bus));
        assertTrue(data.clan().isIn("uchiha"));
        assertEquals(Phase.SELECTING_AFFINITY, data.onboarding().phase());
    }

    @Test
    void elClanAleatorioPuedeDarLeeYSaltarLaAfinidad() {
        // El loot clan_rng del datapack INCLUIA a Lee (indice 14): random puede completar el ninja de una.
        OnboardingController.refresh(data);

        assertTrue(OnboardingController.chooseClan(data, PLAYER, "random", fixed(1), bus)); // 1 -> lee
        assertTrue(data.clan().isIn("lee"));
        assertEquals(Phase.COMPLETED, data.onboarding().phase());
        assertEquals(90, data.progression().ninjaXp()); // y sin ningun roll de finish
    }

    @Test
    void laAfinidadAleatoriaEsUnaDeLasCincoBasicas() {
        OnboardingController.refresh(data);
        OnboardingController.chooseClan(data, PLAYER, "hyuga", quietRolls(), bus);

        // random -> rollBasicAffinity(nextInt(5)=2 -> lightning) y luego los rolls silenciosos.
        assertTrue(OnboardingController.chooseAffinity(data, PLAYER, "random", fixed(2, 49, 5, 3), bus));

        assertTrue(data.natures().has(NatureType.LIGHTNING));
        assertEquals(50, data.natures().affinity(NatureType.LIGHTNING).level());
        assertEquals(Phase.COMPLETED, data.onboarding().phase());
    }

    @Test
    void elFinishPuedeDarKekkeiGenkaiConSuBonusDeXp() {
        OnboardingController.refresh(data);
        OnboardingController.chooseClan(data, PLAYER, "hyuga", quietRolls(), bus);

        // Roll de KG = 2 -> wood (tramo 3..3 => r=2); premio => sin 2a afinidad; genjutsu no (3 de 7); +20.
        OnboardingController.chooseAffinity(data, PLAYER, "fire", fixed(2, 3), bus);

        assertTrue(data.natures().has(KekkeiGenkai.WOOD));
        assertTrue(data.natures().has(NatureType.EARTH)); // implicados por el KG
        assertTrue(data.natures().has(NatureType.WATER));
        // hyuga 20 + fire 25 + wood(10 + earth 20 + water 20) + premio 20 = 115
        assertEquals(115, data.progression().ninjaXp());
        assertEquals(Phase.COMPLETED, data.onboarding().phase());
    }

    @Test
    void elFinishPuedeDarYangGenjutsuYSegundaAfinidad() {
        OnboardingController.refresh(data);
        OnboardingController.chooseClan(data, PLAYER, "hyuga", quietRolls(), bus);

        // Roll KG = 0 -> yang (la rama que el datapack tenia rota, S03-R1); premio; genjutsu SI (0 de 7); +20.
        OnboardingController.chooseAffinity(data, PLAYER, "fire", fixed(0, 0), bus);
        assertTrue(data.natures().has(NatureType.YANG));
        assertTrue(data.natures().has(NatureType.YIN)); // del acierto de genjutsu
        assertEquals(10, data.progression().skill(Skill.GENJUTSU).level()); // genjutsu +10 niveles

        // Y en otro ninja: sin premio (49) -> 2a afinidad SI (0 de 10) -> earth (3 de 5); genjutsu no.
        PlayerNinjaData otro = new PlayerNinjaData();
        OnboardingController.refresh(otro);
        OnboardingController.chooseClan(otro, PLAYER, "hyuga", quietRolls(), bus);
        OnboardingController.chooseAffinity(otro, PLAYER, "fire", fixed(49, 0, 3, 5), bus);
        assertTrue(otro.natures().has(NatureType.EARTH)); // la 2a afinidad
        assertEquals(50, otro.natures().affinity(NatureType.EARTH).level()); // random_aff: set 50
    }
}
