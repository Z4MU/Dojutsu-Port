package com.zamu.dojutsu.dojutsu.ability;

import com.zamu.dojutsu.entity.AbilityAnchor;
import com.zamu.dojutsu.entity.NinjaProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Amaterasu: Flame Control (Mangekyō de Sasuke): el <b>dominio de la llama negra</b> — la habilidad que define a
 * Sasuke frente a Itachi: no solo puede <b>encender</b> el Amaterasu, sino <b>apagarlo</b> a voluntad. Cierra el
 * roster de los 8 Mangekyō por personaje (lote 7). {@code [S05]},
 * {@code sharingan/mangekyo/sasuke/extinguish/cast} → {@code summon} → {@code run}.
 *
 * <p><b>La primera habilidad "de utilidad"</b> del catálogo: no daña ni desplaza — <b>limpia</b>. Un proyectil
 * recorre la zona apagando, en radio {@value #EXTINGUISH_RADIUS}: las <b>entidades ardiendo</b>
 * ({@code d.amat_hit} del datapack → aquí {@code setRemainingFireTicks(0)}), las <b>anclas de Amaterasu</b> del
 * lanzador ({@link AbilityAnchor}, la llama negra persistente de {@link AmaterasuAbility}) y los <b>bloques de
 * fuego</b> del terreno.
 *
 * <p>Coste <b>20 chakra / +10 eye_strain</b> (verificado: {@code extinguish/cost}; exento con ojo eterno) y
 * <b>cooldown 100 ticks</b> ({@code mangekyo_time 100}).
 *
 * <p><b>Divergencia consciente (regla 16):</b> en el datapack el coste solo se cobra <b>si el proyectil acierta</b>
 * a algo ardiendo (el {@code cost} corre desde el {@code run}, no desde el {@code cast}); aquí se cobra por el
 * pipeline al lanzar, como el resto del catálogo. Modelar el cobro diferido exigiría que el pipeline soportara
 * "cobrar más tarde", que hoy no existe — se anota como posible mejora.
 */
public final class FlameControlAbility implements EyeAbility {

    private static final int CHAKRA_COST = 20;
    private static final int STRAIN_COST = 10;
    private static final int COOLDOWN_TICKS = 100; // mangekyo_time 100

    private static final int PROJECTILE_LIFETIME = 40;
    private static final float PROJECTILE_SPEED = 1.5f;
    private static final double EXTINGUISH_RADIUS = 3.0; // distance 0..3 del run

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
        NinjaProjectile wave = NinjaProjectile.create(level, caster)
                .withGravity(0.0)
                .withLifetime(PROJECTILE_LIFETIME)
                .withFlightTick(projectile -> extinguish(caster, projectile));
        wave.launch(caster.getLookAngle(), PROJECTILE_SPEED);
        level.addFreshEntity(wave);
        level.playSound(null, caster.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 0.6f);
    }

    /** Cada tick de vuelo: apaga entidades ardiendo, anclas de Amaterasu y bloques de fuego alrededor. */
    private static void extinguish(ServerPlayer caster, NinjaProjectile wave) {
        if (!(wave.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 pos = wave.position();
        AABB area = new AABB(pos, pos).inflate(EXTINGUISH_RADIUS);

        // 1) entidades ardiendo (el d.amat_hit del datapack)
        for (LivingEntity burning : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.getRemainingFireTicks() > 0)) {
            burning.setRemainingFireTicks(0);
        }
        // 2) las anclas de Amaterasu del lanzador (la llama negra persistente que el propio Sasuke domina)
        for (AbilityAnchor anchor : level.getEntitiesOfClass(AbilityAnchor.class, area,
                a -> a.ownerUuid().filter(caster.getUUID()::equals).isPresent())) {
            anchor.discard();
        }
        // 3) el fuego del terreno
        clearFireBlocks(level, pos);

        level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 12, 0.6, 0.6, 0.6, 0.02);
    }

    /** Retira los bloques de fuego en el cubo alrededor del proyectil. */
    private static void clearFireBlocks(ServerLevel level, Vec3 center) {
        int radius = (int) EXTINGUISH_RADIUS;
        BlockPos origin = BlockPos.containing(center.x, center.y, center.z);
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(Blocks.FIRE) || level.getBlockState(pos).is(Blocks.SOUL_FIRE)) {
                level.removeBlock(pos, false);
            }
        }
    }
}
