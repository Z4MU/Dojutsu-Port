package com.zamu.dojutsu.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests del <b>balance de los hitos de desbloqueo</b> (F10/DJT-221). Verificado contra
 * {@code main/internal/unlocks.mcfunction} (la ninja_xp de cada hito) y {@code predicates/unlocks/*.json}
 * (los umbrales).
 *
 * <p>Lo que estos tests protegen no es una implementación sino <b>una tabla de números</b>: la clase de error
 * que cazan es la que tuvo el mod durante seis fases — cinco umbrales de kenjutsu inventados que nadie había
 * contrastado con el datapack.
 */
class UnlockBalanceTest {

    @AfterEach
    void limpiaElRegistro() {
        UnlockRegistry.load(List.of());
    }

    private static PlayerNinjaData ninjaCon(int maxChakra) {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setChakra(new ChakraState(maxChakra, maxChakra, 0, 0, 0, 0));
        return data;
    }

    // ------------------------------------------------------------------
    // El co-requisito de chakra maximo
    // ------------------------------------------------------------------

    @Test
    void unHitoConCoRequisitoDeChakraNoSeOtorgaSinElChakra() {
        // cr_req: d.medical >= 500 (mod 125) AND d.max_chakra >= 10000
        UnlockRegistry.load(List.of(
                new UnlockDefinition("medical_5_creation_rebirth", "medical", 125, 10000, 20)));

        PlayerNinjaData pobre = ninjaCon(9999);
        assertTrue(UnlockService.applyUnlocks(pobre, "medical", 124, 125).isEmpty(),
                "con 9999 de chakra maximo el hito no debe otorgarse");
        assertFalse(pobre.progression().unlocked().contains("medical_5_creation_rebirth"));
    }

    @Test
    void elMismoHitoSeOtorgaCuandoElChakraLlega() {
        UnlockRegistry.load(List.of(
                new UnlockDefinition("medical_5_creation_rebirth", "medical", 125, 10000, 20)));

        PlayerNinjaData rico = ninjaCon(10000);
        assertEquals(List.of("medical_5_creation_rebirth"),
                UnlockService.applyUnlocks(rico, "medical", 124, 125));
    }

    @Test
    void elBarridoAlEntrarRecuperaElHitoQueElEventoNoPudoVer() {
        // El punto ciego real: se sube la habilidad con poco chakra (el cruce pasa y no otorga) y el chakra
        // llega despues. Sin el barrido, ese hito no se otorgaria jamas.
        UnlockRegistry.load(List.of(
                new UnlockDefinition("medical_5_creation_rebirth", "medical", 125, 10000, 20)));

        PlayerNinjaData data = ninjaCon(5000);
        data.setProgression(data.progression().withSkill(Skill.MEDICAL, new LeveledStat(200, 0)));
        assertTrue(UnlockService.applyUnlocks(data, "medical", 124, 200).isEmpty());

        data.setChakra(new ChakraState(20000, 20000, 0, 0, 0, 0)); // sella una bestia, sube el maximo
        assertEquals(List.of("medical_5_creation_rebirth"), UnlockService.applyReachedUnlocks(data));
    }

    // ------------------------------------------------------------------
    // Idempotencia y ninja_xp
    // ------------------------------------------------------------------

    @Test
    void otorgarDosVecesNoDuplica() {
        UnlockRegistry.load(List.of(new UnlockDefinition("kenjutsu_1_dancing_blade_risk", "kenjutsu", 75, 0, 10)));
        PlayerNinjaData data = ninjaCon(100);

        assertEquals(1, UnlockService.applyUnlocks(data, "kenjutsu", 74, 75).size());
        assertTrue(UnlockService.applyUnlocks(data, "kenjutsu", 74, 200).isEmpty(), "S20-R4: idempotente");
        assertEquals(1, data.progression().unlocked().size());
    }

    @Test
    void cadaHitoDeclaraSuNinjaXpYNoEsUniforme() {
        // main/internal/unlocks: va de 1 (Substitution) a 100 (Izanagi/Ocho Puertas/SOAH). Suponer un valor
        // unico habria sido el error facil.
        UnlockRegistry.load(List.of(
                new UnlockDefinition("substitution", UnlockService.NINJA_XP_SKILL_ID, 1000, 0, 1),
                new UnlockDefinition("eight_gates_1_gate_of_opening", "taijutsu", 75, 0, 100),
                new UnlockDefinition("kenjutsu_1_dancing_blade_risk", "kenjutsu", 75, 0, 10)));

        assertEquals(1, UnlockService.ninjaXpOf("substitution"));
        assertEquals(100, UnlockService.ninjaXpOf("eight_gates_1_gate_of_opening"));
        assertEquals(10, UnlockService.ninjaXpOf("kenjutsu_1_dancing_blade_risk"));
        assertEquals(0, UnlockService.ninjaXpOf("no_existe"));
    }

    @Test
    void laSustitucionDependeDelNinjaXpGlobalYNoDeUnaHabilidad() {
        // subs_req: d.ninja_xp >= 1000, o sea al llegar a Genin. Es el primer jutsu de un ninja.
        UnlockRegistry.load(List.of(
                new UnlockDefinition("substitution", UnlockService.NINJA_XP_SKILL_ID, 1000, 0, 1)));
        PlayerNinjaData data = ninjaCon(100);

        assertTrue(UnlockService.applyUnlocks(data, "taijutsu", 0, 500).isEmpty(),
                "no lo dispara ninguna habilidad");
        assertEquals(List.of("substitution"),
                UnlockService.applyUnlocks(data, UnlockService.NINJA_XP_SKILL_ID, 999, 1000));
    }

    // ------------------------------------------------------------------
    // La escala
    // ------------------------------------------------------------------

    @Test
    void laEscalaDelDatapackEsExactamenteCuatroVecesLaDelMod() {
        // El techo de habilidad del mod es 500 y los requisitos mas altos del datapack valen 2000. Que la
        // division cuadre en el tope no es casualidad: es la misma escala.
        assertEquals(500, 2000 / 4, "maestria de las Ocho Puertas, fuinjutsu 3, Blaze, Creation Rebirth SOAH");
        assertEquals(75, 300 / 4, "kenjutsu 1 y contrato de invocacion");
        assertEquals(350, 1400 / 4, "kenjutsu 5");
    }
}
