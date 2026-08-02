package com.zamu.dojutsu.domain.jutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests del contrato de tecnica y la maquina de precondiciones (DJT-013). */
class JutsuTest {

    /** Caster fake configurable (generico: no es un Player). */
    private static final class FakeCaster implements CastContext {
        int chakra;
        boolean onCooldown;
        boolean hasWind = true; // posee la naturaleza por defecto (la puerta de F4/DJT-074)
        int windAffinity;
        int kenjutsu;
        // Naturalezas componentes de KG y posesion de KG (F4/DJT-091). Poseidas por defecto para no
        // afectar a los tests de una sola afinidad (que solo tocan "wind").
        boolean hasEarth = true;
        boolean hasWater = true;
        int earthAffinity;
        int waterAffinity;
        boolean hasWood;

        @Override
        public int availableChakra() {
            return chakra;
        }

        @Override
        public boolean isOnCooldown(String jutsuId) {
            return onCooldown;
        }

        @Override
        public boolean hasNature(String natureId) {
            return switch (natureId) {
                case "wind" -> hasWind;
                case "earth" -> hasEarth;
                case "water" -> hasWater;
                default -> false;
            };
        }

        @Override
        public int affinity(String natureId) {
            return switch (natureId) {
                case "wind" -> windAffinity;
                case "earth" -> earthAffinity;
                case "water" -> waterAffinity;
                default -> 0;
            };
        }

        @Override
        public int skillLevel(String skillId) {
            return "kenjutsu".equals(skillId) ? kenjutsu : 0;
        }

        @Override
        public boolean hasKekkeiGenkai(String kekkeiGenkaiId) {
            return "wood".equals(kekkeiGenkaiId) && hasWood;
        }
    }

    /** Tecnica de prueba: envuelve una definicion y usa el chequeo estandar como cast. */
    private static Jutsu jutsu(JutsuDefinition def) {
        return new Jutsu() {
            @Override
            public JutsuDefinition definition() {
                return def;
            }

            @Override
            public CastResult cast(CastContext ctx) {
                return evaluatePreconditions(ctx);
            }
        };
    }

    /** Rasengan del ejemplo de [S06]: 100 chakra, cd 10s (200t), Afinidad Viento 150. */
    private static JutsuDefinition rasengan() {
        return new JutsuDefinition(
                "rasengan", 100, 200,
                Optional.of(new AffinityRequirement("wind", 150)),
                Optional.empty(),
                0, "wind", "rasengan_effect");
    }

    /** Tecnica de espada: sin afinidad, con requisito de nivel de kenjutsu (F4/DJT-083). */
    private static JutsuDefinition swordTechnique(int kenjutsuReq) {
        return new JutsuDefinition(
                "dancing_blade_risk", 0, 60,
                Optional.empty(),
                Optional.of(new SkillRequirement("kenjutsu", kenjutsuReq)),
                0, "kenjutsu", "area");
    }

    /**
     * Jutsu de Madera (KG) del datapack: gatea por poseer {@code wood} + las afinidades de sus dos
     * naturalezas componentes (earth y water), ambas obligatorias (F4/DJT-091).
     */
    private static JutsuDefinition woodJutsu() {
        return new JutsuDefinition(
                "great_forest", 120, 200,
                List.of(new AffinityRequirement("earth", 130), new AffinityRequirement("water", 130)),
                Optional.of(new KekkeiGenkaiRequirement("wood")),
                Optional.empty(), 0, "wood", "structure");
    }

    /** Caster con Madera + earth/water al nivel exacto: pasa todas las puertas. */
    private static FakeCaster woodReadyCaster() {
        FakeCaster caster = new FakeCaster();
        caster.chakra = 120;
        caster.hasWood = true;
        caster.earthAffinity = 130;
        caster.waterAffinity = 130;
        return caster;
    }

    @Test
    void jutsuKgExitosoConKgYAmbasAfinidades() {
        assertTrue(jutsu(woodJutsu()).cast(woodReadyCaster()).isSuccess());
    }

