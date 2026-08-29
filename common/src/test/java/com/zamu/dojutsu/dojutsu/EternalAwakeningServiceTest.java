package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.eye.EyeRequirement;
import com.zamu.dojutsu.dojutsu.EternalAwakeningService.AwakenOutcome;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de la decision pura del despertar del EMS (fleco F5): las puertas (ojo eternalizable, no eterno ya,
 * medicina 150, clan otsutsuki_uchiha, uso 864000) verificadas contra el datapack.
 */
class EternalAwakeningServiceTest {

    private static final int OK_MEDICAL = 200;
    private static final int OK_USE = 864000;

    private static EyeDefinition mangekyo() {
        return new EyeDefinition("mangekyo_itachi", EyeFamily.MANGEKYO, true, 20, 80,
                Optional.empty(), Optional.empty(), List.of("amaterasu"), List.of(),
                List.of(EyeRequirement.clan("otsutsuki_uchiha"), EyeRequirement.useAtLeast(864000)), true, 0, Optional.empty(), Optional.empty());
    }

    private static EyeDefinition byakugan() {
        // no eternalizable (sin eternalRequirements)
        return new EyeDefinition("byakugan", EyeFamily.BYAKUGAN, true, 20, 20,
                Optional.of(78000), Optional.empty(), List.of(), List.of(), List.of(), true, 0, Optional.empty(), Optional.empty());
    }

    private static EyeState mangekyoState(int use, boolean eternal) {
        return EyeState.DEFAULT.withActiveEye("mangekyo_itachi").withEternal(eternal)
                .withUseLevel(EyeFamily.MANGEKYO, use);
    }

    @Test
    void todoEnReglaDespierta() {
        AwakenOutcome outcome = EternalAwakeningService.evaluate(
                mangekyo(), mangekyoState(OK_USE, false), Optional.of("uchiha"), OK_MEDICAL);
        assertEquals(AwakenOutcome.AWAKENED, outcome);
    }

    @Test
    void unOjoNoEternalizableNoDespierta() {
        AwakenOutcome outcome = EternalAwakeningService.evaluate(
                byakugan(), EyeState.DEFAULT.withActiveEye("byakugan"), Optional.of("uchiha"), OK_MEDICAL);
        assertEquals(AwakenOutcome.NO_ETERNAL_EYE, outcome);
    }

    @Test
    void sinOjoActivoNoDespierta() {
        assertEquals(AwakenOutcome.NO_ETERNAL_EYE,
                EternalAwakeningService.evaluate(null, EyeState.DEFAULT, Optional.of("uchiha"), OK_MEDICAL));
    }

    @Test
    void siYaEsEternoNoVuelveADespertar() {
        AwakenOutcome outcome = EternalAwakeningService.evaluate(
                mangekyo(), mangekyoState(OK_USE, true), Optional.of("uchiha"), OK_MEDICAL);
        assertEquals(AwakenOutcome.ALREADY_ETERNAL, outcome);
    }

    @Test
    void sinMedicinaSuficienteNoDespierta() {
        AwakenOutcome outcome = EternalAwakeningService.evaluate(
                mangekyo(), mangekyoState(OK_USE, false), Optional.of("uchiha"),
                EternalAwakeningService.MEDICAL_REQUIREMENT - 1);
        assertEquals(AwakenOutcome.NEEDS_MEDICAL, outcome);
    }

    @Test
    void clanEquivocadoNoDespierta() {
        AwakenOutcome outcome = EternalAwakeningService.evaluate(
                mangekyo(), mangekyoState(OK_USE, false), Optional.of("hyuga"), OK_MEDICAL);
        assertEquals(AwakenOutcome.WRONG_CLAN, outcome);
    }

    @Test
    void elClanOtsutsukiTambienVale() {
        // otsutsuki_uchiha es compuesto: Otsutsuki O Uchiha
        AwakenOutcome outcome = EternalAwakeningService.evaluate(
                mangekyo(), mangekyoState(OK_USE, false), Optional.of("otsutsuki"), OK_MEDICAL);
        assertEquals(AwakenOutcome.AWAKENED, outcome);
    }

    @Test
    void mangekyoPocoDesarrolladoNoDespierta() {
        AwakenOutcome outcome = EternalAwakeningService.evaluate(
                mangekyo(), mangekyoState(OK_USE - 1, false), Optional.of("uchiha"), OK_MEDICAL);
        assertEquals(AwakenOutcome.NEEDS_DEVELOPMENT, outcome);
    }
}
