package com.zamu.dojutsu.dojutsu.ability;

import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Genjutsu Stun (Sharingan): la mirada que <b>paraliza</b> — el objetivo queda clavado en el sitio unos segundos.
 * Es la <b>primera habilidad del Sharingan</b> del catálogo y el primer <b>stun duro</b> (frente al genjutsu de
 * daño de {@link TsukuyomiAbility}). Clona la plantilla de {@link AmaterasuAbility}. {@code [S05]},
 * {@code sharingan/stun}.
 *
 * <p><b>No cuesta chakra</b>: verificado — el {@code sharingan/cost.mcfunction} es el goteo <i>pasivo</i> del ojo
 * activo, no el coste de esta habilidad, y {@code stun/cast} no cobra nada. El datapack la gatea <b>solo por
 * cooldown</b> ({@code d.sharstun_cd 200}), de ahí {@link #cooldownTicks()}. Sin fatiga (el Sharingan base no
 * añade {@code eye_strain}).
 *
 * <p>Efectos sobre el objetivo, 100 ticks (5 s) como {@code stun/run}: {@code slowness} amp 255 (inmovilización
 * total) + {@code mining_fatigue} amp 3; la {@code blindness} solo a <b>no jugadores</b>, fiel al
 * {@code unless predicate dojutsu:player/is_player} de {@code stun/main}. Alcance 10 (el {@code distance=0..10}
 * del {@code cast}). Ajuste → balance (F10).
 */
public final class GenjutsuStunAbility implements EyeAbility {

    private static final int CHAKRA_COST = 0;   // el datapack no cobra chakra: gatea por cooldown
    private static final int STRAIN_COST = 0;   // el Sharingan base no añade eye_strain
    private static final int COOLDOWN_TICKS = 200; // d.sharstun_cd 200
    private static final double REACH = 10.0;      // distance=0..10 del cast
    private static final int STUN_TICKS = 100;     // d.sharstun_time 100
    private static final int ROOT_AMPLIFIER = 255; // slowness amp 255 = inmovilizado
    private static final int FATIGUE_AMPLIFIER = 3;
    private static final int BLINDNESS_AMPLIFIER = 1;

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
    public boolean canApply(EyeAbilityContext context) {
        return target(context).isPresent(); // "Nothing nearby" (no_cast_nby del sharingan/stun)
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerLevel level = context.level();
        target(context).ifPresent(target -> stun(level, target));
    }

    /** Entidad mas cercana en el cono de mira (misma busqueda para {@link #canApply} y {@link #execute}). */
    private static Optional<LivingEntity> target(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        Vec3 eyes = caster.getEyePosition();
        return EyeAbilityEffects.nearestLiving(
                context.level(), caster, eyes.add(caster.getLookAngle().scale(REACH / 2)), REACH);
    }

    private static void stun(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, STUN_TICKS, ROOT_AMPLIFIER));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, STUN_TICKS, FATIGUE_AMPLIFIER));
        if (!(target instanceof Player)) {
            // stun/main ciega solo a los no jugadores (a un jugador la pantalla negra sería desproporcionada)
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, STUN_TICKS, BLINDNESS_AMPLIFIER));
        }
        Vec3 pos = target.position();
        level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.5, pos.z, 10, 0.5, 0.5, 0.5, 0);
    }
}
