package com.zamu.dojutsu.domain.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests de la IA del shinobi NPC (F8/DJT-185). El ritmo sale del datapack ({@code shinobi/ai/*}); la banda de
 * distancia es <b>diseño nuevo</b> y aqui se fija para que no derive.
 */
class ShinobiCombatPlanTest {

    /** Distancia representativa de cada banda. */
    private static final double CLOSE = 2.0;
    private static final double MID = 10.0;
    private static final double FAR = 25.0;

    @Test
    void laVidaCreceDeCientoVeinteEnCientoVeinteYElKageRompeElPatron() {
        assertEquals(120.0, ThreatLevel.ACADEMY.maxHealth());
        assertEquals(240.0, ThreatLevel.GENIN.maxHealth());
        assertEquals(360.0, ThreatLevel.CHUNIN.maxHealth());
        assertEquals(480.0, ThreatLevel.TOKUBETSU_JONIN.maxHealth());
        assertEquals(600.0, ThreatLevel.JONIN.maxHealth());
        assertEquals(700.0, ThreatLevel.KAGE.maxHealth()); // seria 720 si la formula valiera
    }

    @Test
    void losSeisNivelesTienenIdUnicoDelDatapack() {
        Set<Integer> ids = new HashSet<>();
        for (ThreatLevel level : ThreatLevel.values()) {
            assertTrue(ids.add(level.datapackId()), "id repetido: " + level.datapackId());
        }
        assertEquals(6, ids.size());
    }

    @Test
    void laMitadDeLosTurnosNoHaceNada() {
        // el datapack tiene "say do nothing" en el 0, el 4 y el 5: tres de seis
        int idle = 0;
        for (int roll = ShinobiCombatPlan.ROLL_MIN; roll <= ShinobiCombatPlan.ROLL_MAX; roll++) {
            if (ShinobiCombatPlan.choose(ThreatLevel.KAGE, roll, MID) == ShinobiCombatPlan.Action.IDLE) {
                idle++;
            }
        }
        assertEquals(3, idle);
    }

    @Test
    void aBocajarroNoSeLanzanBolasDeFuegoALosPies() {
        // diseño nuevo: la distancia filtra la intencion del dado
        for (int roll = 1; roll <= 3; roll++) {
            assertEquals(ShinobiCombatPlan.Action.MELEE,
                    ShinobiCombatPlan.choose(ThreatLevel.KAGE, roll, CLOSE));
        }
    }

    @Test
    void aMediaDistanciaValeTodo() {
        assertEquals(ShinobiCombatPlan.Action.THROW_KUNAI,
                ShinobiCombatPlan.choose(ThreatLevel.KAGE, 1, MID));
        assertEquals(ShinobiCombatPlan.Action.WEAK_JUTSU,
                ShinobiCombatPlan.choose(ThreatLevel.KAGE, 2, MID));
        assertEquals(ShinobiCombatPlan.Action.STRONG_JUTSU,
                ShinobiCombatPlan.choose(ThreatLevel.KAGE, 3, MID));
    }

    @Test
    void deLejosElKunaiNoLlegaYSeConvierteEnJutsu() {
        assertEquals(ShinobiCombatPlan.Action.WEAK_JUTSU,
                ShinobiCombatPlan.choose(ThreatLevel.KAGE, 1, FAR));
    }

    @Test
    void unAcademicoNoSabeJutsusYSiempreTiraKunai() {
        assertTrue(ThreatLevel.ACADEMY.jutsus().isEmpty());
        assertEquals(ShinobiCombatPlan.Action.THROW_KUNAI,
                ShinobiCombatPlan.choose(ThreatLevel.ACADEMY, 2, MID));
        assertEquals(ShinobiCombatPlan.Action.THROW_KUNAI,
                ShinobiCombatPlan.choose(ThreatLevel.ACADEMY, 3, MID));
        // y de lejos no puede hacer nada
        assertEquals(ShinobiCombatPlan.Action.IDLE,
                ShinobiCombatPlan.choose(ThreatLevel.ACADEMY, 1, FAR));
    }

    @Test
    void elRosterCreceConLaAmenaza() {
        assertEquals(0, ThreatLevel.ACADEMY.jutsus().size());
        assertEquals(1, ThreatLevel.GENIN.jutsus().size());
        assertEquals(5, ThreatLevel.KAGE.jutsus().size());
        // el debil y el fuerte son los extremos del roster
        assertEquals(java.util.Optional.of("great_fireball"), ThreatLevel.KAGE.weakJutsu());
        assertEquals(java.util.Optional.of("great_fire_annihilation"), ThreatLevel.KAGE.strongJutsu());
        // con un solo jutsu, debil y fuerte coinciden
        assertEquals(ThreatLevel.GENIN.weakJutsu(), ThreatLevel.GENIN.strongJutsu());
    }

    @Test
    void lasBandasDeDistanciaSonContiguas() {
        assertEquals(ShinobiCombatPlan.Band.MELEE,
                ShinobiCombatPlan.bandFor(ShinobiCombatPlan.MELEE_RANGE));
        assertEquals(ShinobiCombatPlan.Band.THROWING,
                ShinobiCombatPlan.bandFor(ShinobiCombatPlan.MELEE_RANGE + 0.1));
        assertEquals(ShinobiCombatPlan.Band.THROWING,
                ShinobiCombatPlan.bandFor(ShinobiCombatPlan.THROWING_RANGE));
        assertEquals(ShinobiCombatPlan.Band.RANGED,
                ShinobiCombatPlan.bandFor(ShinobiCombatPlan.THROWING_RANGE + 0.1));
    }

    @Test
    void laAccionOciosaNoResuelveNingunJutsu() {
        assertTrue(ShinobiCombatPlan.jutsuFor(ShinobiCombatPlan.Action.IDLE, ThreatLevel.KAGE).isEmpty());
        assertTrue(ShinobiCombatPlan.jutsuFor(ShinobiCombatPlan.Action.MELEE, ThreatLevel.KAGE).isEmpty());
        assertTrue(ShinobiCombatPlan.jutsuFor(ShinobiCombatPlan.Action.THROW_KUNAI, ThreatLevel.KAGE).isEmpty());
        assertFalse(ShinobiCombatPlan.jutsuFor(ShinobiCombatPlan.Action.WEAK_JUTSU, ThreatLevel.KAGE).isEmpty());
    }

    @Test
    void elRitmoEsElMismoQueElDeLasBestias() {
        // no puede ser casualidad: 60 ticks y 32 bloques, igual que BeastStats
        assertEquals(60, ShinobiCombatPlan.DECISION_INTERVAL_TICKS);
        assertEquals(32.0, ShinobiCombatPlan.ACTIVATION_RADIUS);
        assertEquals(6, ShinobiCombatPlan.ROLL_OUTCOMES);
    }
}
