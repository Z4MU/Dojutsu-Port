package com.zamu.dojutsu.dojutsu.ability;

import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Amenotejikara (Rinnegan): <b>intercambio/teletransporte</b> instantáneo. Si el lanzador apunta a una entidad,
 * <b>intercambia su posición</b> con ella; si no, se teletransporta al punto al que mira (blink). Clona la
 * plantilla de {@link AmaterasuAbility}; es la habilidad de <b>teleport</b> del catálogo. {@code [S05]},
 * {@code rinnegan/amenotejikara}.
 *
 * <p>Coste <b>600 chakra</b> (verificado: {@code amenotejikara/cast}; sin fatiga, como el resto del Rinnegan).
 * El datapack usa un sistema de marcas/enlace para el swap; aquí es directo (equivalente). Ajuste → balance.
 */
public final class AmenotejikaraAbility implements EyeAbility {

    private static final int CHAKRA_COST = 600;
    private static final int STRAIN_COST = 0; // el Rinnegan no añade eye_strain
    private static final double AIM_RANGE = 20.0;
    private static final double SWAP_RADIUS = 2.5; // margen para "apuntar a" una entidad

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
        Vec3 aim = caster.pick(AIM_RANGE, 1.0f, false).getLocation();
        Vec3 casterPos = caster.position();
        Optional<LivingEntity> target = EyeAbilityEffects.nearestLiving(level, caster, aim, SWAP_RADIUS);
        if (target.isPresent()) {
            Vec3 targetPos = target.get().position();
            caster.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            target.get().teleportTo(casterPos.x, casterPos.y, casterPos.z); // intercambio
        } else {
            caster.teleportTo(aim.x, aim.y, aim.z); // blink al punto de mira
        }
        level.sendParticles(ParticleTypes.PORTAL, casterPos.x, casterPos.y + 1, casterPos.z, 30, 0.4, 0.6, 0.4, 0.4);
    }
}
