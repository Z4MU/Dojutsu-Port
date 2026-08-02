package com.zamu.dojutsu.natures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.random.RandomGenerator;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de la asignacion aleatoria (DJT-054): que el JSON <b>real</b> coincide con {@code get_random_kg} y que
 * la distribucion con semilla es la canonica del datapack.
 */
class NatureRollTest {

    private static final Gson GSON = new Gson();

    /** Carga la tabla real de {@code data/dojutsu/rng/nature_roll.json} (el contenido que se envia). */
    private static NatureRollTable loadReal() {
        try (InputStream in = NatureRollTest.class.getResourceAsStream("/data/dojutsu/rng/nature_roll.json")) {
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return NatureRollTable.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar nature_roll.json", e);
        }
    }

    @BeforeEach
    void setUp() {
        NatureRollRegistry.load(loadReal());
    }

    @AfterEach
    void tearDown() {
        NatureRollRegistry.load(NatureRollTable.EMPTY); // el registro es estatico
    }

    @Test
    void laTablaRealEsCoherente() {
        assertEquals(Optional.empty(), loadReal().validate());
    }

    @Test
    void losRangosCoincidenConElDatapack() {
        NatureRollTable table = loadReal();
        assertEquals(50, table.range()); // loot random_kg_rng = 1..50
        assertEquals(10, table.secondAffinityOneIn()); // random_aff_rng_2 = 1..10, acierta en 1
        assertEquals(7, table.genjutsuOneIn()); // gen_ulk_rng = 1..7, acierta en 1
        assertEquals(List.of(NatureType.FIRE, NatureType.WIND, NatureType.LIGHTNING, NatureType.EARTH,
                NatureType.WATER), table.basicAffinities()); // random_aff = 1..5
        assertEquals(17, table.entries().size()); // 16 tramos base + el de Senju
    }

    @Test
    void yangOcupaDosTiradasYEsLoUnicoQueNoEsKg() {
        // get_random_kg: 'matches 1..2' -> yang (rama rota en el datapack, corregida en DJT-051).
        List<NatureRollTable.Entry> natureEntries = loadReal().entries().stream()
                .filter(e -> e.nature().isPresent()).toList();
        assertEquals(1, natureEntries.size());
        assertEquals(NatureType.YANG, natureEntries.get(0).nature().orElseThrow());
        assertEquals(2, natureEntries.get(0).size()); // 1..2
    }

    @Test
    void losPesosSonLosDelDatapackParaNoSenju() {
        // 1..2 yang (2) + 3..17 KG (15) = 17 con premio; 18..50 = 33 sin premio. Total 50.
        var table = loadReal().rollTable(false);
        assertEquals(50, table.totalWeight());
        assertEquals(33, table.entries().stream()
                .filter(e -> !e.value().claimed()).mapToInt(e -> e.weight()).sum());
    }

    @Test
    void elSenjuAnadeMaderaSinCambiarElTotal() {
        // 30..40 (11) solo para Senju: 17 + 11 = 28 con premio, 22 sin premio. Total sigue siendo 50.
        var table = loadReal().rollTable(true);
        assertEquals(50, table.totalWeight());
        assertEquals(22, table.entries().stream()
                .filter(e -> !e.value().claimed()).mapToInt(e -> e.weight()).sum());
    }

    @Test
    void distribucionConSemillaReproduceLasProbabilidadesCanonicas() {
        // 2/50 = 4% yang; 1/50 = 2% cada KG; 33/50 = 66% sin premio.
        Map<String, Integer> counts = simulate(200_000, false);

        assertEquals(4.0, pct(counts, "yang", 200_000), 0.4);
        assertEquals(2.0, pct(counts, "wood", 200_000), 0.3);
        assertEquals(2.0, pct(counts, "mud", 200_000), 0.3);
        assertEquals(66.0, pct(counts, "(nada)", 200_000), 0.6);
    }

    @Test
    void elSenjuTieneUn22PorCientoDeMadera() {
        // 30..40 (11) + su 1/50 base = 12/50 = 24%.
        Map<String, Integer> counts = simulate(200_000, true);

        assertEquals(24.0, pct(counts, "wood", 200_000), 0.5);
        assertEquals(44.0, pct(counts, "(nada)", 200_000), 0.6); // 22/50
    }

    @Test
    void losKgDeClanNoSalenNunca() {
        // blaze, shikotsumyaku y jugo_clan no estan en get_random_kg.
        Map<String, Integer> counts = simulate(50_000, true);

        assertFalse(counts.containsKey("blaze"));
        assertFalse(counts.containsKey("shikotsumyaku"));
        assertFalse(counts.containsKey("jugo_clan"));
    }

    @Test
    void laSegundaAfinidadAciertaUnaDeCada10() {
        RandomGenerator random = new Random(1234L);
        int hits = 0;
        for (int i = 0; i < 100_000; i++) {
            if (NatureRoll.rollSecondAffinity(random)) {
                hits++;
            }
        }
        assertEquals(10.0, 100.0 * hits / 100_000, 0.4);
    }

    @Test
    void elGenjutsuAciertaUnaDeCada7() {
        RandomGenerator random = new Random(1234L);
        int hits = 0;
        for (int i = 0; i < 100_000; i++) {
            if (NatureRoll.rollGenjutsu(random)) {
                hits++;
            }
        }
        assertEquals(100.0 / 7, 100.0 * hits / 100_000, 0.4);
    }

