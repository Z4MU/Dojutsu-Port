package com.zamu.dojutsu.dojutsu.ability;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Bansho Ten'in / "Universal Pull" (Rinnegan): atracción en área — <b>tira</b> de las entidades cercanas hacia
 * el lanzador. Complemento de {@link ShinraTenseiAbility}; clona la plantilla de {@link AmaterasuAbility}.
 * {@code [S05]}, {@code rinnegan/bansho_tenin}.
 *
 * <p>Coste <b>360 chakra</b> (verificado: {@code bansho_tenin/cast} {@code d.chakra matches 360..}; el datapack
 * atrae a las entidades a {@code distance 0..5}). Sin daño (solo reposiciona), fiel al efecto de atracción.
 */
public final class BanshoTeninAbility implements EyeAbility {

    private static final int CHAKRA_COST = 360;
    private static final int STRAIN_COST = 0; // el datapack no añade eye_strain en Bansho Ten'in
    private static final double RADIUS = 5.0; // distance 0..5 del datapack
    private static final double PULL_HORIZONTAL = 1.2;
    private static final double PULL_VERTICAL = 0.25;

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
        Vec3 origin = caster.position();
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, origin, RADIUS)) {
            // atracción: desde la entidad hacia el lanzador (invierte el origen y el destino)
            Vec3 pull = EyeAbilityEffects.launchDirection(target.position(), origin, PULL_HORIZONTAL, PULL_VERTICAL);
            EyeAbilityEffects.impulse(target, pull);
        }
        level.sendParticles(ParticleTypes.PORTAL, origin.x, origin.y + 1, origin.z, 20, 1.5, 1, 1.5, 0.2);
    }
}
