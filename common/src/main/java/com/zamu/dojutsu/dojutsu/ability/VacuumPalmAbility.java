package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Eight Trigrams Vacuum Palm (Byakugan): palma de aire a distancia — <b>empuja y daña</b> a las entidades en la
 * dirección a la que apunta el lanzador. Clona la plantilla de {@link AmaterasuAbility} para un empuje
 * <b>dirigido</b> (frente a la repulsión radial de {@link ShinraTenseiAbility}). {@code [S05]},
 * {@code byakugan/vacuum_palm}.
 *
 * <p>Coste <b>40 chakra</b> (verificado: {@code vacuum_palm/cast} {@code d.chakra matches 40..}). Ajuste de
 * fuerza/daño/alcance → balance (F10).
 */
public final class VacuumPalmAbility implements EyeAbility {

    private static final int CHAKRA_COST = 40;
    private static final int STRAIN_COST = 0; // el datapack no añade eye_strain en Vacuum Palm
    private static final double REACH = 8.0;
    private static final double RADIUS = 2.5;
    private static final double PUSH_HORIZONTAL = 1.4;
    private static final double PUSH_VERTICAL = 0.3;
    private static final float DAMAGE = 5.0f;

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
        Vec3 center = caster.getEyePosition().add(look.scale(REACH)); // punto frente al lanzador
        Vec3 push = new Vec3(look.x * PUSH_HORIZONTAL, PUSH_VERTICAL, look.z * PUSH_HORIZONTAL);
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, center, RADIUS)) {
            EyeAbilityEffects.impulse(target, push); // empuje en la dirección de apuntado
            EyeAbilityEffects.damage(level, caster, target, DAMAGE);
        }
        for (double d = 1; d < REACH; d += 1.0) { // estela de la palma
            Vec3 p = caster.getEyePosition().add(look.scale(d));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }
}
