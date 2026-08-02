package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.NinjaProjectile;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Beams of Light (Mangekyō de Shisui): el <b>rayo dirigido</b> que sale disparado hacia el objetivo cercano,
 * <b>lo clava en el sitio</b> y arrasa la zona a su paso. Segunda técnica de Shisui, junto a
 * {@link KotoamatsukamiAbility}. {@code [S05]}, {@code sharingan/mangekyo/shisui/beams_of_light} →
 * {@code summon_beam} → {@code run_beams_of_light} + {@code beam_hit}.
 *
 * <p><b>La habilidad más cara del roster:</b> <b>80 chakra / +20 eye_strain</b> (exento con ojo eterno) y
 * <b>cooldown 800 ticks</b> (40 s, {@code mangekyo_time 800}) — verificado en {@code beams_of_light}. Acorde a su
 * efecto: el objetivo queda <b>inmovilizado</b> ({@code beam_hit}: {@code slowness}/{@code jump_boost}/
 * {@code mining_fatigue} amp 128) mientras el rayo viaja hacia él dañando en {@code distance 0..8}.
 *
 * <p>Lleva {@code no_cast_nby}, así que implementa {@link #canApply} y <b>no cobra sin objetivo</b>.
 */
public final class BeamsOfLightAbility implements EyeAbility {

    private static final int CHAKRA_COST = 80;
    private static final int STRAIN_COST = 20;
    private static final int COOLDOWN_TICKS = 800; // mangekyo_time 800 (el mas largo del catalogo)

    private static final double RANGE = 10.0;      // distance 0..10 del cast
    private static final double BEAM_RADIUS = 8.0; // distance 0..8 del run
    private static final int HARM_AMPLIFIER = 1;   // instant_damage amp 1
    private static final int ROOT_TICKS = 60;      // el objetivo queda clavado mientras el rayo llega
    private static final int ROOT_AMPLIFIER = 128; // slowness/jump_boost/mining_fatigue amp 128 (beam_hit)
    private static final int PROJECTILE_LIFETIME = 60;
    private static final float PROJECTILE_SPEED = 1.4f;

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
        return target(context).isPresent(); // "Nothing nearby" (no_cast_nby del cast)
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        target(context).ifPresent(victim -> {
            root(victim); // beam_hit: lo clava en el sitio
            NinjaProjectile beam = NinjaProjectile.create(level, caster)
                    .withGravity(0.0)
                    .withLifetime(PROJECTILE_LIFETIME)
                    .withFlightTick(projectile -> sear(caster, projectile));
            // dirigido: sale hacia el objetivo, no a donde mira
            beam.launch(victim.position().add(0, victim.getBbHeight() / 2, 0)
                    .subtract(caster.getEyePosition()).normalize(), PROJECTILE_SPEED);
            level.addFreshEntity(beam);
        });
    }

    /** Entidad mas cercana en rango (misma busqueda para {@link #canApply} y {@link #execute}). */
    private static Optional<LivingEntity> target(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        return EyeAbilityEffects.nearestLiving(context.level(), caster, caster.position(), RANGE);
    }

    /** Clava al objetivo en el sitio ({@code beam_hit} del datapack). */
    private static void root(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ROOT_TICKS, ROOT_AMPLIFIER));
        victim.addEffect(new MobEffectInstance(MobEffects.JUMP, ROOT_TICKS, ROOT_AMPLIFIER));
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ROOT_TICKS, ROOT_AMPLIFIER));
    }

    /** Cada tick de vuelo: arrasa en radio 8 (el bucle {@code run_beams_of_light}). */
    private static void sear(ServerPlayer caster, NinjaProjectile beam) {
        if (!(beam.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = beam.position();
        for (LivingEntity victim : EyeAbilityEffects.livingAround(level, caster, pos, BEAM_RADIUS)) {
            victim.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        // haz de luz (el datapack usa particle item yellow_stained_glass)
        level.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 30, 0.25, 0.5, 0.25, 0.01);
        level.sendParticles(ParticleTypes.GLOW, pos.x, pos.y, pos.z, 10, 0.4, 0.4, 0.4, 0.02);
    }
}
