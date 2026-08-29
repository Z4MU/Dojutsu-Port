package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.zamu.dojutsu.dojutsu.EyeCostResolver.Context;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * Tests del coste pasivo ocular (F11/DJT-238) contra los <b>datos reales</b> de los ojos.
 *
 * <p>No se inventan tablas aqui: se carga el JSON del ojo tal como lo lee el juego y se comprueba que devuelve
 * los numeros del {@code cost.mcfunction} del datapack. Si alguien toca un dato, este test lo caza.
 *
 * <p>Umbrales de tomoe (de los predicados {@code sharingan_use_level_N}): tomoe 1 hasta 144001, tomoe 2 hasta
 * 216000, tomoe 3 desde ahi.
 */
class EyeCostResolverTest {

    private static final int TOMOE_1 = 100_000;
    private static final int TOMOE_2 = 150_000;
    private static final int TOMOE_3 = 300_000;

    /** Carga la definicion real del ojo desde los recursos del mod. */
    private static EyeDefinition eye(String id) {
        String path = "/data/dojutsu/eyes/" + id + ".json";
        try (InputStream in = EyeCostResolverTest.class.getResourceAsStream(path)) {
            assertTrue(in != null, "no se encontro " + path);
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return EyeDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
        } catch (IOException failure) {
            throw new AssertionError("no se pudo leer " + path, failure);
        }
    }

    private static int cost(String eyeId, Context context) {
        OptionalInt result = EyeCostResolver.costFor(eye(eyeId), context);
        assertTrue(result.isPresent(), "el ojo " + eyeId + " deberia saber cuanto cobrar en este caso");
        return result.getAsInt();
    }

    private static Context ctx(String clan, boolean awakened, boolean eyeMatch, boolean mangekyo, int use) {
        return new Context(Optional.ofNullable(clan), awakened, eyeMatch, mangekyo, use);
    }

    // ------------------------------------------------------------------
    // Byakugan, Ketsuryugan, Tenseigan: despierto / implantado
    // ------------------------------------------------------------------

    @Test
    void elByakuganCuestaElDobleImplantado() {
        assertEquals(8, cost("byakugan", ctx("hyuga", true, true, false, 0)));
        assertEquals(12, cost("byakugan", ctx("hyuga", false, false, false, 0)));
    }

    @Test
    void elTenseiganEsElMasCaroDelJuego() {
        assertEquals(20, cost("tenseigan", ctx("otsutsuki", true, true, false, 0)));
        assertEquals(40, cost("tenseigan", ctx("nara", false, false, false, 0)));
    }

    @Test
    void elKetsuryuganCobra14o28() {
        assertEquals(14, cost("ketsuryugan", ctx("chinoike", true, true, false, 0)));
        assertEquals(28, cost("ketsuryugan", ctx("chinoike", false, false, false, 0)));
    }

    // ------------------------------------------------------------------
    // Sharingan: tres niveles de clan x tres tomoe. Los doce numeros.
    // ------------------------------------------------------------------

    @Test
    void unUchihaDespiertoPagaLaTarifaBarataPorTomoe() {
        assertEquals(6, cost("sharingan", ctx("uchiha", true, true, false, TOMOE_1)));
        assertEquals(7, cost("sharingan", ctx("uchiha", true, true, false, TOMOE_2)));
        assertEquals(8, cost("sharingan", ctx("uchiha", true, true, false, TOMOE_3)));
    }

    @Test
    void tenerElMangekyoAbarataElSharinganDeTresTomoe() {
        // el datapack lo hace mas barato, no mas caro: ya no fuerzas el ojo base
        assertEquals(4, cost("sharingan", ctx("uchiha", true, true, true, TOMOE_3)));
        assertEquals(8, cost("sharingan", ctx("uchiha", false, false, true, TOMOE_3)));
    }

    @Test
    void unUchihaSinDespertarPagaElDoble() {
        assertEquals(12, cost("sharingan", ctx("uchiha", false, false, false, TOMOE_1)));
        assertEquals(14, cost("sharingan", ctx("uchiha", false, false, false, TOMOE_2)));
        assertEquals(16, cost("sharingan", ctx("uchiha", false, false, false, TOMOE_3)));
    }

    @Test
    void elHatakePagaLoMismoDespiertoQueNo() {
        // Kakashi: un forastero que sabe usarlo. Ni el precio de un Uchiha ni el castigo de un ladron.
        assertEquals(12, cost("sharingan", ctx("hatake", true, true, false, TOMOE_1)));
        assertEquals(12, cost("sharingan", ctx("hatake", false, false, false, TOMOE_1)));
        assertEquals(16, cost("sharingan", ctx("hatake", false, false, false, TOMOE_3)));
        assertEquals(8, cost("sharingan", ctx("hatake", false, false, true, TOMOE_3)));
    }