    @Test
    void laAfinidadBasicaEsUniformeEntreLasCinco() {
        RandomGenerator random = new Random(99L);
        Map<NatureType, Integer> counts = new HashMap<>();
        for (int i = 0; i < 100_000; i++) {
            counts.merge(NatureRoll.rollBasicAffinity(random), 1, Integer::sum);
        }
        assertEquals(5, counts.size());
        for (NatureType basic : NatureType.basics()) {
            assertEquals(20.0, 100.0 * counts.get(basic) / 100_000, 0.5);
        }
    }

    @Test
    void sinTablaCargadaNoSeOtorgaNada() {
        NatureRollRegistry.load(NatureRollTable.EMPTY);
        RandomGenerator random = new Random(1L);

        NatureRollTable.Outcome outcome = NatureRoll.rollKekkeiGenkai(random, true);

        assertTrue(outcome.kekkeiGenkai().isEmpty());
        assertTrue(outcome.nature().isEmpty());
        assertFalse(outcome.claimed());
        assertFalse(NatureRoll.rollGenjutsu(random));
        assertFalse(NatureRoll.rollSecondAffinity(random));
    }

    @Test
    void unaTablaConTramosSolapadosSeRechaza() {
        // [S03-R4]: unos pesos mal editados alterarian las probabilidades en silencio.
        NatureRollTable overlapping = new NatureRollTable(50, List.of(
                new NatureRollTable.Entry(1, 5, Optional.empty(), Optional.of(KekkeiGenkai.WOOD), false),
                new NatureRollTable.Entry(5, 9, Optional.empty(), Optional.of(KekkeiGenkai.ICE), false)
        ), 10, 7, List.of(NatureType.FIRE));
        assertTrue(overlapping.validate().isPresent());

        NatureRollTable outOfRange = new NatureRollTable(50, List.of(
                new NatureRollTable.Entry(45, 60, Optional.empty(), Optional.of(KekkeiGenkai.WOOD), false)
        ), 10, 7, List.of(NatureType.FIRE));
        assertTrue(outOfRange.validate().isPresent());

        NatureRollTable twoPrizes = new NatureRollTable(50, List.of(
                new NatureRollTable.Entry(1, 2, Optional.of(NatureType.YANG), Optional.of(KekkeiGenkai.WOOD), false)
        ), 10, 7, List.of(NatureType.FIRE));
        assertTrue(twoPrizes.validate().isPresent());
    }

    @Test
    void elListenerRechazaUnaTablaConTramosSolapados() {
        // Cubre el camino real de /reload (parseo -> validate -> registro), no solo validate() por su cuenta:
        // una tabla incoherente debe dejar el registro VACIO en vez de repartir con probabilidades erroneas.
        JsonElement broken = GSON.fromJson("""
                {
                  "range": 50,
                  "entries": [
                    { "min": 3, "max": 3, "kekkeiGenkai": "wood" },
                    { "min": 3, "max": 3, "kekkeiGenkai": "mud" }
                  ],
                  "secondAffinityOneIn": 10,
                  "genjutsuOneIn": 7,
                  "basicAffinities": ["fire"]
                }
                """, JsonElement.class);

        applyToListener(broken);

        assertEquals(0, NatureRollRegistry.table().range()); // rechazada: registro vacio
        assertFalse(NatureRoll.rollKekkeiGenkai(new Random(1L), true).claimed());
    }

    @Test
    void elListenerCargaLaTablaRealDesdeElDatapack() {
        NatureRollRegistry.load(NatureRollTable.EMPTY);

        applyToListener(realJson());

        assertEquals(50, NatureRollRegistry.table().range());
        assertEquals(17, NatureRollRegistry.table().entries().size());
    }

    @Test
    void sinArchivoElRegistroQuedaVacio() {
        NatureRollRegistry.load(loadReal());

        new NatureRollReloadListener().apply(Map.of(), null, null); // el datapack no trae nature_roll.json

        assertEquals(0, NatureRollRegistry.table().range());
    }

    /** Ejecuta el reload listener con un unico archivo {@code dojutsu:nature_roll}, como haria {@code /reload}. */
    private static void applyToListener(JsonElement json) {
        new NatureRollReloadListener().apply(
                Map.of(ResourceLocation.fromNamespaceAndPath("dojutsu", "nature_roll"), json), null, null);
    }

    private static JsonElement realJson() {
        try (InputStream in = NatureRollTest.class.getResourceAsStream("/data/dojutsu/rng/nature_roll.json")) {
            return GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
        } catch (Exception e) {
            throw new AssertionError("no se pudo leer nature_roll.json", e);
        }
    }

    private static Map<String, Integer> simulate(int times, boolean senju) {
        RandomGenerator random = new Random(4242L); // semilla fija: test determinista
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < times; i++) {
            NatureRollTable.Outcome outcome = NatureRoll.rollKekkeiGenkai(random, senju);
            String key = outcome.kekkeiGenkai().map(KekkeiGenkaiService::id)
                    .or(() -> outcome.nature().map(NatureService::id))
                    .orElse("(nada)");
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    private static double pct(Map<String, Integer> counts, String key, int total) {
        return 100.0 * counts.getOrDefault(key, 0) / total;
    }
}
