package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.NinjaProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Asura Path (Rinnegan): dispara un <b>misil</b> desde el brazo mecanizado. Es la habilidad de <b>proyectil</b>
 * del catálogo (frente al empuje/atracción/anclas/genjutsu): lanza un {@link NinjaProjectile} real que explota y
 * daña al impactar. Clona la plantilla de {@link AmaterasuAbility}. {@code [S05]},
 * {@code rinnegan/six_paths/asura}.
 *
 * <p>Coste <b>450 chakra</b> (verificado: {@code asura/cast}; sin fatiga, como el resto del Rinnegan). El datapack
 * da al lanzador {@code resistance} y lanza el misil como armor stand; aquí es el {@link NinjaProjectile} de F4
 * (DJT-070) con callback de impacto. Ajuste de daño/velocidad → balance (F10).
 */
public final class AsuraPathAbility implements EyeAbility {

    private static final int CHAKRA_COST = 450;
    private static final int STRAIN_COST = 0; // el Rinnegan no añade eye_strain
    private static final int PROJECTILE_LIFETIME = 40;
    private static final float PROJECTILE_SPEED = 2.2f;
    private static final float IMPACT_DAMAGE = 8.0f;
    private static final int RESISTANCE_TICKS = 40;

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
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, RESISTANCE_TICKS, 0)); // como el datapack
        NinjaProjectile missile = NinjaProjectile.create(level, caster)
                .withGravity(0.0)
                .withLifetime(PROJECTILE_LIFETIME)
                .withImpactCallback((projectile, hit) -> onImpact(caster, projectile, hit));
        missile.launch(caster.getLookAngle(), PROJECTILE_SPEED);
        level.addFreshEntity(missile);
    }

    private static void onImpact(ServerPlayer caster, NinjaProjectile projectile, net.minecraft.world.phys.HitResult hit) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = projectile.position();
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target
                && target != caster) {
            target.hurt(level.damageSources().indirectMagic(caster, caster), IMPACT_DAMAGE);
        }
    }
}
