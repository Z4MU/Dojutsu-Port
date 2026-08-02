package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.NinjaProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tengai Shinsei (Mangekyō de Madara): invoca un <b>meteorito</b> que cae del cielo sobre el punto de mira y
 * <b>explota</b> devastando la zona. La técnica más destructiva del roster de Mangekyō (lote 3). {@code [S05]},
 * {@code sharingan/mangekyo/madara/tengai_shinsei} → {@code summon_meteor} → {@code hit}.
 *
 * <p>Coste <b>200 chakra / +13 eye_strain</b> (verificado: {@code madara/cost}; exento con ojo eterno) y
 * <b>cooldown 200 ticks</b> ({@code mangekyo_time 200}). Es una habilidad de <b>un disparo</b>, así que usa el
 * pipeline estándar (coste/fatiga/cooldown por contrato, a diferencia de Kamui). El datapack lo hace caer un
 * marcador desde ~30 bloques y al impactar suelta varios {@code tnt_minecart} + {@code fill ... air destroy};
 * aquí es un {@link NinjaProjectile} que cae con gravedad y provoca una <b>explosión</b> con fuego y destrucción
 * de bloques al impactar. Ajuste de radio/daño → balance (F10).
 */
public final class TengaiShinseiAbility implements EyeAbility {

    private static final int CHAKRA_COST = 200;
    private static final int STRAIN_COST = 13;
    private static final int COOLDOWN_TICKS = 200; // mangekyo_time 200

    private static final double AIM_RANGE = 40.0;      // hasta donde se apunta el impacto
    private static final double METEOR_HEIGHT = 30.0;  // el meteoro nace ~30 bloques sobre el punto (summon_meteor)
    private static final int PROJECTILE_LIFETIME = 100;
    private static final float FALL_SPEED = 1.2f;
    private static final float EXPLOSION_RADIUS = 5.0f;

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
        Vec3 aim = caster.pick(AIM_RANGE, 1.0f, false).getLocation(); // a donde mira (raycast de bloque)
        Vec3 spawn = new Vec3(aim.x, aim.y + METEOR_HEIGHT, aim.z);    // el meteoro nace muy por encima

        NinjaProjectile meteor = NinjaProjectile.create(level, caster)
                .withGravity(0.05)
                .withLifetime(PROJECTILE_LIFETIME)
                .withFlightTick(TengaiShinseiAbility::trail)
                .withImpactCallback((projectile, hit) -> onImpact(caster, projectile, hit));
        meteor.setPos(spawn.x, spawn.y, spawn.z);
        meteor.launch(new Vec3(0, -1, 0), FALL_SPEED); // cae en picado
        level.addFreshEntity(meteor);

        // truenos anunciando la caída (summon_meteor)
        level.playSound(null, caster.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0f, 0.6f);
    }

    /** Estela de humo y piedra del meteoro mientras cae. */
    private static void trail(NinjaProjectile meteor) {
        if (meteor.level() instanceof ServerLevel level) {
            Vec3 pos = meteor.position();
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 6, 0.5, 0.5, 0.5, 0.02);
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 4, 0.4, 0.4, 0.4, 0.02);
        }
    }

    private static void onImpact(ServerPlayer caster, NinjaProjectile meteor, HitResult hit) {
        if (!(meteor.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = meteor.position();
        // gran explosion con fuego y destruccion de bloques (respeta mobGriefing via MOB), como los tnt del datapack
        level.explode(caster, pos.x, pos.y, pos.z, EXPLOSION_RADIUS, true, Level.ExplosionInteraction.MOB);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }
}
