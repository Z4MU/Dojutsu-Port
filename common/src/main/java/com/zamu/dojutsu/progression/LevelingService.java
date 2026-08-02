package com.zamu.dojutsu.progression;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.NinjaXpChangedEvent;
import com.zamu.dojutsu.domain.event.SkillLevelChangedEvent;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.XpCurve;
import java.util.Locale;
import java.util.UUID;

/**
 * Motor de auto-nivelacion por XP ({@code menu/leveling/main} del datapack, {@code [S13]}). Al anadir XP a una
 * habilidad, sube <b>tantos niveles como corresponda de una sola vez</b> (sin la guardia "1 level-up/tick" del
 * datapack, prohibida) y emite {@link SkillLevelChangedEvent} si el nivel cambio.
 *
 * <p><b>Reactivo, no por tick:</b> la nivelacion ocurre al escribir la XP (evento discreto: uso de jutsu,
 * gasto, hito), no sondeando el pipeline cada tick. Es el modelo que habilito {@code LeveledStat} en F1;
 * no se registra ningun {@code PlayerTickSystem} (no hay nada que sondear).
 *
 * <p>Logica <b>pura</b> (muta {@link PlayerNinjaData}, recibe el {@link DomainEventBus} inyectado): testeable
 * sin Minecraft. La fachada de runtime es {@link XpApi} (unica puerta de XP).
 */
public final class LevelingService {

    private LevelingService() {
    }

    /**
     * Anade XP a una habilidad y auto-nivela. Ignora XP no positiva y las habilidades sin auto-nivelacion
     * ({@code senjutsu}/{@code cc}). Emite {@link SkillLevelChangedEvent} solo si el nivel cambia.
     */
    public static void addSkillXp(PlayerNinjaData data, UUID player, Skill skill, int xp, DomainEventBus bus) {
        if (xp <= 0 || !skill.autoLevels()) {
            return; // XP no positiva, o habilidad que solo sube por puntos: se ignora
        }
        ProgressionState progression = data.progression();
        LeveledStat current = progression.skill(skill);
        int oldLevel = current.level();
        LeveledStat updated = current.withXpAdded(xp); // copia nueva: multi-nivel con arrastre, cap 500
        data.setProgression(progression.withSkill(skill, updated));
        if (updated.level() != oldLevel) {
            bus.publish(new SkillLevelChangedEvent(player, skill.name().toLowerCase(Locale.ROOT), oldLevel, updated.level()));
        }
    }

    /**
     * Sube el <b>nivel</b> de una habilidad directamente ({@code d.<skill> += N} del datapack: bonus de clan al
     * unirse, DJT-057), conservando su XP y clampando al cap. Emite {@link SkillLevelChangedEvent} si el nivel
     * cambia — asi los desbloqueos por hito reaccionan tambien a los bonus de clan (reactivo, como todo).
     *
     * <p>A diferencia de {@code SkillPointService.spend}, aqui no se paga nada (es un regalo del clan), asi que
     * al llegar al cap se <b>clampa</b> en vez de rechazar (mismo criterio que {@code addAffinityLevels}).
     *
     * @param levels niveles a sumar, positivos (se ignoran los no positivos)
     */
    public static void addSkillLevels(PlayerNinjaData data, UUID player, Skill skill, int levels, DomainEventBus bus) {
        if (levels <= 0) {
            return;
        }
        ProgressionState progression = data.progression();
        LeveledStat current = progression.skill(skill);
        int raised = Math.min(XpCurve.MAX_LEVEL, current.level() + levels);
        if (raised == current.level()) {
            return; // ya estaba al cap
        }
        data.setProgression(progression.withSkill(skill, new LeveledStat(raised, current.xp())));
        bus.publish(new SkillLevelChangedEvent(player, skill.name().toLowerCase(Locale.ROOT), current.level(), raised));
    }

    /**
     * Resta XP ninja global (suelo en 0) y emite {@link NinjaXpChangedEvent} si el valor cambia. Lo usa
     * {@code NatureService.revoke} ({@code natures/take/<nat>} resta el mismo ninja_xp que dio el {@code give}).
     *
     * <p>Va aparte de {@link #addNinjaXp} en vez de admitir cantidades negativas alli: el contrato de
     * {@code addNinjaXp} es "ignora XP no positiva", y relajarlo convertiria un {@code addNinjaXp(data, p, -5)}
     * accidental en una resta silenciosa. Aqui la intencion de restar es explicita.
     *
     * @param amount cantidad a restar, positiva (se ignoran las no positivas)
     */
    public static void removeNinjaXp(PlayerNinjaData data, UUID player, int amount, DomainEventBus bus) {
        if (amount <= 0) {
            return;
        }
        ProgressionState progression = data.progression();
        int oldXp = progression.ninjaXp();
        int newXp = Math.max(0, oldXp - amount); // el datapack no tiene suelo; aqui la ninja_xp nunca es negativa
        if (newXp == oldXp) {
            return; // ya estaba a 0: sin cambio, sin evento
        }
        data.setProgression(progression.withNinjaXp(newXp));
        bus.publish(new NinjaXpChangedEvent(player, oldXp, newXp));
    }

    /**
     * Anade XP ninja global y emite {@link NinjaXpChangedEvent} (para el rango). Ignora XP no positiva.
     */
    public static void addNinjaXp(PlayerNinjaData data, UUID player, int xp, DomainEventBus bus) {
        if (xp <= 0) {
            return;
        }
        ProgressionState progression = data.progression();
        int oldXp = progression.ninjaXp();
        int newXp = oldXp + xp;
        data.setProgression(progression.withNinjaXp(newXp));
        bus.publish(new NinjaXpChangedEvent(player, oldXp, newXp));
    }
}
