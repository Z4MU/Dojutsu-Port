package com.zamu.dojutsu.items;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.items.SmokeBombTable;
import com.zamu.dojutsu.entity.AbilityAnchor;
import com.zamu.dojutsu.entity.ExplosiveTagEntity;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Activacion de los items que se usan <b>soltandolos</b> ({@code [S14]}, F10/DJT-225): la bomba de humo y el
 * sello explosivo suelto.
 *
 * <h2>Se dispara al aterrizar, no al soltar</h2>
 * Los dos predicates del datapack ({@code items/smoke_bomb}, {@code items/explos_tag}) exigen {@code OnGround:1b}:
 * el efecto ocurre <b>donde cae el item</b>, no donde estaba tu mano. Importa: un sello que se armara a la altura
 * de los ojos quedaria flotando, y una cortina de humo que saliera del jugador no taparia una retirada.
 *
 * <h2>Como se observa la caida sin barrer el mundo</h2>
 * El datapack recorre <b>todas</b> las entidades item del mundo cada tick preguntando si alguna es una bomba. Aqui
 * el alta es <b>reactiva</b> ({@link PlayerEvent#DROP_ITEM}, nativo en los dos loaders) y solo entra en la lista de
 * vigilancia lo que de verdad es una bomba o un sello. El bucle por tick recorre esa lista, que en una partida
 * normal esta <b>vacia</b> y como mucho tiene lo que este cayendo ahora mismo.
 *
 * <p>No se puede resolver del todo sin bucle: "tocar el suelo" no es un evento en Minecraft, y el item cae con
 * fisica vanilla. Lo que si se puede es que el coste dependa de cuantas bombas hay en el aire y no de cuantos
 * items hay en el mundo.
 */
public final class DropUseService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Ticks tras los que se deja de vigilar un item que nunca aterriza (cayo al vacio, quedo en un liquido raro
     * o alguien lo empujo eternamente). Sin esto la lista crece sin techo en un servidor de larga vida.
     */
    private static final int WATCH_TIMEOUT_TICKS = 400;

    private static final List<Watched> WATCHED = new ArrayList<>();

    /** Un item soltado del que esperamos que toque el suelo. */
    private record Watched(ItemEntity entity, DropUseItem.Kind kind, UUID ownerId, long droppedAtTick) {
    }

    private DropUseService() {
    }

    /** Suscribe el alta reactiva y el barrido de la lista de vigilancia. Llamar una vez en el arranque. */
    public static void register() {
        PlayerEvent.DROP_ITEM.register(DropUseService::onDrop);
        TickEvent.SERVER_POST.register(DropUseService::tick);
    }

    /** Vacia la vigilancia entre partidas (las entidades del mundo anterior ya no existen). */
    public static void reset() {
        WATCHED.clear();
    }

    private static EventResult onDrop(Player player, ItemEntity item) {
        if (item.getItem().getItem() instanceof DropUseItem dropUse && !player.level().isClientSide) {
            WATCHED.add(new Watched(item, dropUse.kind(), player.getUUID(), player.level().getGameTime()));
        }
        return EventResult.pass(); // nunca se cancela el soltar: el item cae como cualquier otro
    }

    private static void tick(MinecraftServer server) {
        if (WATCHED.isEmpty()) {
            return;
        }
        Iterator<Watched> it = WATCHED.iterator();
        while (it.hasNext()) {
            Watched watched = it.next();
            ItemEntity entity = watched.entity();
            if (!entity.isAlive() || !(entity.level() instanceof ServerLevel level)) {
                it.remove();
                continue;
            }
            if (level.getGameTime() - watched.droppedAtTick() > WATCH_TIMEOUT_TICKS) {
                it.remove(); // nunca aterrizo: se queda como item normal, recogible
                continue;
            }
            if (!entity.onGround()) {
                continue;
            }
            it.remove();
            trigger(level, entity, watched);
        }
    }

    /** El item aterrizo: se consume y ocurre lo suyo. */
    private static void trigger(ServerLevel level, ItemEntity entity, Watched watched) {
        Vec3 at = entity.position();
        Entity owner = ownerOrItem(level, entity, watched.ownerId());
        // el datapack hace kill @s sobre la entidad item entera: una pila de 5 bombas se gasta entera de una
        entity.discard();
        switch (watched.kind()) {
            case SMOKE_BOMB -> spawnSmoke(level, owner, at);
            case EXPLOSIVE_TAG -> placeTag(level, at);
        }
    }

    /**
     * Dueño del efecto: el jugador que lo solto si sigue conectado, o el propio item si se fue (el ancla exige
     * un dueño y el humo debe salir igual — quien lo tiro ya no importa una vez cayo).
     */
    private static Entity ownerOrItem(ServerLevel level, ItemEntity entity, UUID ownerId) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerId);
        return player != null ? player : entity;
    }

    /**
     * Cortina de humo: un {@link AbilityAnchor} de {@value SmokeBombTable#DURATION_TICKS} ticks que suelta humo y
     * chisporrotea. El ancla encaja sin reservas — el humo es un efecto transitorio, y su caducidad ya venia
     * garantizada de F4.
     */
    private static void spawnSmoke(ServerLevel level, Entity owner, Vec3 at) {
        AbilityAnchor anchor = AbilityAnchor.create(level, owner, SmokeBombTable.DURATION_TICKS)
                .at(at.add(0.0D, SmokeBombTable.SPAWN_HEIGHT, 0.0D))
                .withTickCallback(DropUseService::smokePulse);
        level.addFreshEntity(anchor);
    }

    /** Un tick de humo: particulas y chisporroteo, sin efecto de estado ({@code items/smoke_bomb/smoke}). */
    private static void smokePulse(AbilityAnchor anchor) {
        if (!(anchor.level() instanceof ServerLevel level)) {
            return;
        }
        level.playSound(null, anchor.getX(), anchor.getY(), anchor.getZ(),
                SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS,
                SmokeBombTable.SOUND_VOLUME, SmokeBombTable.SOUND_PITCH);
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                anchor.getX(), anchor.getY() + SmokeBombTable.PARTICLE_HEIGHT, anchor.getZ(),
                SmokeBombTable.PARTICLE_COUNT,
                SmokeBombTable.PARTICLE_SPREAD, SmokeBombTable.PARTICLE_SPREAD, SmokeBombTable.PARTICLE_SPREAD,
                SmokeBombTable.PARTICLE_SPEED);
    }

    /** Sello explosivo: se coloca desarmado donde cayo. */
    private static void placeTag(ServerLevel level, Vec3 at) {
        if (ExplosiveTagEntity.place(level, at) == null) {
            LOGGER.warn("[{}] El mundo rechazo un sello explosivo en {} {} {}.",
                    DojutsuMod.MOD_NAME, (int) at.x, (int) at.y, (int) at.z);
        }
    }
}
