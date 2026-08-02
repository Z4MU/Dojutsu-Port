package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.domain.eye.EyeEvolution;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.eye.EyeRequirement;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de las definiciones <b>reales</b> de {@code data/dojutsu/eyes/*.json} (DJT-113): que parsean, que su id
 * coincide con el archivo y que el <b>grafo de evolucion</b> (Sharingan -> Mangekyo -> EMS, con rutas
 * divergentes) refleja el datapack {@code [S05]}. Los valores estan verificados contra los predicates/funciones
 * ({@code sharingan_use_level_4} = 360000, {@code tenseigan_req2} = 864000, awaken +20/+100...).
 */
class EyeDefinitionTest {

    private static final Gson GSON = new Gson();

    private static final List<String> EYES =
            List.of("byakugan", "sharingan", "mangekyo_itachi", "rinnegan", "tenseigan", "ketsuryugan", "naka");

    private static EyeDefinition load(String id) {
        String path = "/data/dojutsu/eyes/" + id + ".json";
        try (InputStream in = EyeDefinitionTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "falta el JSON: " + path);
            JsonElement json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonElement.class);
            return EyeDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            throw new AssertionError("no se pudo cargar " + path, e);
        }
    }

    private static EyeEvolution evolutionTo(EyeDefinition eye, String target) {
        return eye.evolutions().stream().filter(e -> e.to().equals(target)).findFirst()
                .orElseThrow(() -> new AssertionError(eye.id() + " no evoluciona a " + target));
    }

    private static Optional<EyeRequirement> requirement(EyeEvolution evolution, EyeRequirement.Type type) {
        return evolution.requirements().stream().filter(r -> r.type() == type).findFirst();
    }

    @Test
    void todosLosOjosRepresentativosParseanYCoincideElId() {
        for (String id : EYES) {
            assertEquals(id, load(id).id(), "el id del JSON no coincide con su nombre de archivo");
        }
    }

    @Test
    void bonusDeDespertarCoincidenConElDatapack() {
        assertEquals(20, load("byakugan").awakenMaxChakraBonus());
        assertEquals(20, load("byakugan").awakenNinjaXpBonus());
        assertEquals(20, load("sharingan").awakenMaxChakraBonus());
        assertEquals(100, load("rinnegan").awakenMaxChakraBonus());
        assertEquals(150, load("rinnegan").awakenNinjaXpBonus());
        assertEquals(100, load("tenseigan").awakenMaxChakraBonus());
        assertEquals(30, load("ketsuryugan").awakenMaxChakraBonus());
    }

    @Test
    void soloLosOjosDespertablesPorClanTienenAwakenTicks() {
        // Byakugan/Sharingan 78000, Ketsuryugan 24000 (de <eye>_waketime); los destinos de evolución no.
        assertEquals(78000, load("byakugan").awakenTicks().orElseThrow());
        assertEquals(78000, load("sharingan").awakenTicks().orElseThrow());
        assertEquals(24000, load("ketsuryugan").awakenTicks().orElseThrow());
        assertTrue(load("mangekyo_itachi").awakenTicks().isEmpty(), "el Mangekyo es evolución, no despierta por clan");
        assertTrue(load("rinnegan").awakenTicks().isEmpty());
        assertTrue(load("tenseigan").awakenTicks().isEmpty());
    }

    @Test
    void soloElSharinganFijaUsoInicialAlDespertar() {
        // sharingan/awaken del datapack hace d.sharingan_use=78000 (nace en tomoe 1); los demas no fijan uso.
        assertEquals(78000, load("sharingan").awakenInitialUse().orElseThrow());
        assertTrue(load("byakugan").awakenInitialUse().isEmpty());
        assertTrue(load("ketsuryugan").awakenInitialUse().isEmpty());
    }

    @Test
    void elSharinganEvolucionaAMangekyoConUmbralYClanCorrectos() {
        EyeDefinition sharingan = load("sharingan");
        assertEquals(EyeFamily.SHARINGAN, sharingan.family());

        EyeEvolution toMangekyo = evolutionTo(sharingan, "mangekyo_itachi");
        // Ruta divergente (R6): solo Uchiha/Otsutsuki, y con el umbral de uso de sharingan_use_level_4.
        assertEquals("otsutsuki_uchiha", requirement(toMangekyo, EyeRequirement.Type.CLAN).orElseThrow().value());
        assertEquals(360000, requirement(toMangekyo, EyeRequirement.Type.USE_LEVEL).orElseThrow().min());
    }

    @Test
    void elSharinganEvolucionaARinneganPorAdnDeHashirama() {
        EyeEvolution toRinnegan = evolutionTo(load("sharingan"), "rinnegan");
        assertEquals("uchiha", requirement(toRinnegan, EyeRequirement.Type.CLAN).orElseThrow().value());
        assertEquals("hashirama_dna", requirement(toRinnegan, EyeRequirement.Type.FLAG).orElseThrow().value());
    }

    @Test
    void elByakuganEvolucionaATenseiganConElUmbralDeUso() {
        EyeEvolution toTenseigan = evolutionTo(load("byakugan"), "tenseigan");
        assertEquals("otsutsuki", requirement(toTenseigan, EyeRequirement.Type.CLAN).orElseThrow().value());
        assertEquals(864000, requirement(toTenseigan, EyeRequirement.Type.USE_LEVEL).orElseThrow().min());
    }

    @Test
    void mangekyoEsEternalizablePorOtsutsukiUchiha() {
        EyeDefinition mangekyo = load("mangekyo_itachi");
        assertEquals(EyeFamily.MANGEKYO, mangekyo.family());
        assertTrue(mangekyo.isEternalizable(), "el Mangekyo debe poder volverse eterno (EMS)");
        assertEquals("otsutsuki_uchiha",
                mangekyo.eternalRequirements().get(0).value());
        assertTrue(mangekyo.abilities().contains("amaterasu"), "Amaterasu es el piloto de DJT-118");
    }

    @Test
    void elPlaceholderCargaPeroNoEsJugable() {
        EyeDefinition naka = load("naka"); // Eye:18 del datapack (R7): existe pero sin implementacion real
        assertFalse(naka.implemented());
        assertTrue(naka.abilities().isEmpty());
        assertTrue(naka.evolutions().isEmpty());
    }

    @Test
    void losOjosConVisionLaDanDesdeQueSeLlevan() {
        // byakugan/mangekyo/rinnegan/tenseigan corren chakra_vision unless no_cv, sin umbral de uso (DJT-125)
        for (String id : List.of("byakugan", "mangekyo_itachi", "rinnegan", "tenseigan")) {
            assertTrue(load(id).grantsChakraVision(0), id + " debe dar chakra vision desde el principio");
        }
    }

    @Test
    void elSharinganBaseNoDaVisionPeroElMaduroSi() {
        // el datapack da vision solo al Sharingan maduro (Eye:4, 3 tomoe): sharingan_use_level_3 = 216000..
        EyeDefinition sharingan = load("sharingan");
        assertFalse(sharingan.grantsChakraVision(78000), "1 tomoe (recien despierto) no da vision");
        assertFalse(sharingan.grantsChakraVision(215999), "por debajo del maduro, sin vision");
        assertTrue(sharingan.grantsChakraVision(216000), "3 tomoe (maduro) ya da vision");
    }

    @Test
    void elMangekyoDeSasukeConcedeElBlazeAlDespertar() {
        // natures/give/kg/blaze: el datapack da Blaze Release al despertar el Mangekyo de Sasuke (data-driven)
        assertEquals("blaze", load("mangekyo_sasuke").awakenKekkeiGenkai().orElseThrow());
        // los demas Mangekyo no conceden ninguno
        assertTrue(load("mangekyo_itachi").awakenKekkeiGenkai().isEmpty());
        assertTrue(load("mangekyo_obito").awakenKekkeiGenkai().isEmpty());
    }

    @Test
    void shisuiDeclaraSusDosTecnicas() {
        // afterglow (kotoamatsukami) + beams_of_light, ambas suyas en el datapack
        List<String> abilities = load("mangekyo_shisui").abilities();
        assertTrue(abilities.contains("kotoamatsukami"));
        assertTrue(abilities.contains("beams_of_light"));
    }

    @Test
    void elKetsuryuganNoDaVision() {
        // no esta en ninguna lista de ojos que corren chakra_vision en el datapack
        assertFalse(load("ketsuryugan").grantsChakraVision(Integer.MAX_VALUE));
    }
}