    @Test
    void otroClanPagaLaTarifaDeLadron() {
        // ESTE es el nivel que la tabla del plan se habia dejado: 16/18/20/12, la penalizacion mas dura.
        assertEquals(16, cost("sharingan", ctx("nara", false, false, false, TOMOE_1)));
        assertEquals(18, cost("sharingan", ctx("nara", false, false, false, TOMOE_2)));
        assertEquals(20, cost("sharingan", ctx("nara", false, false, false, TOMOE_3)));
        assertEquals(12, cost("sharingan", ctx("nara", false, false, true, TOMOE_3)));
    }

    @Test
    void sinClanSeCobraComoOtroClan() {
        assertEquals(20, cost("sharingan", ctx(null, false, false, false, TOMOE_3)));
    }

    // ------------------------------------------------------------------
    // Mangekyo y Rinnegan: aqui vive el eyematch
    // ------------------------------------------------------------------

    @Test
    void llevarElMangekyoDeOtroUchihaCuestaMas() {
        // el motivo por el que EyeState guarda QUE variante despertaste, no solo la familia
        assertEquals(14, cost("mangekyo_itachi", ctx("uchiha", true, true, false, 0)));
        assertEquals(18, cost("mangekyo_itachi", ctx("uchiha", true, false, false, 0)));
    }

    @Test
    void unMangekyoRobadoPorOtroClanCuesta28() {
        assertEquals(28, cost("mangekyo_itachi", ctx("nara", false, false, false, 0)));
        assertEquals(28, cost("mangekyo_itachi", ctx("uchiha", false, false, false, 0)));
        assertEquals(18, cost("mangekyo_itachi", ctx("hatake", false, false, false, 0)));
    }

    @Test
    void todaslasVariantesDeMangekyoCuestanIgual() {
        for (String variant : new String[] {"itachi", "obito", "sasuke", "madara", "indra", "shisui",
                "fugaku", "shin"}) {
            assertEquals(14, cost("mangekyo_" + variant, ctx("uchiha", true, true, false, 0)),
                    "mangekyo_" + variant);
        }
    }

    @Test
    void elRinneganSinEyematchYaNoEsGratis() {
        // DESVIACION DELIBERADA: rinnegan/cost_0.mcfunction solo cobra "if eyematch" y no tiene rama else, asi
        // que en el datapack sale GRATIS. Se cobra 18, que es la tarifa que el propio datapack aplica a esos
        // clanes con el Rinnegan sin despertar: no se inventa un numero.
        assertEquals(12, cost("rinnegan", ctx("uchiha", true, true, false, 0)));
        assertEquals(18, cost("rinnegan", ctx("uchiha", true, false, false, 0)));
    }

    @Test
    void elUzumakiTieneTratoDeFavorConElRinnegan() {
        assertEquals(12, cost("rinnegan", ctx("uzumaki", true, true, false, 0)));
        assertEquals(18, cost("rinnegan", ctx("uzumaki", false, false, false, 0)));
        assertEquals(36, cost("rinnegan", ctx("nara", false, false, false, 0)));
    }

    // ------------------------------------------------------------------
    // Forma del modelo
    // ------------------------------------------------------------------

    @Test
    void elIntervaloEsElDelDatapack() {
        // 40 ticks = 2 segundos; ningun ojo lo redefine
        assertEquals(40, eye("sharingan").passiveCost().orElseThrow().intervalTicks());
        assertEquals(EyePassiveCost.DEFAULT_INTERVAL_TICKS,
                eye("byakugan").passiveCost().orElseThrow().intervalTicks());
    }

    @Test
    void unOjoSinCosteDeclaradoNoCobra() {
        // el Rinne Sharingan no aparece en basic_costs del datapack: no cuesta chakra
        assertTrue(EyeCostResolver.costFor(eye("rinne_sharingan"),
                ctx("otsutsuki", true, true, false, 0)).isEmpty());
    }

    @Test
    void laPrimeraReglaQueEncajaMandaYNoLaMasEspecifica() {
        // se evalua en orden, como el mcfunction. Un Uchiha despierto con eyematch cae en la PRIMERA regla
        // del Mangekyo (14) y nunca llega a la general de 28.
        assertEquals(14, cost("mangekyo_itachi", ctx("uchiha", true, true, false, 0)));
    }
}
