package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Tsukuyomi (Mangekyo Sharingan): genjutsu de <b>ilusión</b> sobre un objetivo — lo ciega, inmoviliza y daña.
 * Clona la plantilla de {@link AmaterasuAbility}; es la habilidad de <b>debuff/genjutsu</b> del catálogo (frente
 * al empuje, la atracción y el daño puro). {@code [S05]}, {@code sharingan/mangekyo/.../tsukuyomi}.
 *
 * <p>Coste <b>100 chakra / +30 eye strain</b> (verificado: {@code tsukuyomi/cost}; el más caro en fatiga, y
 * <b>exento con el ojo eterno</b> —lo aplica el pipeline—). Efectos del datapack: {@code slowness} (inmoviliza),
 * {@code blindness}, {@code mining_fatigue}, y {@code instant_damage} (los no-muertos se curan, via
 * {@link MobEffects#HARM}). Duraciones fijas de piloto; ajuste → balance (F10).
 */
public final class TsukuyomiAbility implements EyeAbility {

    private static final int CHAKRA_COST = 100;
    private static final int STRAIN_COST = 30;
    private static final double RANGE = 12.0;
    private static final int DURATION_TICKS = 60; // 3 s de genjutsu

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
        EyeAbilityEffects.nearestLiving(level, caster, caster.position(), RANGE).ifPresent(target -> {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION_TICKS, 4)); // inmoviliza
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION_TICKS, 0));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, DURATION_TICKS, 1)); // mining_fatigue
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0)); // vanilla cura a los no-muertos
            Vec3 pos = target.position();
            level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 1, pos.z, 40, 0.5, 0.8, 0.5, 0.05);
        });
    }
}
