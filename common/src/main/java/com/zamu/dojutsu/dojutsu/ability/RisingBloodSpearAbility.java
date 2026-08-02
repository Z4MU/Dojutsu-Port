package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Rising Blood Spear (Ketsuryugan): una lanza de sangre a donde apunta el lanzador — daño mágico (HARM) en área.
 * Fiel a {@code ketsuryugan/rising_blood_spear} ({@code anchored eyes → summon}; los objetivos a {@code
 * distance 0..5} reciben {@code instant_damage}; los no-muertos {@code instant_health}). Clona la plantilla.
 *
 * <p>Coste <b>100 chakra / +14 eye strain</b> (verificado: {@code rising_blood_spear/cost}). Usa
 * {@link MobEffects#HARM} (amp 0, Harming I), que vanilla invierte para los no-muertos.
 */
public final class RisingBloodSpearAbility implements EyeAbility {

    private static final int CHAKRA_COST = 100;
    private static final int STRAIN_COST = 14;
    private static final double REACH = 6.0;
    private static final double RADIUS = 5.0;    // distance 0..5 del datapack
    private static final int HARM_AMPLIFIER = 0; // instant_damage amp 0 (Harming I)

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
        Vec3 center = caster.getEyePosition().add(look.scale(REACH)); // punto de impacto frente al lanzador
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, center, RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, center.x, center.y, center.z, 40, RADIUS / 2, 1, RADIUS / 2, 0.1);
    }
}
