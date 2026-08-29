package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.eye.EyeEvolution;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.eye.EyeRequirement;
import com.zamu.dojutsu.dojutsu.EvolutionService.EvolveResult;
import com.zamu.dojutsu.dojutsu.EvolutionService.Transition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Tests de la lógica pura de evolución de ojos (DJT-115): acumulación de uso, transición por grafo al alcanzar
 * el umbral, rutas divergentes por clan (otsutsuki_uchiha vs. implante), requisito de flag (Rinnegan por ADN de
 * Hashirama), precedencia por orden de la lista, ojos terminales y sin ojo. Sin Minecraft.
 */
class EvolutionServiceTest {

    private static final Predicate<String> NO_FLAGS = flag -> false;
    private static final Predicate<String> HASHIRAMA_DNA = "hashirama_dna"::equals;

    /** Azar con semilla fija: los tests no-aleatorios no dependen de el (aristas de destino unico). */
    private static final java.util.Random RNG = new java.util.Random(0);

    /** Sharingan con las dos aristas reales: -> Mangekyo (clan+uso) y -> Rinnegan (clan+ADN). */
    private static EyeDefinition sharingan() {
        return new EyeDefinition("sharingan", EyeFamily.SHARINGAN, true, 20, 20, Optional.of(78000),
                Optional.of(78000), List.of(),
                List.of(
                        new EyeEvolution("mangekyo_itachi",
                                List.of(EyeRequirement.clan("otsutsuki_uchiha"), EyeRequirement.useAtLeast(360000))),
                        new EyeEvolution("rinnegan",
                                List.of(EyeRequirement.clan("uchiha"), EyeRequirement.flag("hashirama_dna")))),
                List.of(), false, 0, Optional.empty(), Optional.empty());
    }

    private static EyeDefinition terminal(String id, EyeFamily family) {
        return new EyeDefinition(id, family, true, 0, 0, Optional.empty(), Optional.empty(), List.of(), List.of(),
                List.of(), false, 0, Optional.empty(), Optional.empty());
    }

    private static Function<String, Optional<EyeDefinition>> lookup(EyeDefinition... eyes) {
        Map<String, EyeDefinition> byId = new HashMap<>();
        for (EyeDefinition eye : eyes) {
            byId.put(eye.id(), eye);
        }
        return id -> Optional.ofNullable(byId.get(id));
    }

    private static Function<String, Optional<EyeDefinition>> world() {
        return lookup(sharingan(), terminal("mangekyo_itachi", EyeFamily.MANGEKYO),
                terminal("rinnegan", EyeFamily.RINNEGAN));
    }

    @Test
    void sinOjoActivoNoHaceNada() {
        EvolveResult r = EvolutionService.evaluate(EyeState.DEFAULT, Optional.of("uchiha"), NO_FLAGS, world(), RNG);
        assertSame(EyeState.DEFAULT, r.state());
        assertTrue(r.transition().isEmpty());
    }

    @Test
    void acumulaUsoBajoElUmbral() {
        EyeState start = EyeState.DEFAULT.withActiveEye("sharingan").withUseLevel(EyeFamily.SHARINGAN, 1000);
        EvolveResult r = EvolutionService.evaluate(start, Optional.of("uchiha"), NO_FLAGS, world(), RNG);
        assertTrue(r.transition().isEmpty());
        assertEquals(1001, r.state().useLevel(EyeFamily.SHARINGAN));
    }

    @Test
    void evolucionaAMangekyoAlAlcanzarElUmbral() {
        EyeState start = EyeState.DEFAULT.withActiveEye("sharingan").withUseLevel(EyeFamily.SHARINGAN, 359999);
        EvolveResult r = EvolutionService.evaluate(start, Optional.of("uchiha"), NO_FLAGS, world(), RNG);
        assertEquals(Optional.of(new Transition("sharingan", "mangekyo_itachi")), r.transition());
        assertEquals("mangekyo_itachi", r.state().activeEye().orElseThrow());
        assertTrue(r.state().isAwakened(EyeFamily.MANGEKYO));
    }

    @Test
    void elImplanteNoLlegaAMangekyo() {
        // Ruta divergente (R6): un clan que no es Uchiha/Otsutsuki nunca cumple el requisito de clan del Mangekyo.
        EyeState start = EyeState.DEFAULT.withActiveEye("sharingan").withUseLevel(EyeFamily.SHARINGAN, 500000);
        EvolveResult r = EvolutionService.evaluate(start, Optional.of("hatake"), NO_FLAGS, world(), RNG);
        assertTrue(r.transition().isEmpty());
        assertEquals(500001, r.state().useLevel(EyeFamily.SHARINGAN)); // sigue acumulando, pero no evoluciona
    }

    @Test
    void evolucionaARinneganPorAdnDeHashiramaSinAlcanzarElUsoDeMangekyo() {
        EyeState start = EyeState.DEFAULT.withActiveEye("sharingan").withUseLevel(EyeFamily.SHARINGAN, 1000);
        EvolveResult r = EvolutionService.evaluate(start, Optional.of("uchiha"), HASHIRAMA_DNA, world(), RNG);
        assertEquals(Optional.of(new Transition("sharingan", "rinnegan")), r.transition());
        assertEquals("rinnegan", r.state().activeEye().orElseThrow());
    }

    @Test
    void laPrecedenciaEsElOrdenDeLaLista() {
        // Con uso alto + ADN, ambas aristas se cumplen; gana la primera (Mangekyo).
        EyeState start = EyeState.DEFAULT.withActiveEye("sharingan").withUseLevel(EyeFamily.SHARINGAN, 359999);
        EvolveResult r = EvolutionService.evaluate(start, Optional.of("uchiha"), HASHIRAMA_DNA, world(), RNG);
        assertEquals("mangekyo_itachi", r.state().activeEye().orElseThrow());
    }

