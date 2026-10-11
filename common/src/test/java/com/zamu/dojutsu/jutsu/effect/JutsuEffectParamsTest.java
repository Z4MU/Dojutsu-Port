package com.zamu.dojutsu.jutsu.effect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Tests del parseo de parametros de las familias (F4/DJT-076), contra los JSON <b>reales</b>
 * del classpath — los valores son los auditados en los {@code .mcfunction} del datapack.
 */
class JutsuEffectParamsTest {

    private static final Gson GSON = new Gson();

    /** Carga los effectParams reales de {@code data/dojutsu/jutsu/<subruta>.json}. */
    private static JsonObject effectParams(String subpath) {
        String path = "/data/dojutsu/jutsu/" + subpath + ".json";
        try (InputStream in = JutsuEffectParamsTest.class.getResourceAsStream(path)) {
            JsonObject json = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            return json.has("effectParams") ? json.getAsJsonObject("effectParams") : new JsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("no se pudo cargar " + path, e);
        }
    }

    @Test
    void proyectilDelGranFireballCoincideConElDatapack() {
        ProjectileEffect.Params p = ProjectileEffect.Params.parse(effectParams("fire/great_fireball"));
        assertEquals(1.0, p.speed());
        assertEquals(100, p.lifetimeTicks());
        assertEquals(3.0, p.radius());
        assertEquals(100, p.fireTicks()); // Fire set value 100 (5 s)
        assertTrue(p.igniteGround());
        assertEquals("minecraft:fire_charge", p.displayItem());
    }

    @Test
    void proyectilConDefaults() {
        ProjectileEffect.Params p = ProjectileEffect.Params.parse(new JsonObject());
        assertEquals(1.0, p.speed());
        assertEquals(0.0, p.gravity());
        assertEquals(0.0, p.radius()); // sin aura por defecto
        assertFalse(p.igniteGround());
        assertEquals(0.0F, p.damage());
        assertEquals("minecraft:snowball", p.displayItem());
        assertEquals(0.0F, p.hitboxSize()); // la caja de 0.25 de siempre
        assertEquals(com.zamu.dojutsu.entity.ProjectileTrail.NONE, p.trail());
    }

    @Test
    void laBalaDeFuegoPrendeElSueloYLlevaEstela() {
        // F11/DJT-241. fire_bullet/run.mcfunction hacia "setblock ~ ~0 ~ fire" en cada tick de vuelo; el port no
        // lo tenia activado y en partida el suelo no ardia.
        ProjectileEffect.Params p = ProjectileEffect.Params.parse(effectParams("fire/fire_bullet"));
        assertTrue(p.igniteGround());
        assertEquals(3.0, p.radius());     // distance=0..3
        assertEquals(100, p.fireTicks());  // Fire set value 100
        assertEquals(0.8F, p.hitboxSize(), 1.0e-6F);
        assertEquals(com.zamu.dojutsu.entity.ProjectileTrail.FIRE, p.trail());
    }

    @Test
    void areaDeVacuumWaveCoincideConElDatapack() {
        AreaEffect.Params p = AreaEffect.Params.parse(effectParams("wind/vacuum_wave"));
        assertEquals(10.0, p.radius());   // distance=0..10
        assertEquals(8.0, p.launchY());   // tp @s ~ ~8 ~
        assertEquals(1, p.effects().size());
        assertEquals("minecraft:slow_falling", p.effects().get(0).id());
        assertEquals(6, p.effects().get(0).seconds());
        assertEquals(8, p.effects().get(0).amplifier());
    }

    @Test
    void buffDeFireproofCoincideConElDatapack() {
        BuffEffect.Params p = BuffEffect.Params.parse(effectParams("fire/fireproof"));
        assertEquals(1, p.effects().size());
        assertEquals("minecraft:fire_resistance", p.effects().get(0).id()); // effect give 1 0 true
        assertEquals(1, p.effects().get(0).seconds());
        assertEquals(0, p.effects().get(0).amplifier());
    }

    @Test
    void teleportDeSubstitutionCoincideConElDatapack() {
        TeleportEffect.Params p = TeleportEffect.Params.parse(effectParams("universal/substitution"));
        assertEquals(5.0, p.minRange());  // spreadplayers ~ ~ 5 10
        assertEquals(10.0, p.maxRange());
        assertEquals(6, p.invisibilitySeconds()); // invisibility 6 0 true
    }

    @Test
    void estructuraDeEarthWallCoincideConElDatapack() {
        StructureEffect.Params p = StructureEffect.Params.parse(effectParams("earth/earth_wall"));
        assertEquals("minecraft:dirt", p.block());
        assertEquals(3, p.width());   // ^-1..^1
        assertEquals(4, p.height());  // ^1..^4
        assertEquals(2, p.forward()); // ^ ^ ^2
    }

    @Test
    void curacionDeHealingCoincideConElDatapack() {
        HealEffect.Params p = HealEffect.Params.parse(effectParams("universal/healing"));
        assertEquals(3.0, p.radius()); // distance=0..3, sort=nearest
        assertEquals("minecraft:regeneration", p.effects().get(0).id()); // regeneration 2 20 true
        assertEquals(2, p.effects().get(0).seconds());
        assertEquals(20, p.effects().get(0).amplifier());
    }

    @Test
    void losParametrosVaciosNoRompenNingunaFamilia() {
        JsonObject empty = new JsonObject();
        AreaEffect.Params.parse(empty);
        BuffEffect.Params.parse(empty);
        TeleportEffect.Params.parse(empty);
        StructureEffect.Params.parse(empty);
        HealEffect.Params.parse(empty);
        // Radio 0 / listas vacias: casos borde de la ficha — parsear nunca lanza.
        assertTrue(BuffEffect.Params.parse(empty).effects().isEmpty());
        assertEquals(0.0, ProjectileEffect.Params.parse(empty).radius());
    }
}
