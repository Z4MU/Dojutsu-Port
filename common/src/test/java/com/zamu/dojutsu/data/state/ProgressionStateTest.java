package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.NinjaRank;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** Tests del sub-estado de progresion (DJT-034): defaults, 7 habilidades, rango derivado e integridad NBT. */
class ProgressionStateTest {

    private static ProgressionState decode(Tag tag) {
        return ProgressionState.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    private static Tag encode(ProgressionState state) {
        return ProgressionState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
    }

    @Test
    void defaultTieneLas7HabilidadesANivel0() {
        ProgressionState d = ProgressionState.DEFAULT;
        assertEquals(0, d.skillPoints());
        assertEquals(0, d.ninjaXp());
        assertEquals(Skill.values().length, d.skills().size());
        for (Skill skill : Skill.values()) {
            assertEquals(new LeveledStat(0, 0), d.skill(skill));
        }
        assertTrue(d.unlocked().isEmpty());
    }

    @Test
    void tresHabilidadesAutonivelanDosNo() {
        // senjutsu y cc solo suben por puntos (sin auto-nivelacion): postura [ADD/00 §3].
        assertTrue(Skill.TAIJUTSU.autoLevels());
        assertTrue(Skill.KENJUTSU.autoLevels());
        assertFalse(Skill.SENJUTSU.autoLevels());
        assertFalse(Skill.CC.autoLevels());
    }

    @Test
    void elDefaultNoSeCorrompeAlSubirUnaHabilidad() {
        // DJT-063: LeveledStat es inmutable, asi que operar sobre el DEFAULT estatico no puede envenenarlo.
        ProgressionState.DEFAULT.skill(Skill.TAIJUTSU).withXpAdded(500);
        assertEquals(new LeveledStat(0, 0), ProgressionState.DEFAULT.skill(Skill.TAIJUTSU));
    }

    @Test
    void rangoDerivadoDeLaNinjaXp() {
        assertEquals(NinjaRank.ACADEMY_STUDENT, ProgressionState.DEFAULT.rank());
        assertEquals(NinjaRank.GENIN, ProgressionState.DEFAULT.withNinjaXp(1_000).rank());
        assertEquals(NinjaRank.GOD, ProgressionState.DEFAULT.withNinjaXp(999_999).rank());
    }

    @Test
    void roundTripNbtNoPierdeEstado() {
        ProgressionState original = ProgressionState.DEFAULT
                .withSkillPoints(5)
                .withNinjaXp(2_500)
                .withSkill(Skill.KENJUTSU, new LeveledStat(12, 7))
                .withUnlock("izanagi");

        ProgressionState decoded = decode(encode(original));

        assertEquals(5, decoded.skillPoints());
        assertEquals(2_500, decoded.ninjaXp());
        assertEquals(new LeveledStat(12, 7), decoded.skill(Skill.KENJUTSU));
        assertTrue(decoded.unlocked().contains("izanagi"));
        assertEquals(NinjaRank.CHUNIN, decoded.rank());
        assertEquals(original, decoded);
    }

    @Test
    void nbtVacioCaeADefault() {
        assertEquals(ProgressionState.DEFAULT, decode(new CompoundTag()));
    }

    @Test
    void saveParcialRellenaHabilidadesAusentes() {
        // Solo trae taijutsu; el resto de habilidades toman el default (nivel 0).
        CompoundTag taijutsu = new CompoundTag();
        taijutsu.putInt("level", 8);
        CompoundTag root = new CompoundTag();
        root.put("taijutsu", taijutsu);
        ProgressionState decoded = decode(root);
        assertEquals(8, decoded.skill(Skill.TAIJUTSU).level());
        assertEquals(new LeveledStat(0, 0), decoded.skill(Skill.MEDICAL));
    }

    @Test
    void codecClampaNivelCorrupto() {
        CompoundTag genjutsu = new CompoundTag();
        genjutsu.putInt("level", 99_999); // por encima del cap 500
        genjutsu.putInt("xp", -5);        // negativo
        CompoundTag root = new CompoundTag();
        root.put("genjutsu", genjutsu);
        ProgressionState decoded = decode(root);
        assertEquals(500, decoded.skill(Skill.GENJUTSU).level());
        assertEquals(0, decoded.skill(Skill.GENJUTSU).xp());
    }

    @Test
    void withUnlockEsIdempotente() {
        ProgressionState once = ProgressionState.DEFAULT.withUnlock("izanagi");
        ProgressionState twice = once.withUnlock("izanagi");
        assertSame(once, twice); // ya estaba: misma instancia
        assertEquals(1, once.unlocked().size());
    }

    @Test
    void skillPointsYNinjaXpNoNegativos() {
        ProgressionState s = ProgressionState.DEFAULT.withSkillPoints(-3).withNinjaXp(-10);
        assertEquals(0, s.skillPoints());
        assertEquals(0, s.ninjaXp());
    }
}
