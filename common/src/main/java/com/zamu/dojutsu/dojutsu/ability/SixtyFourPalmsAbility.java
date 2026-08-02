package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Eight Trigrams 64 Palms (Byakugan): ráfaga de golpes de puño suave en <b>corto alcance</b> — daña a todas las
 * entidades alrededor. Clona la plantilla de {@link AmaterasuAbility}. {@code [S05]}, {@code byakugan/64_palms}.
 *
 * <p>Coste <b>60 chakra</b> (verificado: {@code 64_palms/cast}; sin fatiga, como el resto del Byakugan). El
 * datapack aplica al lanzador {@code slowness}/{@code jump_boost} amp 255 (la postura enraizada) y daña a las
 * entidades a {@code distance 0..5} con {@code instant_damage} (cura no-muertos). Aquí: breve auto-inmovilización
 * de la postura + {@link MobEffects#HARM} en el radio. Ajuste → balance (F10).
 */
public final class SixtyFourPalmsAbility implements EyeAbility {

    private static final int CHAKRA_COST = 60;
    private static final int STRAIN_COST = 0; // el Byakugan no añade eye_strain
    private static final double RADIUS = 5.0;   // distance 0..5 del datapack
    private static final int STANCE_TICKS = 20;  // 1 s de postura enraizada (slowness)
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
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STANCE_TICKS, 4)); // postura enraizada
        Vec3 origin = caster.position();
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, origin, RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        level.sendParticles(ParticleTypes.CRIT, origin.x, origin.y + 1, origin.z, 60, RADIUS / 2, 1, RADIUS / 2, 0.3);
    }
}
