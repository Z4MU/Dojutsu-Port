package com.zamu.dojutsu.dojutsu.ability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Tests de la resolución de casteo de habilidades oculares (DJT-118): requiere ojo activo + habilidad
 * seleccionada + registrada. La ejecución del efecto (Amaterasu ancla + fuego) es glue con Minecraft y no se
 * testea aquí; sí los costes de la habilidad piloto.
 */
class EyeAbilityServiceTest {

    private static final EyeAbility DUMMY = new EyeAbility() {
        @Override public int chakraCost() { return 40; }
        @Override public int strainCost() { return 10; }
        @Override public void execute(EyeAbilityContext context) { /* no-op en test */ }
    };

    private static final Function<String, Optional<EyeAbility>> LOOKUP =
            id -> Optional.ofNullable(Map.of("amaterasu", DUMMY).get(id));

    private static PlayerNinjaData data(EyeState eye) {
        PlayerNinjaData d = new PlayerNinjaData();
        d.setEye(eye);
        return d;
    }

    @Test
    void sinOjoActivoNoResuelve() {
        // hay selección pero no ojo activo (no debería pasar, pero se blinda)
        EyeState eye = EyeState.DEFAULT.withSelectedAbility("amaterasu");
        assertTrue(EyeAbilityService.resolveCast(data(eye), LOOKUP).isEmpty());
    }

    @Test
    void sinSeleccionNoResuelve() {
        EyeState eye = EyeState.DEFAULT.withActiveEye("mangekyo_itachi");
        assertTrue(EyeAbilityService.resolveCast(data(eye), LOOKUP).isEmpty());
    }

    @Test
    void habilidadNoRegistradaNoResuelve() {
        // seleccionada pero sin clase (data-only del menú): no se puede lanzar
        EyeState eye = EyeState.DEFAULT.withActiveEye("mangekyo_itachi").withSelectedAbility("tsukuyomi");
        assertTrue(EyeAbilityService.resolveCast(data(eye), LOOKUP).isEmpty());
    }

    @Test
    void ojoActivoMasSeleccionRegistradaResuelve() {
        EyeState eye = EyeState.DEFAULT.withActiveEye("mangekyo_itachi").withSelectedAbility("amaterasu");
        assertSame(DUMMY, EyeAbilityService.resolveCast(data(eye), LOOKUP).orElseThrow());
    }

    @Test
    void amaterasuTieneLosCostesDelDatapack() {
        AmaterasuAbility amaterasu = new AmaterasuAbility();
        assertEquals(40, amaterasu.chakraCost());
        assertEquals(10, amaterasu.strainCost());
    }

    @Test
    void lasHabilidadesDeDJT119TienenLosCostesDelDatapack() {
        assertEquals(40, new VacuumPalmAbility().chakraCost());   // vacuum_palm/cast: 40..
        assertEquals(600, new ShinraTenseiAbility().chakraCost()); // shinra_tensei/cast: 600..
        assertEquals(360, new BanshoTeninAbility().chakraCost());  // bansho_tenin/cast: 360..
    }

    @Test
    void lasHabilidadesDelKetsuryuganTienenLosCostesYStrainDelDatapack() {
        assertEquals(120, new BloodEruptionAbility().chakraCost());
        assertEquals(8, new BloodEruptionAbility().strainCost());
        assertEquals(80, new IronBloodFistAbility().chakraCost());
        assertEquals(4, new IronBloodFistAbility().strainCost());
        assertEquals(100, new RisingBloodSpearAbility().chakraCost());
        assertEquals(14, new RisingBloodSpearAbility().strainCost());
    }

    @Test
    void elLote5TieneLosCostesYStrainDelDatapack() {
        assertEquals(450, new AsuraPathAbility().chakraCost());   // asura/cast: 450 (Rinnegan, strain 0)
        assertEquals(0, new AsuraPathAbility().strainCost());
        assertEquals(150, new PretaPathAbility().chakraCost());   // preta/cast: 150
        assertEquals(0, new PretaPathAbility().strainCost());
        assertEquals(300, new TruthSeekingOrbsAbility().chakraCost()); // truth_seeking_orb: consume 300
        assertEquals(0, new TruthSeekingOrbsAbility().strainCost());
    }

