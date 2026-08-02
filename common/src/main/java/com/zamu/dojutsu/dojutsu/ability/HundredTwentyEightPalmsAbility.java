package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Eight Trigrams 128 Palms (Byakugan): la versión superior de {@link SixtyFourPalmsAbility} — ráfaga de golpes en
 * corto alcance con daño mayor. {@code [S05]}, {@code byakugan/128_palms}.
 *
 * <p>Coste <b>80 chakra</b> (verificado: {@code 128_palms/cast}; sin fatiga, como el resto del Byakugan). Daña a
 * {@code distance 0..5} con {@code instant_damage} amp 2 (Harming III, frente al amp 1 de las 64 Palms; cura
 * no-muertos via {@link MobEffects#HARM}) y aplica la postura enraizada al lanzador. Ajuste → balance (F10).
 */
public final class HundredTwentyEightPalmsAbility implements EyeAbility {

    private static final int CHAKRA_COST = 80;
    private static final int STRAIN_COST = 0; // el Byakugan no añade eye_strain
    private static final double RADIUS = 5.0;
    private static final int STANCE_TICKS = 20;
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
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STANCE_TICKS, 4)); // postura enraizada
        Vec3 origin = caster.position();
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, origin, RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        level.sendParticles(ParticleTypes.CRIT, origin.x, origin.y + 1, origin.z, 100, RADIUS / 2, 1, RADIUS / 2, 0.4);
    }
}
