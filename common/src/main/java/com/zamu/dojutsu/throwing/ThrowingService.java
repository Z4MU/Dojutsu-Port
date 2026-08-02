package com.zamu.dojutsu.throwing;

import com.zamu.dojutsu.domain.throwing.ThrowableType;
import com.zamu.dojutsu.domain.throwing.ThrowingTable;
import com.zamu.dojutsu.entity.NinjaProjectile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Lanzamiento de <b>armas arrojadizas</b> {@code [S16]} (F8/DJT-180): kunai, shuriken y compañia sobre el
 * {@link NinjaProjectile} de F4.
 *
 * <h2>El proyectil ya existia</h2>
 * El datapack invoca un <b>armor stand</b> por tiro y lo teletransporta {@code ^ ^ ^.5} cada tick, comprobando a
 * mano si el bloque de delante es solido en cuatro pasos. Eso es exactamente lo que hace {@code NinjaProjectile}
 * (F4/DJT-070) con fisica real, raycast e impacto — y ya lleva el <b>dueño por UUID</b> que pide el riesgo
 * <b>S16-R2</b>, en vez del {@code d.throw_ply}/{@code d.throw_as} pareado por scoreboard del original.
 *
 * <h2>Dos daños que no hay que confundir</h2>
 * El atributo del item (5 el kunai, 3 el shuriken, 4 el fuma) es para <b>blandirlo</b>. Lanzado, las cinco armas
 * hacen lo mismo: {@value ThrowingTable#THROWN_DAMAGE} puntos. Un fuma shuriken no pega mas por volar.
 *
 * <h2>Recuperarla es cosa de vanilla</h2>
 * El datapack tiene {@code throwing/drop}: un temporizador de {@value ThrowingTable#STUCK_LIFETIME_TICKS} ticks
 * y luego un {@code summon item} con el NBT del arma reconstruido a mano por cada tipo. Aqui el proyectil
 * simplemente <b>suelta su propio item</b> al clavarse; recogerlo es la mecanica de item de siempre, sin codigo.
 */
public final class ThrowingService {

    /** Veto entre lanzamientos por jugador ({@code d.thrown} + {@code schedule end}). */
    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    private ThrowingService() {
    }

    /**
     * Lanza un arma.
     *
     * @param stack el item lanzado (se usa para el render y para lo que se suelta al clavarse)
     * @return {@code true} si se lanzo (y por tanto hay que consumir el item)
     */
    public static boolean throwWeapon(ServerPlayer player, ThrowableType type, ItemStack stack) {
        UUID id = player.getUUID();
        if (COOLDOWNS.getOrDefault(id, 0) > 0) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        ItemStack display = stack.copyWithCount(1);
        NinjaProjectile projectile = NinjaProjectile.create(level, player)
                .withDisplayItem(display)
                .withImpactCallback((thrown, hit) -> onImpact(thrown, hit, type, display, player));
        level.addFreshEntity(projectile);
        projectile.launch(player.getLookAngle(), type.speed());
        level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5f, 1.2f);
        COOLDOWNS.put(id, type.throwCooldown());
        return true;
    }

    /**
     * Impacto: daño en area, y despues lo que toque segun el arma — detonar, marcar o quedarse clavada y caer.
     *
     * <p>El {@code HARM} se <b>invierte solo</b> en los no-muertos, como la rama {@code #dojutsu:undead} del
     * datapack: la fidelidad sale gratis.
     */
    private static void onImpact(NinjaProjectile projectile, net.minecraft.world.phys.HitResult hit,
            ThrowableType type, ItemStack display, ServerPlayer thrower) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = hit.getLocation();
        damageNearby(level, at, thrower);

        if (type.explodes()) {
            level.explode(thrower, at.x, at.y, at.z, ThrowingTable.EXPLOSIVE_POWER, Level.ExplosionInteraction.MOB);
            return;
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.PLAYERS, 0.6f, 1f);
        if (type.isMark()) {
            FtgMarkerService.mark(thrower, level, at);
            return;
        }
        if (type.recoverable() && !(hit instanceof EntityHitResult)) {
            // el arma clavada cae al suelo como item: recogerla es la mecanica vanilla, sin codigo propio
            level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
                    level, at.x, at.y + 0.5, at.z, display.copy()));
        }
    }

    /** Daño en area del impacto ({@code move}: radio 3, sin tocar al que lanzo). */
    private static void damageNearby(ServerLevel level, Vec3 at, Entity thrower) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(at, at).inflate(ThrowingTable.HIT_RADIUS))) {
            if (victim != thrower) {
                victim.addEffect(new MobEffectInstance(MobEffects.HARM, 1, ThrowingTable.THROWN_DAMAGE_AMPLIFIER));
            }
        }
    }

    /** Tickea el veto de lanzamiento. La llama {@code FtgMarkerService}, que ya recorre a los jugadores. */
    static void tickCooldown(UUID playerId) {
        int value = COOLDOWNS.getOrDefault(playerId, 0);
        if (value > 0) {
            COOLDOWNS.put(playerId, value - 1);
        }
    }

    /** Olvida el veto (al desconectar). */
    public static void forget(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }
}
