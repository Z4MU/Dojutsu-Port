package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.NinjaProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Phase Shift (Mangekyō de Shin): el <b>salto dimensional corto</b> — una grieta de chakra avanza unos instantes
 * y el lanzador <b>reaparece donde esta</b>. Roster de Mangekyō, lote 6. {@code [S05]},
 * {@code sharingan/mangekyo/shin/teleport/cast} → {@code summon} → {@code main}.
 *
 * <p><b>Teleporte "guiado", no instantaneo:</b> a diferencia del blink de {@link AmenotejikaraAbility} (que salta
 * al punto de mira al instante) o del {@link KamuiAbility} (que vuelve a una marca previa), aqui vuela un
 * <b>marcador</b> durante {@value #TRAVEL_TICKS} ticks y el lanzador aparece <b>donde haya llegado</b> — si choca
 * antes, el salto es mas corto. Fiel a {@code teleport/main}: al llegar, {@code slow_falling} para no morir de
 * caida.
 *
 * <p>Coste <b>100 chakra / +8 eye_strain</b> (verificado: {@code teleport/cast}; exento con ojo eterno) y
 * <b>cooldown 200 ticks</b> ({@code shin_timer 200}).
 */
public final class PhaseShiftAbility implements EyeAbility {

    private static final int CHAKRA_COST = 100;
    private static final int STRAIN_COST = 8;
    private static final int COOLDOWN_TICKS = 200; // shin_timer 200

    /** Ticks que vuela el marcador antes de arrastrar al lanzador ({@code shin_tptimer 10}). */
    private static final int TRAVEL_TICKS = 10;
    private static final float PROJECTILE_SPEED = 2.0f;
    private static final int SLOW_FALLING_TICKS = 100; // slow_falling al reaparecer

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
        // Un solo disparo del salto, lo provoque el fin del vuelo o un choque antes de tiempo.
        java.util.concurrent.atomic.AtomicBoolean shifted = new java.util.concurrent.atomic.AtomicBoolean();
        NinjaProjectile rift = NinjaProjectile.create(level, caster)
                .withGravity(0.0)
                .withLifetime(TRAVEL_TICKS)
                .withFlightTick(projectile -> travel(caster, projectile, shifted))
                .withImpactCallback((projectile, hit) -> shiftOnce(caster, projectile.position(), shifted));
        rift.launch(caster.getLookAngle(), PROJECTILE_SPEED);
        level.addFreshEntity(rift);
    }

    /**
     * Estela mientras viaja y, en el <b>ultimo tick</b> de vuelo, arrastra al lanzador. Se hace aqui porque
     * {@link NinjaProjectile} solo descarta al expirar (no tiene callback de expiracion, a diferencia del ancla).
     */
    private static void travel(ServerPlayer caster, NinjaProjectile rift,
            java.util.concurrent.atomic.AtomicBoolean shifted) {
        if (!(rift.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = rift.position();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 12, 0.3, 0.3, 0.3, 0.05);
        if (rift.tickCount >= TRAVEL_TICKS - 1) {
            shiftOnce(caster, pos, shifted);
        }
    }

    /** Ejecuta el salto una sola vez (fin de vuelo o choque, lo que ocurra antes). */
    private static void shiftOnce(ServerPlayer caster, Vec3 destination,
            java.util.concurrent.atomic.AtomicBoolean shifted) {
        if (shifted.compareAndSet(false, true)) {
            shift(caster, destination);
        }
    }

    /** Reaparece al lanzador donde llego la grieta, con caida suave. */
    private static void shift(ServerPlayer caster, Vec3 destination) {
        if (!(caster.level() instanceof ServerLevel level) || caster.isRemoved()) {
            return;
        }
        poof(level, caster.position());
        caster.teleportTo(destination.x, destination.y, destination.z);
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS, 1, true, false));
        poof(level, destination);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private static void poof(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1, pos.z, 30, 0.4, 0.6, 0.4, 0.3);
    }
}
