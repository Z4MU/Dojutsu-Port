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
 * Blood Eruption (Ketsuryugan): hace estallar la sangre de la <b>entidad más cercana</b> — daño mágico (HARM).
 * Fiel a {@code ketsuryugan/blood_eruption} ({@code as @e ... distance=0..10 sort=nearest limit=1 → hit}: aplica
 * {@code instant_damage} amp 1; a los no-muertos les da {@code instant_health}). Clona la plantilla de DJT-118.
 *
 * <p>Coste <b>120 chakra / +8 eye strain</b> (verificado: {@code blood_eruption/cost}). Se usa el efecto vanilla
 * {@link MobEffects#HARM}, que <b>ya invierte</b> el signo para los no-muertos (los cura), reproduciendo el
 * comportamiento del datapack sin ramas.
 */
public final class BloodEruptionAbility implements EyeAbility {

    private static final int CHAKRA_COST = 120;
    private static final int STRAIN_COST = 8;
    private static final double RANGE = 10.0;   // distance 0..10 del datapack
    private static final int HARM_AMPLIFIER = 1; // instant_damage amp 1 (Harming II)

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
        return target(context).isPresent(); // "Nothing nearby" (no_cast_nby) si no hay a quien reventar
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerLevel level = context.level();
        target(context).ifPresent(target -> {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
            Vec3 pos = target.position();
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, pos.x, pos.y + 1, pos.z, 30, 0.5, 0.5, 0.5, 0.1);
        });
    }

    /** Entidad mas cercana en rango (misma busqueda para {@link #canApply} y {@link #execute}, no divergen). */
    private static Optional<LivingEntity> target(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        return EyeAbilityEffects.nearestLiving(context.level(), caster, caster.position(), RANGE);
    }
}
