package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.NinjaProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Kinrin Tensei Baku (Tenseigan): la esfera dorada que el usuario <b>canaliza</b> — avanza despacio y detona en
 * área. Es la primera habilidad <b>canalizada con auto-raíz</b> del catálogo: mientras el proyectil vuela, el
 * lanzador queda clavado en el sitio, y se libera al impactar. Clona la plantilla de {@link AmaterasuAbility}.
 * {@code [S05]}, {@code tenseigan/chakra_mode/kinrin_tensei_baku}.
 *
 * <p>Coste <b>400 chakra</b> (verificado: {@code kinrin_tensei_baku/cast} exige {@code d.chakra matches 400..} y
 * resta 400; el predicado {@code bb/costs/kinrin} bloquea por debajo de 400). Sin fatiga (el Tenseigan no añade
 * {@code eye_strain}). La <b>raíz</b> es fiel: {@code slowness 255} + {@code jump_boost 255} al lanzar, limpiados
 * al impactar. Al detonar aplica {@code instant_damage} amp 2 (Harming III, cura no-muertos vía
 * {@link MobEffects#HARM}) en {@code distance 0..5}. Ajuste de velocidad/daño → balance (F10).
 */
public final class KinrinTenseiBakuAbility implements EyeAbility {

    private static final int CHAKRA_COST = 400;
    private static final int STRAIN_COST = 0;    // el Tenseigan no añade eye_strain
    private static final int PROJECTILE_LIFETIME = 100; // acota tambien la raiz si nunca impacta
    private static final float PROJECTILE_SPEED = 0.6f; // lento: el datapack avanza a 0.2 por paso
    private static final double BLAST_RADIUS = 5.0;     // distance 0..5 del run
    private static final int HARM_AMPLIFIER = 2;        // instant_damage amp 2 (Harming III)
    private static final int ROOT_AMPLIFIER = 255;      // slowness/jump_boost amp 255 = clavado en el sitio

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
        root(caster);
        NinjaProjectile orb = NinjaProjectile.create(level, caster)
                .withGravity(0.0)
                .withLifetime(PROJECTILE_LIFETIME)
                .withImpactCallback((projectile, hit) -> onImpact(caster, projectile, hit));
        orb.launch(caster.getLookAngle(), PROJECTILE_SPEED);
        level.addFreshEntity(orb);
    }

    /** Clava al lanzador mientras canaliza (el datapack da slowness+jump_boost 255 al castear). */
    private static void root(ServerPlayer caster) {
        caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, PROJECTILE_LIFETIME, ROOT_AMPLIFIER));
        caster.addEffect(new MobEffectInstance(MobEffects.JUMP, PROJECTILE_LIFETIME, ROOT_AMPLIFIER));
    }

    private static void onImpact(ServerPlayer caster, NinjaProjectile projectile, HitResult hit) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        // el run limpia slowness/jump_boost del lanzador al detonar: la canalizacion termina
        caster.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        caster.removeEffect(MobEffects.JUMP);
        Vec3 pos = projectile.position();
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, pos, BLAST_RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 60, BLAST_RADIUS / 2, 1, BLAST_RADIUS / 2, 0.1);
        level.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }
}
