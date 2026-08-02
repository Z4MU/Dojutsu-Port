package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.AbilityAnchor;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Kotoamatsukami (Mangekyō de Shisui): el <b>genjutsu supremo</b> — domina la mente del objetivo, que queda
 * desorientado y <b>pierde el rumbo</b> durante mucho tiempo sin darse cuenta. Roster de Mangekyō, lote 5.
 * {@code [S05]}, {@code sharingan/mangekyo/shisui/afterglow} + {@code run_afterglow}.
 *
 * <p><b>El genjutsu más largo del catálogo</b> (500 ticks = 25 s, frente a los 100 de {@link TsukuyomiAbility}) y
 * el primero que <b>manipula la orientación</b> de la víctima: fiel a {@code run_afterglow}, cada
 * {@value #TURN_INTERVAL_TICKS} ticks le da media vuelta ({@code tp @s ~ ~ ~ ~180 ~}) — el "control mental" del
 * datapack, que hace perder el norte a quien lo sufre. Mientras dura: {@code slowness} amp 2, {@code blindness}
 * amp 1 y {@code mining_fatigue} amp 1, con partículas de tinta.
 *
 * <p>Coste <b>50 chakra / +15 eye_strain</b> (verificado: {@code afterglow}; exento con ojo eterno) y
 * <b>cooldown 500 ticks</b> ({@code mangekyo_time 500}) — el más largo del roster, acorde a su poder. El datapack
 * lo gatea con {@code no_cast_nby} (radio 10), así que implementa {@link #canApply} y <b>no cobra sin objetivo</b>.
 *
 * <p>El efecto persistente lo lleva un {@link AbilityAnchor} que <b>sigue a la víctima</b> (el ancla de F4/DJT-071,
 * con caducidad por construcción), no un armor stand colgado.
 */
public final class KotoamatsukamiAbility implements EyeAbility {

    private static final int CHAKRA_COST = 50;
    private static final int STRAIN_COST = 15;
    private static final int COOLDOWN_TICKS = 500; // mangekyo_time 500

    private static final double RANGE = 10.0;          // distance 0..10 del afterglow
    private static final int GENJUTSU_TICKS = 500;     // koto_agtimer 500
    private static final int TURN_INTERVAL_TICKS = 50; // koag_tptimer 50: media vuelta cada 50 ticks
    private static final int SLOWNESS_AMPLIFIER = 2;
    private static final int BLINDNESS_AMPLIFIER = 1;
    private static final int FATIGUE_AMPLIFIER = 1;

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
        return target(context).isPresent(); // "Nothing nearby" (no_cast_nby del afterglow)
    }

    @Override
    public void execute(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        target(context).ifPresent(victim -> {
            AbilityAnchor anchor = AbilityAnchor.create(level, caster, GENJUTSU_TICKS)
                    .withTickCallback(a -> dominate(a, victim));
            level.addFreshEntity(anchor);
        });
    }

    /** Entidad mas cercana en rango (misma busqueda para {@link #canApply} y {@link #execute}). */
    private static Optional<LivingEntity> target(EyeAbilityContext context) {
        ServerPlayer caster = context.caster();
        return EyeAbilityEffects.nearestLiving(context.level(), caster, caster.position(), RANGE);
    }

    /**
     * Mantiene el genjutsu sobre la victima cada tick: la sigue, refresca los debuffs y le da media vuelta cada
     * {@value #TURN_INTERVAL_TICKS} ticks. Si la victima muere o se va, descarta el ancla (nada colgado).
     */
    private static void dominate(AbilityAnchor anchor, LivingEntity victim) {
        if (!(anchor.level() instanceof ServerLevel level)) {
            return;
        }
        if (!victim.isAlive() || victim.isRemoved() || victim.level() != level) {
            anchor.discard();
            return;
        }
        Vec3 pos = victim.position();
        anchor.setPos(pos.x, pos.y, pos.z); // el ancla acompana a la victima
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, SLOWNESS_AMPLIFIER, true, false));
        victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 3, BLINDNESS_AMPLIFIER, true, false));
        victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 2, FATIGUE_AMPLIFIER, true, false));
        level.sendParticles(ParticleTypes.SQUID_INK, pos.x, pos.y + 1.5, pos.z, 10, 0.5, 0.5, 0.5, 0);
        if (anchor.tickCount % TURN_INTERVAL_TICKS == 0) {
            spin(victim); // pierde el rumbo (tp ~ ~ ~ ~180 ~)
        }
    }

    /** Le da media vuelta a la victima, sin moverla (el {@code ~180} del datapack). */
    private static void spin(LivingEntity victim) {
        float turned = victim.getYRot() + 180.0f;
        victim.setYRot(turned);
        victim.setYHeadRot(turned);
        if (victim instanceof ServerPlayer player) {
            // a un jugador hay que empujarle la rotacion por red (el cliente manda su propia camara)
            player.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(),
                    java.util.Set.of(), turned, player.getXRot());
        }
    }
}
