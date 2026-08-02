package com.zamu.dojutsu.domain.summoning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests de las invocaciones (F8/DJT-175): las dos puertas (firmar y usar), el veto que baja con la maestria y la
 * inconsistencia de suelos del datapack. Verificado contra {@code summoning/*} y {@code jutsu/summoning/cast}.
 */
class SummonTableTest {

    @Test
    void elVetoBajaAMedidaQueSubeElFuinjutsu() {
        assertEquals(SummonTable.COOLDOWN_LOW_FUINJUTSU, SummonTable.cooldownTicks(SummonTable.CAST_MIN_FUINJUTSU));
        assertEquals(SummonTable.COOLDOWN_LOW_FUINJUTSU,
                SummonTable.cooldownTicks(SummonTable.COOLDOWN_MID_THRESHOLD - 1));
        assertEquals(SummonTable.COOLDOWN_MID_FUINJUTSU,
                SummonTable.cooldownTicks(SummonTable.COOLDOWN_MID_THRESHOLD));
        assertEquals(SummonTable.COOLDOWN_HIGH_FUINJUTSU,
                SummonTable.cooldownTicks(SummonTable.COOLDOWN_HIGH_THRESHOLD));
        assertEquals(SummonTable.COOLDOWN_HIGH_FUINJUTSU, SummonTable.cooldownTicks(500)); // cap del skill
    }

    @Test
    void elSueloDelCastMandaSobreElDelTierBajo() {
        // inconsistencia del datapack: el tier bajo abre a 75 pero el cast exige 125
        assertTrue(SummonTable.TIER_LOW_FUINJUTSU < SummonTable.CAST_MIN_FUINJUTSU);
        assertTrue(SummonTier.LOW.canSummon(SummonTable.TIER_LOW_FUINJUTSU, SummonTable.CONTRACT_MIN_RANK));
        assertFalse(SummonTable.canCast(SummonTable.TIER_LOW_FUINJUTSU)); // ...y aqui se para
        assertTrue(SummonTable.canCast(SummonTable.CAST_MIN_FUINJUTSU));
    }

    @Test
    void firmarPideChuninParaCualquierTier() {
        int academyStudent = 0;
        for (SummonTier tier : SummonTier.values()) {
            assertFalse(tier.canForm(500, academyStudent), tier.id() + " no deberia firmarse sin rango");
        }
        // con Chunin y fuinjutsu de sobra, el bajo y el medio si
        assertTrue(SummonTier.LOW.canForm(500, SummonTable.CONTRACT_MIN_RANK));
        assertTrue(SummonTier.MID.canForm(500, SummonTable.CONTRACT_MIN_RANK));
    }

    @Test
    void elTierBajoNoPideFuinjutsuParaFirmar() {
        // ...pero si para usar: son dos puertas distintas
        assertTrue(SummonTier.LOW.canForm(0, SummonTable.CONTRACT_MIN_RANK));
        assertFalse(SummonTier.LOW.canSummon(0, SummonTable.CONTRACT_MIN_RANK));
    }

    @Test
    void elTierAltoPideUnRangoMas() {
        assertFalse(SummonTier.HIGH.canForm(SummonTier.HIGH_FUINJUTSU, SummonTable.CONTRACT_MIN_RANK));
        assertTrue(SummonTier.HIGH.canForm(SummonTier.HIGH_FUINJUTSU, SummonTable.HIGH_TIER_RANK));
        assertFalse(SummonTier.HIGH.canSummon(SummonTier.HIGH_FUINJUTSU, SummonTable.CONTRACT_MIN_RANK));
        assertTrue(SummonTier.HIGH.canSummon(SummonTier.HIGH_FUINJUTSU, SummonTable.HIGH_TIER_RANK));
    }

    @Test
    void elTierMedioPideFuinjutsuPeroNoRangoExtra() {
        assertFalse(SummonTier.MID.canForm(SummonTier.MID_FUINJUTSU - 1, SummonTable.CONTRACT_MIN_RANK));
        assertTrue(SummonTier.MID.canForm(SummonTier.MID_FUINJUTSU, SummonTable.CONTRACT_MIN_RANK));
    }

    @Test
    void laInvocacionDuraUnMinuto() {
        assertEquals(1_200, SummonTable.LIFETIME_TICKS);
    }

    @Test
    void losTiersSeResuelvenPorId() {
        assertEquals(Optional.of(SummonTier.HIGH), SummonTier.byId("high"));
        assertTrue(SummonTier.byId("legendary").isEmpty()); // estado tolerante
    }
}
