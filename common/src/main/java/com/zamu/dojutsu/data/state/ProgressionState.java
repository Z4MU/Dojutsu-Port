package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.NinjaRank;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sub-estado de progresion del ninja {@code [S13]}/{@code [S20]}: skill points, el nivel/XP de las 7
 * habilidades ({@link Skill}) como {@link LeveledStat}, la XP ninja global y los desbloqueos obtenidos.
 *
 * <p>Value object <b>inmutable</b> (patron de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}): expone {@link #DEFAULT}, {@link #CODEC} y withers.
 * {@link LeveledStat} tambien es inmutable (DJT-063), asi que el estado es inmutable <b>hasta el fondo</b>:
 * para cambiar una habilidad se <b>reemplaza</b> con {@link #withSkill}, y {@link #DEFAULT} no es corruptible.
 *
 * <p>El <b>rango</b> ({@link #rank()}) es <b>derivado</b> de {@link #ninjaXp} ({@code NinjaRank.fromXp}), no un
 * campo persistido: asi no puede desincronizarse (satisface "rango cacheado, no fuente de verdad" de DJT-037
 * sin el riesgo de un cache almacenado). {@code RankService} (DJT-037) solo detecta el cambio y sincroniza.
 *
 * <p>La escala de afinidad y skill point es <b>compartida</b> ({@code [S13-R1]}): un skill point sube el mismo
 * {@code LeveledStat} que la XP de uso; no son dos ejes. Las afinidades viven en {@code NatureProfile} (F3).
 *
 * @param skillPoints puntos de habilidad disponibles para gastar
 * @param skills      nivel/XP por habilidad (siempre las 7; mapa inmutable)
 * @param ninjaXp     XP ninja global (sumidero de {@code [S20]}; deriva el rango)
 * @param unlocked    ids de desbloqueos ya obtenidos (idempotencia, {@code [S20]})
 */
public record ProgressionState(int skillPoints, Map<Skill, LeveledStat> skills, int ninjaXp, Set<String> unlocked) {

    /** Estado por defecto de un jugador nuevo: sin puntos, todas las habilidades a nivel 0, sin XP ni unlocks. */
    public static final ProgressionState DEFAULT = new ProgressionState(0, Map.of(), 0, Set.of());

    /**
     * Codec del sub-estado. Cada habilidad es un campo opcional propio (tolerancia por habilidad); el codec de
     * cada {@link LeveledStat} es {@link LeveledStats#CODEC}, compartido con las afinidades ({@code [S13-R1]}:
     * mismo eje nivel/XP).
     */
    public static final Codec<ProgressionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("skillPoints", 0).forGetter(ProgressionState::skillPoints),
            LeveledStats.CODEC.optionalFieldOf("taijutsu", new LeveledStat()).forGetter(s -> s.skill(Skill.TAIJUTSU)),
            LeveledStats.CODEC.optionalFieldOf("genjutsu", new LeveledStat()).forGetter(s -> s.skill(Skill.GENJUTSU)),
            LeveledStats.CODEC.optionalFieldOf("medical", new LeveledStat()).forGetter(s -> s.skill(Skill.MEDICAL)),
            LeveledStats.CODEC.optionalFieldOf("fuinjutsu", new LeveledStat()).forGetter(s -> s.skill(Skill.FUINJUTSU)),
            LeveledStats.CODEC.optionalFieldOf("kenjutsu", new LeveledStat()).forGetter(s -> s.skill(Skill.KENJUTSU)),
            LeveledStats.CODEC.optionalFieldOf("senjutsu", new LeveledStat()).forGetter(s -> s.skill(Skill.SENJUTSU)),
            LeveledStats.CODEC.optionalFieldOf("cc", new LeveledStat()).forGetter(s -> s.skill(Skill.CC)),
            Codec.INT.optionalFieldOf("ninjaXp", 0).forGetter(ProgressionState::ninjaXp),
            Codec.STRING.listOf().optionalFieldOf("unlocked", List.of())
                    .forGetter(s -> s.unlocked().stream().sorted().toList())
    ).apply(instance, ProgressionState::fromCodec));

    /** Normaliza: no-negativos, mapa de habilidades completo (las 7) e inmutable, unlocks inmutable. */
    public ProgressionState {
        skillPoints = Math.max(0, skillPoints);
        ninjaXp = Math.max(0, ninjaXp);
        skills = normalizeSkills(skills);
        unlocked = Set.copyOf(unlocked);
    }

    /** {@code LeveledStat} de una habilidad (siempre presente). */
    public LeveledStat skill(Skill skill) {
        return skills.get(skill);
    }

    /** Rango ninja <b>derivado</b> de la XP ninja global ({@code [S20]}). Cosmetico; no fuente de verdad. */
    public NinjaRank rank() {
        return NinjaRank.fromXp(ninjaXp);
    }

    /** Copia con otros skill points. Inmutable. */
    public ProgressionState withSkillPoints(int newSkillPoints) {
        return new ProgressionState(newSkillPoints, skills, ninjaXp, unlocked);
    }

    /** Copia reemplazando el {@link LeveledStat} de una habilidad. Inmutable. */
    public ProgressionState withSkill(Skill skill, LeveledStat stat) {
        EnumMap<Skill, LeveledStat> next = new EnumMap<>(skills);
        next.put(skill, stat);
        return new ProgressionState(skillPoints, next, ninjaXp, unlocked);
    }

    /** Copia con otra XP ninja global. Inmutable. */
    public ProgressionState withNinjaXp(int newNinjaXp) {
        return new ProgressionState(skillPoints, skills, newNinjaXp, unlocked);
    }

    /** Copia anadiendo un desbloqueo (idempotente: si ya estaba, devuelve la misma instancia). Inmutable. */
    public ProgressionState withUnlock(String unlockId) {
        if (unlocked.contains(unlockId)) {
            return this;
        }
        HashSet<String> next = new HashSet<>(unlocked);
        next.add(unlockId);
        return new ProgressionState(skillPoints, skills, ninjaXp, next);
    }

    private static ProgressionState fromCodec(int skillPoints, LeveledStat taijutsu, LeveledStat genjutsu,
            LeveledStat medical, LeveledStat fuinjutsu, LeveledStat kenjutsu, LeveledStat senjutsu,
            LeveledStat cc, int ninjaXp, List<String> unlocked) {
        EnumMap<Skill, LeveledStat> skills = new EnumMap<>(Skill.class);
        skills.put(Skill.TAIJUTSU, taijutsu);
        skills.put(Skill.GENJUTSU, genjutsu);
        skills.put(Skill.MEDICAL, medical);
        skills.put(Skill.FUINJUTSU, fuinjutsu);
        skills.put(Skill.KENJUTSU, kenjutsu);
        skills.put(Skill.SENJUTSU, senjutsu);
        skills.put(Skill.CC, cc);
        return new ProgressionState(skillPoints, skills, ninjaXp, new HashSet<>(unlocked));
    }

    private static Map<Skill, LeveledStat> normalizeSkills(Map<Skill, LeveledStat> given) {
        EnumMap<Skill, LeveledStat> complete = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) {
            LeveledStat stat = given.get(skill);
            complete.put(skill, stat != null ? stat : new LeveledStat());
        }
        return Collections.unmodifiableMap(complete);
    }
}