    @Test
    void jutsuKgFallaSinPoseerElKekkeiGenkai() {
        FakeCaster caster = woodReadyCaster();
        caster.hasWood = false;
        CastResult r = jutsu(woodJutsu()).cast(caster);
        assertEquals(CastFailureReason.MISSING_KEKKEI_GENKAI, r.failureReason().orElseThrow());
    }

    @Test
    void jutsuKgFallaSiUnaAfinidadComponenteNoSeAlcanza() {
        FakeCaster caster = woodReadyCaster();
        caster.waterAffinity = 129; // earth ok, water justo por debajo
        CastResult r = jutsu(woodJutsu()).cast(caster);
        assertEquals(CastFailureReason.INSUFFICIENT_AFFINITY, r.failureReason().orElseThrow());
    }

    @Test
    void jutsuKgFallaSiNoSePoseeUnaNaturalezaComponente() {
        FakeCaster caster = woodReadyCaster();
        caster.hasWater = false; // tiene el nivel pero no posee la naturaleza
        CastResult r = jutsu(woodJutsu()).cast(caster);
        assertEquals(CastFailureReason.MISSING_NATURE, r.failureReason().orElseThrow());
    }

    @Test
    void laPuertaDeKgVaAntesQueLaAfinidad() {
        FakeCaster caster = woodReadyCaster();
        caster.hasWood = false;     // falla KG
        caster.waterAffinity = 0;   // y tambien afinidad
        CastResult r = jutsu(woodJutsu()).cast(caster);
        assertEquals(CastFailureReason.MISSING_KEKKEI_GENKAI, r.failureReason().orElseThrow());
    }

    @Test
    void afinidadRequirementDevuelveLaPrimeraDeLaLista() {
        JutsuDefinition def = woodJutsu();
        assertEquals(2, def.affinityRequirements().size());
        assertEquals("earth", def.affinityRequirement().orElseThrow().natureId());
    }

    @Test
    void laDefinicionExponeCosteCooldownYAfinidad() {
        JutsuDefinition def = rasengan();
        assertEquals("rasengan", def.id());
        assertEquals(100, def.chakraCost());
        assertEquals(200, def.cooldownTicks());
        assertEquals(150, def.affinityRequirement().orElseThrow().amount());
        assertEquals("wind", def.affinityRequirement().orElseThrow().natureId());
        assertEquals("rasengan_effect", def.effectId());
    }

    @Test
    void castExitosoCuandoSeCumpleTodo() {
        FakeCaster caster = new FakeCaster();
        caster.chakra = 100;
        caster.onCooldown = false;
        caster.windAffinity = 150;
        CastResult r = jutsu(rasengan()).cast(caster);
        assertTrue(r.isSuccess());
        assertTrue(r.failureReason().isEmpty());
    }

    @Test
    void falloPorChakraTienePrioridad() {
        FakeCaster caster = new FakeCaster();
        caster.chakra = 99;        // insuficiente
        caster.onCooldown = true;  // tambien en cooldown
        caster.windAffinity = 0;   // y sin afinidad
        CastResult r = jutsu(rasengan()).cast(caster);
        assertFalse(r.isSuccess());
        assertEquals(CastFailureReason.INSUFFICIENT_CHAKRA, r.failureReason().orElseThrow());
    }

    @Test
    void falloPorCooldownCuandoElChakraAlcanza() {
        FakeCaster caster = new FakeCaster();
        caster.chakra = 100;
        caster.onCooldown = true;
        caster.windAffinity = 0; // aun sin afinidad, cooldown va antes
        CastResult r = jutsu(rasengan()).cast(caster);
        assertEquals(CastFailureReason.ON_COOLDOWN, r.failureReason().orElseThrow());
    }

    @Test
    void falloPorAfinidadCuandoChakraYCooldownOk() {
        FakeCaster caster = new FakeCaster();
        caster.chakra = 100;
        caster.onCooldown = false;
        caster.windAffinity = 149; // justo por debajo
        CastResult r = jutsu(rasengan()).cast(caster);
        assertEquals(CastFailureReason.INSUFFICIENT_AFFINITY, r.failureReason().orElseThrow());
    }

