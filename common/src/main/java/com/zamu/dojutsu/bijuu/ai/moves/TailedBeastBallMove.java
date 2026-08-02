package com.zamu.dojutsu.bijuu.ai.moves;

import com.zamu.dojutsu.bijuu.ai.BeastMove;
import com.zamu.dojutsu.bijuu.ai.BeastMoveContext;
import com.zamu.dojutsu.domain.bijuu.BeastAiPlan;
import com.zamu.dojutsu.entity.NinjaProjectile;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * <b>Bola Bestia con Cola</b> {@code [S09]} (F7/DJT-161): la tecnica emblematica de las Bestias con Cola, y la
 * <b>primera prueba real</b> de que {@link BeastMove} resuelve la dualidad {@code [S09-R2]} — la misma instancia
 * la lanzara la IA (hoy) y el jinchuriki transformado (DJT-166), sin una segunda copia.
 *
 * <h2>Aqui es donde el datapack se habia partido en dos</h2>
 * Existian dos versiones que <b>ya habian divergido</b>:
 * <ul>
 *   <li>{@code tailed_beasts/ai/moves/tailed_beast_ball/}: vive {@value #LIFETIME_TICKS} ticks, hace daño en area
 *       y sabe que el Camino Preta puede absorberla.</li>
 *   <li>{@code jinchuriki/form/<bestia>/tailed_beast_ball/}: vive <b>1200</b> ticks, no hace el daño en area y no
 *       sabe nada de absorcion.</li>
 * </ul>
 * Se toma la version de la IA como la buena y se documenta la eleccion: 1200 ticks a
 * {@value #SPEED_BLOCKS_PER_TICK} bloques/tick son mas de 2600 bloques de alcance, que no es una decision de
 * diseño sino un copy-paste de un efecto mucho mas lento. Con {@value #LIFETIME_TICKS} el alcance queda en ~220
 * bloques, coherente con una bola de chakra.
 *
 * <h2>Numeros verificados</h2>
 * Se mueve en once pasos de 0,2 bloques por tick ({@code move.mcfunction}) = {@value #SPEED_BLOCKS_PER_TICK}
 * bloques/tick, pero <b>no arranca hasta el cuarto tick</b> (las tags {@code d.hit1/2/3} son una cuenta atras de
 * tres). Al impactar reparte {@code instant_damage} amplificador {@value #DAMAGE_AMPLIFIER} en
 * {@value #BLAST_RADIUS} bloques —que son 24 puntos, doce corazones— y detona. Como en todo el proyecto, el
 * {@code HARM} se <b>invierte solo</b> en los no-muertos: la fidelidad sale gratis, sin una rama.
 *
 * <p><b>Pendiente anotado:</b> la absorcion por el Camino Preta ({@code d.absorbed}) necesita que el proyectil
 * exponga un gancho de absorcion; se hara cuando se toque {@code PretaPathAbility}, no aqui.
 */
public final class TailedBeastBallMove implements BeastMove {

    /** Ticks que vive la bola ({@code d.tb_tbbtimer = 100}). */
    public static final int LIFETIME_TICKS = 100;

    /** Bloques por tick: once pasos de 0,2 en {@code move.mcfunction}. */
    public static final float SPEED_BLOCKS_PER_TICK = 2.2F;

    /** Ticks de carga antes de salir disparada (las tags {@code d.hit1/2/3}). */
    public static final int CHARGE_TICKS = 3;

    /** Amplificador del daño instantaneo al detonar (24 puntos de daño). */
    public static final int DAMAGE_AMPLIFIER = 3;

    /** Radio del daño en area ({@code @e[distance=0..5]}). */
    public static final double BLAST_RADIUS = 5.0;

    /** Potencia de la explosion: aproxima los tres minecarts de TNT apilados del datapack. */
    public static final float EXPLOSION_POWER = 6.0F;

    /** Chakra que cuesta a un <b>jinchuriki</b> ({@code form/.../cast}: {@code chakra -= 100}). */
    public static final int CHAKRA_COST = 100;

    /** Ticks de reutilizacion para el jinchuriki ({@code d.jinform_timer = 200}). */
    public static final int COOLDOWN_TICKS = 200;

    /** Bloques por delante del lanzador donde nace la bola ({@code positioned ^ ^1 ^3} + el tp de 1,5). */
    private static final double SPAWN_FORWARD = 3.0;

    @Override
    public String id() {
        return BeastAiPlan.TAILED_BEAST_BALL;
    }

    @Override
    public int chakraCost() {
        return CHAKRA_COST;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public void execute(BeastMoveContext context) {
        LivingEntity caster = context.caster();
        ServerLevel level = context.level();
        Vec3 look = caster.getLookAngle();
        Vec3 spawn = caster.getEyePosition().add(look.scale(SPAWN_FORWARD));

        AtomicInteger charge = new AtomicInteger(CHARGE_TICKS);
        NinjaProjectile ball = NinjaProjectile.create(level, caster)
                .withLifetime(LIFETIME_TICKS)
                .withDisplayItem(new ItemStack(Items.MAGMA_CREAM))
                .withFlightTick(projectile -> {
                    if (charge.get() > 0) {
                        // cuenta atras d.hit1/2/3: la bola se forma quieta antes de salir
                        charge.decrementAndGet();
                        projectile.setDeltaMovement(Vec3.ZERO);
                    }
                    aura(projectile);
                })
                .withImpactCallback((projectile, hit) -> detonate(projectile.level(), projectile.position(), caster));
        ball.setPos(spawn.x, spawn.y, spawn.z);
        level.addFreshEntity(ball);
        ball.launch(look, SPEED_BLOCKS_PER_TICK);

        level.playSound(null, caster.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 4F, 0.4F);
    }

    /** Estela de la bola durante el vuelo. */
    private static void aura(NinjaProjectile projectile) {
        if (projectile.level() instanceof ServerLevel level) {
            Vec3 pos = projectile.position();
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 6, 0.3, 0.3, 0.3, 0.01);
        }
    }

    /**
     * Detonacion: daño en area a todo lo cercano <b>menos al lanzador</b>, mas la explosion. El {@code HARM} se
     * invierte solo en los no-muertos (los cura), como hace el datapack con su rama {@code #dojutsu:undead}.
     */
    private static void detonate(Level level, Vec3 at, LivingEntity caster) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(BLAST_RADIUS))) {
            if (victim != caster) {
                victim.addEffect(new MobEffectInstance(MobEffects.HARM, 1, DAMAGE_AMPLIFIER));
            }
        }
        serverLevel.explode(caster, at.x, at.y, at.z, EXPLOSION_POWER, Level.ExplosionInteraction.MOB);
    }
}