    @Test
    void elLote6TieneLosCostesYStrainDelDatapack() {
        // El Sharingan base no cobra chakra ni fatiga: gatea por cooldown (d.sharstun_cd 200).
        assertEquals(0, new GenjutsuStunAbility().chakraCost());
        assertEquals(0, new GenjutsuStunAbility().strainCost());
        assertEquals(200, new GenjutsuStunAbility().cooldownTicks());
        assertEquals(0, new CopyJutsuAbility().chakraCost());
        assertEquals(0, new CopyJutsuAbility().strainCost());
        assertEquals(400, new KinrinTenseiBakuAbility().chakraCost()); // kinrin/cast: matches 400.. y resta 400
        assertEquals(0, new KinrinTenseiBakuAbility().strainCost());   // Tenseigan: sin eye_strain
    }

    @Test
    void porDefectoUnaHabilidadNoTieneCooldown() {
        // el grueso del catalogo se autolimita por su coste de chakra
        assertEquals(0, DUMMY.cooldownTicks());
        assertEquals(0, new AmaterasuAbility().cooldownTicks());
    }

    @Test
    void kamuiAutogestionaSuCosteAsiQueElContratoEsCeroCero() {
        // marca=gratis, teletransporte=20/+2 (dentro de execute): el pipeline no cobra nada (roster lote 2).
        assertEquals(0, new KamuiAbility().chakraCost());
        assertEquals(0, new KamuiAbility().strainCost());
        assertEquals(0, new KamuiAbility().cooldownTicks());
        // Kamui (Others) tambien autogestiona (20/+3 dentro de execute, cooldown compartido).
        assertEquals(0, new KamuiOthersAbility().chakraCost());
        assertEquals(0, new KamuiOthersAbility().strainCost());
    }

    @Test
    void tengaiShinseiTieneLosCostesDelDatapack() {
        // madara/cost: -200 chakra, +13 eye_strain, mangekyo_time 200 (roster lote 3)
        assertEquals(200, new TengaiShinseiAbility().chakraCost());
        assertEquals(13, new TengaiShinseiAbility().strainCost());
        assertEquals(200, new TengaiShinseiAbility().cooldownTicks());
    }

    @Test
    void beamsOfLightTieneLosCostesDelDatapack() {
        // shisui/beams_of_light: -80 chakra, +20 eye_strain, mangekyo_time 800 (la mas cara del roster)
        assertEquals(80, new BeamsOfLightAbility().chakraCost());
        assertEquals(20, new BeamsOfLightAbility().strainCost());
        assertEquals(800, new BeamsOfLightAbility().cooldownTicks());
    }

    @Test
    void flameControlTieneLosCostesDelDatapack() {
        // sasuke/extinguish/cost: -20 chakra, +10 eye_strain, mangekyo_time 100 (roster lote 7, cierra los 8)
        assertEquals(20, new FlameControlAbility().chakraCost());
        assertEquals(10, new FlameControlAbility().strainCost());
        assertEquals(100, new FlameControlAbility().cooldownTicks());
    }

    @Test
    void lasTecnicasDeShinTienenLosCostesDelDatapack() {
        // shin/blades/cast: -20 chakra, +6 strain, mangekyo_time 60 (el cd mas corto del roster)
        assertEquals(20, new ChakraBladesAbility().chakraCost());
        assertEquals(6, new ChakraBladesAbility().strainCost());
        assertEquals(60, new ChakraBladesAbility().cooldownTicks());
        // shin/teleport/cast: -100 chakra, +8 strain, shin_timer 200
        assertEquals(100, new PhaseShiftAbility().chakraCost());
        assertEquals(8, new PhaseShiftAbility().strainCost());
        assertEquals(200, new PhaseShiftAbility().cooldownTicks());
    }