    @Test
    void tecnicaUniversalIgnoraLaAfinidad() {
        JutsuDefinition universal = new JutsuDefinition(
                "body_slam", 10, 0, Optional.empty(), Optional.empty(), 0, "taijutsu", "body_slam_effect");
        FakeCaster caster = new FakeCaster();
        caster.chakra = 10;
        caster.windAffinity = 0; // sin afinidad: da igual
        assertTrue(jutsu(universal).cast(caster).isSuccess());
    }

    @Test
    void costeCeroSiempreTieneChakra() {
        JutsuDefinition free = new JutsuDefinition(
                "free_jutsu", 0, 0, Optional.empty(), Optional.empty(), 0, "misc", "free_effect");
        FakeCaster caster = new FakeCaster(); // chakra 0
        assertTrue(jutsu(free).cast(caster).isSuccess());
    }

    @Test
    void casterSinAfinidadFallaSiSeRequiere() {
        FakeCaster caster = new FakeCaster();
        caster.chakra = 100;
        caster.windAffinity = 0; // perfil de afinidad vacio -> 0
        assertEquals(CastFailureReason.INSUFFICIENT_AFFINITY,
                jutsu(rasengan()).cast(caster).failureReason().orElseThrow());
    }

    @Test
    void sinPoseerLaNaturalezaFallaAunqueConserveNivelDeAfinidad() {
        // check_affinity (F4/DJT-074): la posesion es una puerta previa al nivel — un caster con
        // la naturaleza revocada conserva su afinidad (DJT-051) y aun asi NO puede castear.
        FakeCaster caster = new FakeCaster();
        caster.chakra = 100;
        caster.hasWind = false;
        caster.windAffinity = 150; // nivel suficiente, naturaleza ausente
        assertEquals(CastFailureReason.MISSING_NATURE,
                jutsu(rasengan()).cast(caster).failureReason().orElseThrow());
    }

    @Test
    void definicionRechazaValoresInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new JutsuDefinition(
                " ", 100, 200, Optional.empty(), Optional.empty(), 0, "wind", "fx"));
        assertThrows(IllegalArgumentException.class, () -> new JutsuDefinition(
                "id", -1, 200, Optional.empty(), Optional.empty(), 0, "wind", "fx"));
        assertThrows(IllegalArgumentException.class, () -> new JutsuDefinition(
                "id", 100, 200, Optional.empty(), Optional.empty(), 0, "wind", ""));
        assertThrows(IllegalArgumentException.class, () -> new AffinityRequirement("", 10));
        assertThrows(IllegalArgumentException.class, () -> new AffinityRequirement("wind", -1));
        assertThrows(IllegalArgumentException.class, () -> new SkillRequirement(" ", 10));
        assertThrows(IllegalArgumentException.class, () -> new SkillRequirement("kenjutsu", -1));
    }

    // --- Requisito de nivel de habilidad (F4/DJT-083) ---

    @Test
    void laTecnicaDeEspadaFallaSinNivelDeKenjutsu() {
        FakeCaster caster = new FakeCaster();
        caster.kenjutsu = 74; // justo por debajo de 75
        assertEquals(CastFailureReason.INSUFFICIENT_SKILL,
                jutsu(swordTechnique(75)).cast(caster).failureReason().orElseThrow());
    }

    @Test
    void laTecnicaDeEspadaPasaConNivelDeKenjutsuSuficiente() {
        FakeCaster caster = new FakeCaster();
        caster.kenjutsu = 75;
        assertTrue(jutsu(swordTechnique(75)).cast(caster).isSuccess());
    }

    @Test
    void elRequisitoDeHabilidadSeChequeaDespuesDeChakraYCooldown() {
        // Orden: chakra -> cd -> (afinidad) -> habilidad. Con chakra insuficiente gana el chakra.
        FakeCaster caster = new FakeCaster();
        caster.kenjutsu = 0; // no cumple
        // La tecnica de espada tiene coste 0, asi que el chakra siempre pasa; el fallo es de habilidad.
        assertEquals(CastFailureReason.INSUFFICIENT_SKILL,
                jutsu(swordTechnique(75)).cast(caster).failureReason().orElseThrow());
    }
}
