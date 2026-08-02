package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.NinjaProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Hinokagutsuchi (Mangekyō de Indra): la <b>llama negra viviente</b> — una esfera de fuego que avanza dejando un
 * rastro de incendio, <b>quema y daña todo a su paso</b>, y detona al impactar. Roster de Mangekyō, lote 4.
 * {@code [S05]}, {@code sharingan/mangekyo/indra/hinokagutsuchi} → {@code summon} → {@code run}.
 *
 * <p><b>Primer proyectil con aura persistente</b> del catálogo: los anteriores ({@code AsuraPath},
 * {@code KinrinTenseiBaku}, {@code TengaiShinsei}) solo actuaban al impactar; este castiga <b>cada tick de vuelo</b>
 * en un radio de 8 ({@code run}: {@code Fire 400} + {@code instant_damage} amp 1, que cura no-muertos vía
 * {@link MobEffects#HARM}) y prende el suelo que sobrevuela.
 *
 * <p>Coste <b>20 chakra / +5 eye_strain</b> (verificado: {@code hinokagutsuchi}; exento con ojo eterno) y
 * <b>cooldown 200 ticks</b> ({@code mangekyo_time 200}). Fiel al datapack, el lanzador recibe
 * {@code resistance} amp 255 un instante al lanzarlo (queda a salvo de su propia llama). Ajuste de radio/daño →
 * balance (F10).
 */
public final class HinokagutsuchiAbility implements EyeAbility {

    private static final int CHAKRA_COST = 20;
    private static final int STRAIN_COST = 5;
    private static final int COOLDOWN_TICKS = 200; // mangekyo_time 200

    private static final int PROJECTILE_LIFETIME = 60;
    private static final float PROJECTILE_SPEED = 0.8f; // avanza despacio (el datapack va a 0.1 por paso)
    private static final double AURA_RADIUS = 8.0;      // distance 0..8 del run
    private static final int FIRE_TICKS = 400;          // Fire set value 400
    private static final int HARM_AMPLIFIER = 1;        // instant_damage amp 1
    private static final int SELF_RESISTANCE_TICKS = 40;
    private static final float IMPACT_EXPLOSION = 3.0f;

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
        // el datapack protege al lanzador de su propia llama (resistance 255 mientras la conduce)
        caster.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, SELF_RESISTANCE_TICKS, 255, true, false));

        NinjaProjectile flame = NinjaProjectile.create(level, caster)
                .withGravity(0.0)
                .withLifetime(PROJECTILE_LIFETIME)
                .withFlightTick(projectile -> scorch(caster, projectile))
                .withImpactCallback((projectile, hit) -> onImpact(caster, projectile, hit));
        flame.launch(caster.getLookAngle(), PROJECTILE_SPEED);
        level.addFreshEntity(flame);
    }

    /** Cada tick de vuelo: quema y daña alrededor, y prende el suelo (el bucle {@code run} del datapack). */
    private static void scorch(ServerPlayer caster, NinjaProjectile flame) {
        if (!(flame.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = flame.position();
        for (LivingEntity target : EyeAbilityEffects.livingAround(level, caster, pos, AURA_RADIUS)) {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), FIRE_TICKS));
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, HARM_AMPLIFIER)); // vanilla cura no-muertos
        }
        // llama negra: humo denso en vez de fuego naranja
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 30, 0.8, 0.8, 0.8, 0.02);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 10, 0.5, 0.5, 0.5, 0.01);
        ignite(level, pos);
    }

    /** Prende el bloque de aire bajo la llama, si el suelo lo sostiene ({@code setblock ~ ~ ~ fire}). */
    private static void ignite(ServerLevel level, Vec3 pos) {
        BlockPos at = BlockPos.containing(pos.x, pos.y, pos.z);
        if (level.isEmptyBlock(at) && Blocks.FIRE.defaultBlockState().canSurvive(level, at)) {
            level.setBlockAndUpdate(at, Blocks.FIRE.defaultBlockState());
        }
    }

    private static void onImpact(ServerPlayer caster, NinjaProjectile flame, HitResult hit) {
        if (!(flame.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = flame.position();
        // detonacion final (los tnt_minecart del run del datapack), con fuego
        level.explode(caster, pos.x, pos.y, pos.z, IMPACT_EXPLOSION, true, Level.ExplosionInteraction.MOB);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }
}