    @Test
    void kotoamatsukamiTieneLosCostesDelDatapack() {
        // shisui/afterglow: -50 chakra, +15 eye_strain, mangekyo_time 500 (el cd mas largo del roster; lote 5)
        assertEquals(50, new KotoamatsukamiAbility().chakraCost());
        assertEquals(15, new KotoamatsukamiAbility().strainCost());
        assertEquals(500, new KotoamatsukamiAbility().cooldownTicks());
    }

    @Test
    void hinokagutsuchiTieneLosCostesDelDatapack() {
        // indra/hinokagutsuchi: -20 chakra, +5 eye_strain, mangekyo_time 200 (roster lote 4)
        assertEquals(20, new HinokagutsuchiAbility().chakraCost());
        assertEquals(5, new HinokagutsuchiAbility().strainCost());
        assertEquals(200, new HinokagutsuchiAbility().cooldownTicks());
    }

    @Test
    void porDefectoUnaHabilidadSiempreEsAplicable() {
        // canApply default = true: AoE/auto-buff/proyectil/punto de mira siempre aplican (no refund).
        // El "Nothing nearby" (no_cast_nby) solo lo overridean blood_eruption/exploding_human/genjutsu_stun,
        // cuya busqueda de objetivo necesita un ServerLevel: se valida en runtime, no aqui.
        assertTrue(DUMMY.canApply(null));
        assertTrue(new IronBloodFistAbility().canApply(null)); // auto-buff: nada que apuntar
    }

    @Test
    void elCooldownBloqueaHastaAlcanzarElTickDeDisponibilidad() {
        assertTrue(EyeAbilityService.isOnCooldown(200L, 199L));   // aun enfriando
        assertFalse(EyeAbilityService.isOnCooldown(200L, 200L));  // justo disponible (inclusivo)
        assertFalse(EyeAbilityService.isOnCooldown(200L, 201L));
        assertFalse(EyeAbilityService.isOnCooldown(Long.MIN_VALUE, 0L)); // nunca lanzada: disponible
    }

    @Test
    void elLote4TieneLosCostesYStrainDelDatapack() {
        assertEquals(200, new ExplodingHumanAbility().chakraCost());
        assertEquals(20, new ExplodingHumanAbility().strainCost());          // Ketsuryugan: sí añade fatiga
        assertEquals(80, new HundredTwentyEightPalmsAbility().chakraCost());
        assertEquals(0, new HundredTwentyEightPalmsAbility().strainCost());  // Byakugan: no
        assertEquals(600, new ChibakuTenseiAbility().chakraCost());
        assertEquals(0, new ChibakuTenseiAbility().strainCost());            // Rinnegan: no
    }

    @Test
    void elLote3TieneLosCostesYStrainDelDatapack() {
        assertEquals(100, new TsukuyomiAbility().chakraCost());   // tsukuyomi/cost: -100 chakra
        assertEquals(30, new TsukuyomiAbility().strainCost());     // +30 eye_strain (el más caro; exento si eterno)
        assertEquals(600, new AmenotejikaraAbility().chakraCost()); // amenotejikara/cast: 600.. (Rinnegan, strain 0)
        assertEquals(0, new AmenotejikaraAbility().strainCost());
        assertEquals(60, new SixtyFourPalmsAbility().chakraCost()); // 64_palms/cast: 60.. (Byakugan, strain 0)
        assertEquals(0, new SixtyFourPalmsAbility().strainCost());
    }

    @Test
    void elRegistroDevuelveLoRegistrado() {
        // se prueba el registro directamente (EyeAbilityService.register toca la red, no disponible sin MC).
        EyeAbilityRegistry.register("amaterasu", new AmaterasuAbility());
        assertTrue(EyeAbilityRegistry.get("amaterasu").isPresent());
        assertFalse(EyeAbilityRegistry.get("no_existe").isPresent());
    }
}
