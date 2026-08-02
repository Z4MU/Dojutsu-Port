package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Truth-Seeking Orbs (Tenseigan): los orbes negros de energía — detonan en el punto al que apunta el lanzador,
 * dañando la zona. Primera habilidad del Tenseigan en el catálogo. Clona la plantilla de {@link AmaterasuAbility}.
 * {@code [S05]}, {@code tenseigan/.../truth_seeking_orb}.
 *
 * <p>Coste <b>300 chakra</b> (el {@code truth_seeking_orb/summon} consume 300; el datapack lo <b>gatea</b> a 600
 * de disponibilidad — aquí se usa el valor consumido). Sin fatiga (no encontrada). Aplica {@code instant_damage}
 * amp 2 (Harming III, cura no-muertos via {@link MobEffects#HARM}) a {@code distance 0..5} del impacto. Ajuste →
 * balance (F10).
 */
public final class TruthSeekingOrbsAbility implements EyeAbility {

    private static final int CHAKRA_COST = 300;
    private static final int STRAIN_COST = 0;
    private static final double REACH = 8.0;
    private static final double RADIUS = 5.0;    // distance 0..5 del datapack
    private static final int HARM_AMPLIFIER = 2; // instant_damage amp 2 (Harming III)

    @Override
    public int chakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int strainCost() {
        return STRAIN_COST;
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        Vec3 look = caster.getLookAngle();
        Vec3 center = caster.getEyePosition().add(look.scale(REACH)); // detonación en el punto de mira
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, center, RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 40, RADIUS / 2, 1, RADIUS / 2, 0.05);
    }
}
