package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Preta Path (Rinnegan): la barrera de <b>absorción</b> — envuelve al lanzador en una defensa que absorbe el
 * daño. Es la habilidad <b>defensiva</b> del catálogo (frente al daño/empuje/proyectil). Clona la plantilla de
 * {@link AmaterasuAbility}. {@code [S05]}, {@code rinnegan/six_paths/preta}.
 *
 * <p>Coste <b>150 chakra</b> (verificado: {@code preta/cast}; sin fatiga, como el resto del Rinnegan). El
 * datapack absorbe/drena chakra ajeno; aquí se reinterpreta como un buff de {@link MobEffects#ABSORPTION} +
 * {@link MobEffects#DAMAGE_RESISTANCE} sobre el lanzador (la barrera que absorbe). Ajuste → balance (F10).
 */
public final class PretaPathAbility implements EyeAbility {

    private static final int CHAKRA_COST = 150;
    private static final int STRAIN_COST = 0; // el Rinnegan no añade eye_strain
    private static final int DURATION_TICKS = 200; // 10 s de barrera

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
        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, DURATION_TICKS, 2));       // corazones extra
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_TICKS, 1)); // absorbe daño
        Vec3 pos = caster.position();
        level.sendParticles(ParticleTypes.ENCHANT, pos.x, pos.y + 1, pos.z, 40, 0.6, 1, 0.6, 0.2);
    }
}