    @Test
    void ojoTerminalNoAcumulaNiEvoluciona() {
        EyeState start = EyeState.DEFAULT.withActiveEye("rinnegan").withUseLevel(EyeFamily.RINNEGAN, 5);
        EvolveResult r = EvolutionService.evaluate(start, Optional.of("uchiha"), NO_FLAGS, world(), RNG);
        assertSame(start, r.state()); // sin aristas ni uso-gated: misma instancia
    }

    @Test
    void clanMatchesAceptaPredicateCompuesto() {
        assertTrue(EvolutionService.clanMatches("uchiha", "otsutsuki_uchiha"));
        assertTrue(EvolutionService.clanMatches("otsutsuki", "otsutsuki_uchiha"));
        assertFalse(EvolutionService.clanMatches("hyuga", "otsutsuki_uchiha"));
        assertTrue(EvolutionService.clanMatches("uchiha", "uchiha"));
    }

    @Test
    void elMangekyoEternalizableAcumulaUsoAunqueNoTengaAristas() {
        // fleco F5: el Mangekyo (terminal, sin evoluciones) debe acumular uso hacia el EMS (use_level en
        // eternalRequirements), para que el 864000 del ems_req sea alcanzable en nuestro modelo colapsado.
        EyeDefinition mangekyo = new EyeDefinition("mangekyo_itachi", EyeFamily.MANGEKYO, true, 20, 80,
                Optional.empty(), Optional.empty(), List.of("amaterasu"), List.of(),
                List.of(EyeRequirement.clan("otsutsuki_uchiha"), EyeRequirement.useAtLeast(864000)), true, 0, Optional.empty(), Optional.empty());
        assertTrue(EvolutionService.hasUseGatedEternal(mangekyo));

        EyeState start = EyeState.DEFAULT.withActiveEye("mangekyo_itachi").withUseLevel(EyeFamily.MANGEKYO, 100);
        EvolveResult r = EvolutionService.evaluate(start, Optional.of("uchiha"), NO_FLAGS,
                id -> "mangekyo_itachi".equals(id) ? Optional.of(mangekyo) : Optional.empty(), RNG);
        assertEquals(101, r.state().useLevel(EyeFamily.MANGEKYO)); // +1/tick, ahora sí acumula
    }

    @Test
    void laEvolucionConPoolEligeUnaVarianteAlAzarDeLasQueExisten() {
        // mangekyo_rng: el Sharingan despierta un Mangekyo de personaje aleatorio de entre los que existen.
        EyeDefinition sharinganRng = new EyeDefinition("sharingan", EyeFamily.SHARINGAN, true, 20, 20,
                Optional.of(78000), Optional.of(78000), List.of(),
                List.of(new EyeEvolution("mangekyo_itachi",
                        List.of(EyeRequirement.clan("otsutsuki_uchiha"), EyeRequirement.useAtLeast(360000)),
                        List.of("mangekyo_itachi", "mangekyo_obito", "mangekyo_sasuke"))),
                List.of(), false, 0, Optional.empty(), Optional.empty());
        Function<String, Optional<EyeDefinition>> world = lookup(sharinganRng,
                terminal("mangekyo_itachi", EyeFamily.MANGEKYO),
                terminal("mangekyo_obito", EyeFamily.MANGEKYO),
                terminal("mangekyo_sasuke", EyeFamily.MANGEKYO));

        EyeState start = EyeState.DEFAULT.withActiveEye("sharingan").withUseLevel(EyeFamily.SHARINGAN, 359999);
        var pool = java.util.Set.of("mangekyo_itachi", "mangekyo_obito", "mangekyo_sasuke");
        // con varias semillas debe salir un destino del pool (y no siempre el mismo)
        var elegidos = new java.util.HashSet<String>();
        for (long seed = 0; seed < 20; seed++) {
            EvolveResult r = EvolutionService.evaluate(
                    start, Optional.of("uchiha"), NO_FLAGS, world, new java.util.Random(seed));
            String to = r.state().activeEye().orElseThrow();
            assertTrue(pool.contains(to), "el destino debe ser una variante del pool: " + to);
            elegidos.add(to);
        }
        assertTrue(elegidos.size() > 1, "el pool debe elegir variantes distintas segun el azar");
    }

    @Test
    void unaVarianteInexistenteSeIgnoraYSoloSeEligeEntreLasQueCargan() {
        // si una variante del pool no existe en el registro, no debe elegirse (fiel: solo las cargadas)
        EyeDefinition sharinganRng = new EyeDefinition("sharingan", EyeFamily.SHARINGAN, true, 20, 20,
                Optional.of(78000), Optional.of(78000), List.of(),
                List.of(new EyeEvolution("mangekyo_itachi",
                        List.of(EyeRequirement.clan("otsutsuki_uchiha"), EyeRequirement.useAtLeast(360000)),
                        List.of("mangekyo_itachi", "mangekyo_noexiste"))),
                List.of(), false, 0, Optional.empty(), Optional.empty());
        Function<String, Optional<EyeDefinition>> world = lookup(sharinganRng,
                terminal("mangekyo_itachi", EyeFamily.MANGEKYO));

        EyeState start = EyeState.DEFAULT.withActiveEye("sharingan").withUseLevel(EyeFamily.SHARINGAN, 359999);
        for (long seed = 0; seed < 10; seed++) {
            EvolveResult r = EvolutionService.evaluate(
                    start, Optional.of("uchiha"), NO_FLAGS, world, new java.util.Random(seed));
            assertEquals("mangekyo_itachi", r.state().activeEye().orElseThrow());
        }
    }
}
