package com.zamu.dojutsu.jutsu.effect;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * Golpe cargado (F4/DJT-085d, {@code [S06]}): la familia de efecto <b>{@code primed_strike}</b> del rayo
 * (lightning_bolt, chidori, lightning_cutter, drilling_hand_of_sage_lightning). Castear <b>arma</b> el golpe
 * durante una ventana; el <b>siguiente golpe cuerpo a cuerpo</b> del caster descarga el efecto (daño + un rayo
 * visual) sobre el objetivo. Reproduce {@code d.lb_ready}/{@code d.chi_ready} + {@code hit_effect} del datapack.
 *
 * <p><b>Divergencia documentada:</b> el datapack consume el chakra <b>al golpear</b>; aqui el pipeline lo consume
 * <b>al castear</b> (armar). Armar y fallar el golpe gasta el chakra — divergencia menor, coherente con el
 * "consumir en cast" del {@code JutsuCastPipeline}.
 *
 * <p>Estado <b>transitorio</b> (no persiste): al desconectar se olvida ({@link #forget}); la ventana caduca sola
 * (expiracion perezosa). Es infraestructura de este efecto; se registra en el arranque.
 */
public final class PrimedStrikeManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Golpe armado de un caster: que jutsu, cuanto daño y hasta que tick de mundo es valido. */
    public record Primed(String jutsuId, float damage, long expiresAtTick) {

        public Primed {
            Objects.requireNonNull(jutsuId, "jutsuId");
        }

        /** Indica si el golpe sigue armado al tick dado (la ventana no expiro). */
        public boolean isLiveAt(long nowTick) {
            return nowTick < expiresAtTick;
        }
    }

    /** Golpes armados por caster (UUID -> Primed). Un cast nuevo reemplaza; se poda al golpear/expirar. */
    private static final Map<UUID, Primed> PRIMED = new ConcurrentHashMap<>();

    private PrimedStrikeManager() {
    }

    /** Suscribe el hook de golpe. Llamar una vez en el arranque. */
    public static void register() {
        EntityEvent.LIVING_HURT.register(PrimedStrikeManager::onLivingHurt);
    }

    /** Olvida el golpe armado de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        PRIMED.remove(playerId);
    }

    /** Arma un golpe cargado para el caster durante {@code windowTicks} desde ahora. */
    public static void arm(ServerPlayer caster, String jutsuId, float damage, int windowTicks) {
        long expiresAt = caster.serverLevel().getGameTime() + Math.max(1, windowTicks);
        PRIMED.put(caster.getUUID(), new Primed(jutsuId, damage, expiresAt));
    }

    private static EventResult onLivingHurt(LivingEntity target, DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ServerPlayer attacker && attacker != target) {
            discharge(attacker, target);
        }
        return EventResult.pass(); // no cancelamos el golpe normal: el rayo se suma
    }

    private static void discharge(ServerPlayer attacker, LivingEntity target) {
        Primed primed = PRIMED.get(attacker.getUUID());
        if (primed == null) {
            return;
        }
        long now = attacker.serverLevel().getGameTime();
        if (!primed.isLiveAt(now)) {
            PRIMED.remove(attacker.getUUID()); // ventana expirada: se descarta (sin efecto)
            return;
        }
        PRIMED.remove(attacker.getUUID()); // un golpe, una descarga
        // Rayo visual sobre el objetivo + daño configurado (mismo dueño que el golpe, no daño de entorno).
        ServerLevel level = attacker.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(target.getX(), target.getY(), target.getZ());
            bolt.setVisualOnly(true); // el daño lo aplicamos nosotros (configurable por jutsu)
            bolt.setCause(attacker);
            level.addFreshEntity(bolt);
        }
        if (primed.damage() > 0) {
            target.hurt(level.damageSources().indirectMagic(attacker, attacker), primed.damage());
        }
        LOGGER.debug("[{}] {} descargo {} ({} de daño) sobre {}.", DojutsuMod.MOD_NAME,
                attacker.getGameProfile().getName(), primed.jutsuId(), primed.damage(), target.getName().getString());
    }
}
