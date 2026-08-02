package com.zamu.dojutsu.dojutsu.ability;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Shinra Tensei / "Almighty Push" (Rinnegan): repulsión en área — <b>empuja</b> a las entidades cercanas lejos
 * del lanzador y las daña. Clona la plantilla de {@link AmaterasuAbility} (DJT-118) para un efecto novedoso
 * (manipulación de velocidad). {@code [S05]}, {@code rinnegan/six_paths/deva} / {@code shinra_tensei}.
 *
 * <p>Coste <b>600 chakra</b> (verificado: {@code rinne_sharingan/shinra_tensei/cast} {@code d.chakra matches
 * 600..}). El datapack empuja con explosiones de {@code tnt_minecart}; aquí es un impulso de velocidad directo
 * (equivalente, sin destruir el terreno). Ajuste de fuerza/daño → balance (F10).
 */
public final class ShinraTenseiAbility implements EyeAbility {

    private static final int CHAKRA_COST = 600;
    private static final int STRAIN_COST = 0; // el datapack no añade eye_strain en el Deva/Shinra Tensei
    private static final double RADIUS = 6.0;
    private static final double PUSH_HORIZONTAL = 1.6;
    private static final double PUSH_VERTICAL = 0.5;
    private static final float DAMAGE = 4.0f;

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
        List<LivingEntity> targets = EyeAbilityEffects.livingAround(level, caster, origin, RADIUS);
        for (LivingEntity target : targets) {
            // empuje: desde el lanzador hacia la entidad (lejos)
            Vec3 push = EyeAbilityEffects.launchDirection(origin, target.position(), PUSH_HORIZONTAL, PUSH_VERTICAL);
            EyeAbilityEffects.impulse(target, push);
            EyeAbilityEffects.damage(level, caster, target, DAMAGE);
        }
        level.sendParticles(ParticleTypes.EXPLOSION, origin.x, origin.y + 1, origin.z, 8, RADIUS / 2, 1, RADIUS / 2, 0);
    }
}
