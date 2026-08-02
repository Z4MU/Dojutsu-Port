package com.zamu.dojutsu.progression;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.domain.event.NinjaXpChangedEvent;
import com.zamu.dojutsu.domain.progression.NinjaRank;
import dev.architectury.utils.GameInstance;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Recalculo <b>reactivo</b> del rango ninja ({@code get_rank} del datapack, {@code [S20]}). Se suscribe a
 * {@link NinjaXpChangedEvent} y actua <b>solo cuando el rango cambia</b> (cruce de umbral), notificando al
 * jugador. Reemplaza el {@code get_rank} que el datapack recalculaba al abrir el menu.
 *
 * <p>El rango es <b>derivado</b> de {@code ninja_xp} ({@code ProgressionState.rank()} = {@code NinjaRank.fromXp}),
 * no un campo cacheado: no hay que actualizar ningun estado, solo <b>detectar el cambio</b> para el efecto
 * observable. Es cosmetico ({@code [ADD/00 §3]}: no gatea habilidades). Sin recalculo por tick (por evento).
 *
 * <p>La sincronizacion del rango al HUD llega con el cliente (DJT-041); aqui el efecto es la notificacion.
 */
public final class RankService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private RankService() {
    }

    /** Suscribe el recalculo reactivo del rango al bus de dominio. Llamar una vez en el arranque. */
    public static void register() {
        DomainEvents.bus().subscribe(NinjaXpChangedEvent.class, RankService::onNinjaXpChanged);
    }

    /**
     * Rango nuevo si el cambio de {@code ninja_xp} cruzo un umbral; vacio si el rango no cambio. Puro (derivado
     * de {@link NinjaRank#fromXp}), testeable sin Minecraft.
     */
    public static Optional<NinjaRank> rankChangeFrom(int oldXp, int newXp) {
        NinjaRank previous = NinjaRank.fromXp(oldXp);
        NinjaRank current = NinjaRank.fromXp(newXp);
        return previous == current ? Optional.empty() : Optional.of(current);
    }

    private static void onNinjaXpChanged(NinjaXpChangedEvent event) {
        rankChangeFrom(event.oldXp(), event.newXp()).ifPresent(rank -> notifyRankUp(event.player(), rank));
    }

    private static void notifyRankUp(UUID playerId, NinjaRank rank) {
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return; // sin servidor activo: nada que notificar
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return; // el jugador ya no esta conectado
        }
        player.sendSystemMessage(Component.literal("Nuevo rango ninja: " + rank.title() + "!"));
        LOGGER.info("[{}] {} alcanzo el rango {}.", DojutsuMod.MOD_NAME, player.getGameProfile().getName(), rank.title());
    }
}
