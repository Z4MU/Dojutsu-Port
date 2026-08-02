package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.clans.ClanDefinition;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.dojutsu.AwakeningService.AwakenResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/**
 * Tests de la lógica pura de despertar del dojutsu (DJT-114): acumulación del temporizador, despertar al
 * umbral, 50/50 de Otsutsuki, "una vez por clan", y descarte de ojos no despertables (sin `awakenTicks`,
 * no implementados). Sin Minecraft (azar inyectado con semilla).
 */
class AwakeningServiceTest {

    private static ClanDefinition clan(String id, List<String> potentialDojutsu) {
        return new ClanDefinition(id, List.of(), List.of(), Map.of(), Map.of(), 100, 0, List.of(), List.of(),
                potentialDojutsu, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    private static EyeDefinition eye(String id, EyeFamily family, Optional<Integer> awakenTicks) {
        return eye(id, family, awakenTicks, Optional.empty());
    }

    private static EyeDefinition eye(String id, EyeFamily family, Optional<Integer> awakenTicks,
            Optional<Integer> awakenInitialUse) {
        return new EyeDefinition(id, family, true, 20, 20, awakenTicks, awakenInitialUse, List.of(), List.of(),
                List.of(), false, 0, Optional.empty());
    }

    private static Function<String, Optional<EyeDefinition>> lookup(EyeDefinition... eyes) {
        Map<String, EyeDefinition> byId = new java.util.HashMap<>();
        for (EyeDefinition eye : eyes) {
            byId.put(eye.id(), eye);
        }
        return id -> Optional.ofNullable(byId.get(id));
    }

    @Test
    void acumulaElTimerBajoElUmbral() {
        EyeDefinition byakugan = eye("byakugan", EyeFamily.BYAKUGAN, Optional.of(3));
        AwakenResult r = AwakeningService.evaluate(EyeState.DEFAULT, "hyuga", clan("hyuga", List.of("byakugan")),
                lookup(byakugan), new Random(0));

        assertTrue(r.awakenedEyeId().isEmpty());
        assertEquals(1, r.state().timer("hyuga"));
        assertFalse(r.state().isAwakened(EyeFamily.BYAKUGAN));
    }

    @Test
    void despiertaAlAlcanzarElUmbral() {
        EyeDefinition byakugan = eye("byakugan", EyeFamily.BYAKUGAN, Optional.of(3));
        EyeState start = EyeState.DEFAULT.withTimer("hyuga", 2); // el siguiente tick alcanza el umbral 3
        AwakenResult r = AwakeningService.evaluate(start, "hyuga", clan("hyuga", List.of("byakugan")),
                lookup(byakugan), new Random(0));

        assertEquals(Optional.of("byakugan"), r.awakenedEyeId());
        assertTrue(r.state().isAwakened(EyeFamily.BYAKUGAN));
        assertEquals("byakugan", r.state().activeEye().orElseThrow());
        assertEquals(0, r.state().timer("hyuga")); // el timer se reinicia al despertar
    }

    @Test
    void alDespertarFijaElUsoInicialSiLoDeclara() {
        // sharingan/awaken del datapack fija sharingan_use=78000 (nace en tomoe 1); byakugan no fija uso inicial.
        EyeDefinition sharingan = eye("sharingan", EyeFamily.SHARINGAN, Optional.of(3), Optional.of(78000));
        EyeState start = EyeState.DEFAULT.withTimer("uchiha", 2);
        AwakenResult r = AwakeningService.evaluate(start, "uchiha", clan("uchiha", List.of("sharingan")),
                lookup(sharingan), new Random(0));

        assertEquals(Optional.of("sharingan"), r.awakenedEyeId());
        assertEquals(78000, r.state().useLevel(EyeFamily.SHARINGAN)); // uso inicial fijado
    }

    @Test
    void otsutsukiEligeUnoDeLosDosOjos() {
        EyeDefinition byakugan = eye("byakugan", EyeFamily.BYAKUGAN, Optional.of(1));
        EyeDefinition sharingan = eye("sharingan", EyeFamily.SHARINGAN, Optional.of(1));
        AwakenResult r = AwakeningService.evaluate(EyeState.DEFAULT, "otsutsuki",
                clan("otsutsuki", List.of("byakugan", "sharingan")), lookup(byakugan, sharingan), new Random(0));

        assertTrue(r.awakenedEyeId().isPresent());
        assertTrue(List.of("byakugan", "sharingan").contains(r.awakenedEyeId().orElseThrow()));
        assertEquals(1, r.state().awakened().size()); // despierta exactamente uno
    }

    @Test
    void noVuelveADespertarSiYaTieneUnoDeSusPotenciales() {
        // Otsutsuki que ya tiene Byakugan no despierta ademas el Sharingan (uno por clan; unless _wake).
        EyeDefinition byakugan = eye("byakugan", EyeFamily.BYAKUGAN, Optional.of(1));
        EyeDefinition sharingan = eye("sharingan", EyeFamily.SHARINGAN, Optional.of(1));
        EyeState awoke = EyeState.DEFAULT.withAwakened(EyeFamily.BYAKUGAN);
        AwakenResult r = AwakeningService.evaluate(awoke, "otsutsuki",
                clan("otsutsuki", List.of("byakugan", "sharingan")), lookup(byakugan, sharingan), new Random(0));

        assertSame(awoke, r.state()); // misma instancia: sin cambios
        assertTrue(r.awakenedEyeId().isEmpty());
    }

    @Test
    void clanSinDojutsuPotencialNoDespierta() {
        AwakenResult r = AwakeningService.evaluate(EyeState.DEFAULT, "senju", clan("senju", List.of()),
                lookup(), new Random(0));
        assertSame(EyeState.DEFAULT, r.state());
    }

    @Test
    void ojoSinAwakenTicksNoEsDespertablePorClan() {
        // Un destino de evolucion (Mangekyo, sin awakenTicks) no se despierta por clan aunque este en potential.
        EyeDefinition mangekyo = eye("mangekyo_itachi", EyeFamily.MANGEKYO, Optional.empty());
        AwakenResult r = AwakeningService.evaluate(EyeState.DEFAULT, "x", clan("x", List.of("mangekyo_itachi")),
                lookup(mangekyo), new Random(0));
        assertSame(EyeState.DEFAULT, r.state());
    }

    @Test
    void ojoNoImplementadoSeIgnora() {
        EyeDefinition naka = new EyeDefinition("naka", EyeFamily.MANGEKYO, false, 0, 0, Optional.of(1),
                Optional.empty(), List.of(), List.of(), List.of(), false, 0, Optional.empty());
        AwakenResult r = AwakeningService.evaluate(EyeState.DEFAULT, "x", clan("x", List.of("naka")),
                lookup(naka), new Random(0));
        assertSame(EyeState.DEFAULT, r.state());
    }
}
