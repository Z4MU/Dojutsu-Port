package com.zamu.dojutsu.jutsu.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zamu.dojutsu.domain.jutsu.AffinityRequirement;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests del ancla visual de los jutsus (F11/DJT-239).
 *
 * <p>Lo que se protege es que una tecnica <b>sin ningun dato de arte</b> ya se vea: la particula se deduce de
 * su afinidad y la dispersion, del radio real del efecto. Ese es todo el valor del ticket mientras los modelos
 * no existan.
 */
class EffectVfxTest {

    private static JsonObject params(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static JutsuDefinition jutsu(String nature) {
        Optional<AffinityRequirement> affinity = nature == null
                ? Optional.empty()
                : Optional.of(new AffinityRequirement(nature, 100));
        return new JutsuDefinition("x", 10, 20, affinity, Optional.empty(), 0, "area", "area");
    }

    // ------------------------------------------------------------------
    // Sin datos de arte, la tecnica ya se ve
    // ------------------------------------------------------------------

    @Test
    void unaTecnicaSinDatosDeArteUsaLosPorDefecto() {
        EffectVfx vfx = EffectVfx.parse(params("{\"radius\": 5}"));

        assertEquals(EffectVfx.DEFAULT.count(), vfx.count());
        assertTrue(vfx.particle().isEmpty(), "no declara particula: se deducira de la afinidad");
        assertTrue(vfx.model().isEmpty(), "no hay modelo hasta que exista el arte");
    }

    @Test
    void laParticulaSeDeduceDeLaAfinidad() {
        EffectVfx vfx = EffectVfx.parse(params("{}"));

        assertEquals("minecraft:flame", vfx.particleIdFor(jutsu("fire")));
        assertEquals("minecraft:splash", vfx.particleIdFor(jutsu("water")));
        assertEquals("minecraft:electric_spark", vfx.particleIdFor(jutsu("lightning")));
        assertEquals("minecraft:cloud", vfx.particleIdFor(jutsu("wind")));
    }

    @Test
    void unaTecnicaSinAfinidadUsaLaParticulaNeutra() {
        // kenjutsu, medicas y universales: 19 de las 128 no declaran afinidad
        assertEquals(EffectVfx.NEUTRAL, EffectVfx.parse(params("{}")).particleIdFor(jutsu(null)));
    }

    @Test
    void unaNaturalezaDesconocidaCaeEnLaNeutraYNoRompe() {
        // un datapack de terceros puede traer naturalezas que este mapa no conoce
        assertEquals(EffectVfx.NEUTRAL, EffectVfx.parse(params("{}")).particleIdFor(jutsu("plasma")));
    }

    // ------------------------------------------------------------------
    // Lo declarado manda
    // ------------------------------------------------------------------

    @Test
    void laParticulaDeclaradaGanaALaDeducida() {
        EffectVfx vfx = EffectVfx.parse(params("{\"vfxParticle\": \"minecraft:soul_fire_flame\"}"));

        assertEquals("minecraft:soul_fire_flame", vfx.particle().orElseThrow());
        assertEquals("minecraft:soul_fire_flame", vfx.particleIdFor(jutsu("fire")),
                "lo declarado gana a la afinidad");
    }

    // NOTA: que un id inexistente caiga en la particula deducida se resuelve contra el registro de
    // Minecraft (EffectVfx.particleFor), y este repo no arranca Minecraft en tests. Queda cubierto por el
    // checklist en partida, igual que la resolucion de sonidos de EffectSfx.

    @Test
    void seLeenTodosLosCamposDeclarados() {
        EffectVfx vfx = EffectVfx.parse(params("""
                {"vfxCount": 60, "vfxSpread": 3.5, "vfxSpeed": 0.2, "vfxModel": "dojutsu:jutsu",
                 "vfxModelScale": 2.5, "vfxModelOffsetY": 1.8, "vfxLifetimeTicks": 100}"""));

        assertEquals(60, vfx.count());
        assertEquals(3.5D, vfx.spread());
        assertEquals(0.2D, vfx.speed());
        assertEquals("dojutsu:jutsu", vfx.model().orElseThrow());
        assertEquals(2.5F, vfx.modelScale());
        assertEquals(1.8D, vfx.modelOffsetY());
        assertEquals(100, vfx.lifetimeTicks());
    }

    // ------------------------------------------------------------------
    // La dispersion sigue al tamano del efecto
    // ------------------------------------------------------------------

    @Test
    void sinDispersionDeclaradaSeUsaElRadioDelEfecto() {
        // un jutsu de area de 10 bloques no puede verse igual que uno de 2
        EffectVfx vfx = EffectVfx.parse(params("{}"));

        assertEquals(10.0D, vfx.spreadOr(10.0D));
        assertEquals(2.0D, vfx.spreadOr(2.0D));
    }

    @Test
    void laDispersionDeclaradaGanaAlRadio() {
        EffectVfx vfx = EffectVfx.parse(params("{\"vfxSpread\": 4}"));

        assertEquals(4.0D, vfx.spreadOr(10.0D));
    }

    @Test
    void unRadioCeroNoDejaElVisualInvisible() {
        // buffs y curaciones se anclan con radio 1, pero si algo pasara 0 hay que seguir viendo algo
        assertEquals(0.5D, EffectVfx.parse(params("{}")).spreadOr(0.0D));
    }
}
