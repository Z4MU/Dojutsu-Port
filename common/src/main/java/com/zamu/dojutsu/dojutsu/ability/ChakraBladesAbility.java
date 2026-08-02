package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.NinjaProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Chakra Blades (Mangekyō de Shin): una <b>bandada de cuchillas</b> de chakra que vuela cortando todo lo que
 * encuentra a su paso. Roster de Mangekyō, lote 6. {@code [S05]},
 * {@code sharingan/mangekyo/shin/blades/cast} → {@code summon} → {@code run}.
 *
 * <p>Mismo esqueleto que {@link HinokagutsuchiAbility} (proyectil con aura persistente vía
 * {@code withFlightTick}), pero <b>corta en vez de quemar</b>: en {@code distance 0..5} aplica
 * {@code instant_damage} amp 0 cada tick de vuelo ({@link MobEffects#HARM}, que cura no-muertos), sin fuego ni
 * detonación final — es una habilidad de <b>hostigamiento rápido</b> (cd 60t, el más corto del roster) frente a
 * los ultimates de Madara/Indra.
 *
 * <p>Coste <b>20 chakra / +6 eye_strain</b> (verificado: {@code blades/cast}; exento con ojo eterno) y
 * <b>cooldown 60 ticks</b> ({@code mangekyo_time 60}).
 */
public final class ChakraBladesAbility implements EyeAbility {

    private static final int CHAKRA_COST = 20;
    private static final int STRAIN_COST = 6;
    private static final int COOLDOWN_TICKS = 60; // mangekyo_time 60

    private static final int PROJECTILE_LIFETIME = 40;
    private static final float PROJECTILE_SPEED = 1.6f;
    private static final double CUT_RADIUS = 5.0; // distance 0..5 del run
    private static final int HARM_AMPLIFIER = 0;  // instant_damage amp 0

    @Override
    public int chakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int strainCost() {
        return STRAIN_COST;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        NinjaProjectile blades = NinjaProjectile.create(level, caster)
                .withGravity(0.0)
                .withLifetime(PROJECTILE_LIFETIME)
                .withFlightTick(projectile -> cut(caster, projectile));
        blades.launch(caster.getLookAngle(), PROJECTILE_SPEED);
        level.addFreshEntity(blades);
    }

    /** Cada tick de vuelo: corta a las entidades cercanas (el bucle {@code run} del datapack). */
    private static void cut(ServerPlayer caster, NinjaProjectile blades) {
        if (!(blades.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = blades.position();
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, pos, CUT_RADIUS)) {
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        // destellos metalicos (el datapack usa particle item netherite_ingot)
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 8, 0.5, 0.2, 0.5, 0.05);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y, pos.z, 6, 0.4, 0.2, 0.4, 0.02);
    }
}
