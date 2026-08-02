package com.zamu.dojutsu.dojutsu.ability;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

/**
 * Utilidades compartidas por las habilidades oculares de manipulacion de velocidad (empuje/atraccion) y daño en
 * area, para no duplicar la iteracion de objetivos y el impulso. La <b>sincronizacion del impulso al cliente</b>
 * (necesaria para mover a jugadores desde el servidor) se hace con {@code hurtMarked}, que fuerza el envio de la
 * velocidad en el siguiente tick.
 */
final class EyeAbilityEffects {

    private EyeAbilityEffects() {
    }

    /** Entidades vivas alrededor de {@code center} (radio {@code radius}), excluyendo al dueno y armor stands. */
    static List<LivingEntity> livingAround(ServerLevel level, ServerPlayer owner, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                e -> e != owner && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand));
    }

    /**
     * Aplica un impulso de velocidad a una entidad y lo <b>sincroniza</b> (para que valga tambien sobre
     * jugadores, cuyo movimiento controla el cliente): {@code setDeltaMovement} + {@code hurtMarked}.
     */
    static void impulse(LivingEntity target, Vec3 velocity) {
        target.setDeltaMovement(velocity);
        target.hurtMarked = true; // fuerza el envio de la velocidad al cliente el proximo tick
    }

    /** Entidad viva más cercana a {@code center} dentro del radio (excluye dueno/armor stands), o vacio. */
    static Optional<LivingEntity> nearestLiving(ServerLevel level, ServerPlayer owner, Vec3 center, double radius) {
        return livingAround(level, owner, center, radius).stream()
                .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(center)));
    }

    /** Vector unitario horizontal (con una componente vertical fija) de {@code from} hacia {@code to}. */
    static Vec3 launchDirection(Vec3 from, Vec3 to, double horizontal, double vertical) {
        Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
        Vec3 dir = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 0) : flat.normalize();
        return new Vec3(dir.x * horizontal, vertical, dir.z * horizontal);
    }

    /** Daño mágico indirecto atribuido al dueno (mismo patron que las familias de efecto de jutsu). */
    static void damage(ServerLevel level, ServerPlayer owner, LivingEntity target, float amount) {
        target.hurt(level.damageSources().indirectMagic(owner, owner), amount);
    }
}
