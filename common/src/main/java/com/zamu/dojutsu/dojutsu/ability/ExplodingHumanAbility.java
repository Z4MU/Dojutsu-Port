package com.zamu.dojutsu.dojutsu.ability;

import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Exploding Human Technique (Ketsuryugan): hace estallar la sangre de un objetivo con un daño devastador. Clona
 * la plantilla de {@link AmaterasuAbility}. {@code [S05]}, {@code ketsuryugan/exploding_human}.
 *
 * <p>Coste <b>200 chakra / +20 eye strain</b> (verificado: {@code exploding_human/cost}). El {@code hit_effect}
 * aplica {@code instant_damage} amp 2 (Harming III, cura no-muertos, via {@link MobEffects#HARM}). El datapack
 * marca hasta 3 objetivos y los detona; aquí se simplifica a una detonación inmediata sobre el más cercano
 * (ajuste/multi-objetivo → balance F10).
 */
public final class ExplodingHumanAbility implements EyeAbility {

    private static final int CHAKRA_COST = 200;
    private static final int STRAIN_COST = 20;
    private static final double RANGE = 10.0;
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
    public boolean canApply(EyeAbilityContext context) {
        return target(context).isPresent(); // "Nothing nearby" (no_cast_nby) si no hay a quien detonar
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerLevel level = context.level();
        target(context).ifPresent(target -> {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
            Vec3 pos = target.position();
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1, pos.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y + 1, pos.z, 40, 0.6, 0.6, 0.6, 0.2);
        });
    }

    /** Entidad mas cercana en rango (misma busqueda para {@link #canApply} y {@link #execute}, no divergen). */
    private static Optional<LivingEntity> target(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        return EyeAbilityEffects.nearestLiving(context.level(), caster, caster.position(), RANGE);
    }
}
