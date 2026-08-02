package com.zamu.dojutsu.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.XpCurve;
import org.junit.jupiter.api.Test;

/** Tests de la ganancia y gasto de skill points (DJT-035). Puro sobre el estado, sin Minecraft. */
class SkillPointServiceTest {

    private static PlayerNinjaData withPoints(int points) {
        PlayerNinjaData data = new PlayerNinjaData();
        SkillPointService.award(data, points);
        return data;
    }

    @Test
    void awardSumaPuntos() {
        PlayerNinjaData data = new PlayerNinjaData();
        SkillPointService.award(data, 5);
        assertEquals(5, data.progression().skillPoints());
        SkillPointService.award(data, 3);
        assertEquals(8, data.progression().skillPoints());
    }

    @Test
    void awardIgnoraCantidadesNoPositivas() {
        PlayerNinjaData data = withPoints(4);
        SkillPointService.award(data, 0);
        SkillPointService.award(data, -10);
        assertEquals(4, data.progression().skillPoints());
    }

    @Test
    void spendSubeNivelYDescuentaPuntos() {
        // La version pura sube nivel y descuenta puntos; el ninja_xp lo aplica la sobrecarga MC via XpApi.
        PlayerNinjaData data = withPoints(3);
        assertTrue(SkillPointService.spend(data, Skill.TAIJUTSU, 1));
        assertEquals(1, data.progression().skill(Skill.TAIJUTSU).level());
        assertEquals(2, data.progression().skillPoints());
        assertEquals(0, data.progression().ninjaXp()); // la version pura no toca ninja_xp
    }

    @Test
    void spendMultiPunto() {
        PlayerNinjaData data = withPoints(5);
        assertTrue(SkillPointService.spend(data, Skill.KENJUTSU, 3));
        assertEquals(3, data.progression().skill(Skill.KENJUTSU).level());
        assertEquals(2, data.progression().skillPoints());
    }

    @Test
    void spendRechazaSiFaltanPuntos() {
        PlayerNinjaData data = withPoints(1);
        assertFalse(SkillPointService.spend(data, Skill.MEDICAL, 2));
        assertEquals(0, data.progression().skill(Skill.MEDICAL).level());
        assertEquals(1, data.progression().skillPoints()); // sin efecto
    }

    @Test
    void spendRechazaCantidadNoPositiva() {
        PlayerNinjaData data = withPoints(3);
        assertFalse(SkillPointService.spend(data, Skill.GENJUTSU, 0));
        assertFalse(SkillPointService.spend(data, Skill.GENJUTSU, -1));
        assertEquals(3, data.progression().skillPoints());
    }

    @Test
    void spendFuncionaEnHabilidadSinAutonivel() {
        // senjutsu/cc no auto-nivelan por XP, pero SI son gastables por puntos.
        PlayerNinjaData data = withPoints(2);
        assertTrue(SkillPointService.spend(data, Skill.SENJUTSU, 2));
        assertEquals(2, data.progression().skill(Skill.SENJUTSU).level());
    }

    @Test
    void spendRechazaSiExcedeElCap() {
        PlayerNinjaData data = withPoints(5);
        data.setProgression(data.progression().withSkill(Skill.CC, new LeveledStat(XpCurve.MAX_LEVEL - 1, 0)));
        assertFalse(SkillPointService.spend(data, Skill.CC, 5)); // 499 + 5 > 500
        assertEquals(XpCurve.MAX_LEVEL - 1, data.progression().skill(Skill.CC).level());
        assertTrue(SkillPointService.spend(data, Skill.CC, 1)); // 499 + 1 = 500 (cap exacto)
        assertEquals(XpCurve.MAX_LEVEL, data.progression().skill(Skill.CC).level());
    }
}
