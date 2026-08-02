package com.zamu.dojutsu.progression;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.config.DojutsuConfig;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.XpCurve;
import com.zamu.dojutsu.network.NinjaSync;
import dev.architectury.event.EventResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.slf4j.Logger;

/**
 * Skill points del jugador ({@code [S13]}): se ganan matando entidades y se gastan subiendo habilidades.
 * Reemplaza los advancements {@code killed_mob/ply/boss} y las funciones {@code try_increase_*} del datapack.
 *
 * <p>SRP: {@link #award} y {@link #spend} son <b>puros</b> (mutan {@link PlayerNinjaData}, testeables sin
 * Minecraft); {@link #onKill} es el enganche del evento de muerte (glue con MC) que decide los puntos por tipo
 * de victima y otorga. La escritura de progresion pasa siempre por la puerta {@code setProgression}.
 */
public final class SkillPointService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** ninja_xp por skill point gastado (base; el bonus por clan afin llega en F3). {@code [S13]} */
    private static final int NINJA_XP_PER_POINT = 2;

    private SkillPointService() {
    }

    // --- Servicio puro (sobre el estado) ---

    /** Otorga skill points (ignora cantidades no positivas). */
    public static void award(PlayerNinjaData data, int points) {
        if (points <= 0) {
            return;
        }
        ProgressionState progression = data.progression();
        data.setProgression(progression.withSkillPoints(progression.skillPoints() + points));
    }

    /**
     * Gasta {@code points} skill points subiendo el <b>nivel</b> de {@code skill} (todo o nada): +1 nivel por
     * punto, descontando los puntos. Rechaza si no hay puntos suficientes, si la cantidad no es positiva o si
     * excederia el cap de nivel.
     *
     * <p>Version <b>pura</b> (no toca ninja_xp): la recompensa de ninja_xp del datapack (+2/punto) la aplica la
     * sobrecarga {@link #spend(ServerPlayer, Skill, int)} por la unica puerta {@link XpApi} (que emite evento).
     *
     * @return {@code true} si se aplico el gasto; {@code false} si se rechazo (sin efecto)
     */
    public static boolean spend(PlayerNinjaData data, Skill skill, int points) {
        if (points <= 0) {
            return false;
        }
        ProgressionState progression = data.progression();
        if (progression.skillPoints() < points) {
            return false; // puntos insuficientes
        }
        LeveledStat current = progression.skill(skill);
        if (current.level() + points > XpCurve.MAX_LEVEL) {
            return false; // no exceder el cap de nivel
        }
        LeveledStat raised = new LeveledStat(current.level() + points, current.xp());
        data.setProgression(progression
                .withSkill(skill, raised)
                .withSkillPoints(progression.skillPoints() - points));
        return true;
    }

    /**
     * Gasta puntos subiendo la habilidad y otorga la recompensa de ninja_xp (+2/punto) por {@link XpApi} (unica
     * puerta de XP; emite {@code NinjaXpChangedEvent} -> rango). El bonus por clan afin llega en F3.
     *
     * @return {@code true} si se aplico el gasto; {@code false} si se rechazo (sin efecto)
     */
    public static boolean spend(ServerPlayer player, Skill skill, int points) {
        if (!spend(NinjaData.get(player), skill, points)) {
            return false;
        }
        XpApi.addNinjaXp(player, NINJA_XP_PER_POINT * points);
        return true;
    }

    // --- Enganche del evento de muerte (el atacante gana puntos) ---

    /**
     * Handler de {@code EntityEvent.LIVING_DEATH}: si el responsable de la muerte es un jugador, le otorga
     * skill points segun el tipo de victima. {@code source.getEntity()} es el responsable real (el jugador
     * tambien en kills por proyectil).
     */
    public static EventResult onKill(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer) || killer == victim) {
            return EventResult.pass(); // no fue un jugador, o auto-muerte
        }
        int points = pointsFor(victim);
        award(NinjaData.get(killer), points);
        NinjaSync.sendDelta(killer); // los puntos son observables (menu): sincroniza el cambio
        LOGGER.info("[{}] {} gano {} skill point(s) por matar a {}.",
                DojutsuMod.MOD_NAME, killer.getGameProfile().getName(), points, victim.getType().getDescriptionId());
        return EventResult.pass();
    }

    private static int pointsFor(LivingEntity victim) {
        DojutsuConfig config = DojutsuConfig.get();
        if (victim instanceof ServerPlayer) {
            return config.killPlayerPoints();
        }
        if (isBoss(victim)) {
            return config.killBossPoints();
        }
        return config.killMobPoints();
    }

    private static boolean isBoss(LivingEntity victim) {
        // Jefes vanilla; las bestias con cola (S09) se anaden como jefes en F7.
        return victim instanceof EnderDragon || victim instanceof WitherBoss;
    }
}
