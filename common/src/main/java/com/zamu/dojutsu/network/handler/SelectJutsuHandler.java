package com.zamu.dojutsu.network.handler;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.jutsu.JutsuSelectionService;
import com.zamu.dojutsu.jutsu.JutsuSelectionService.SelectOutcome;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.network.SelectJutsuPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Handler servidor de {@link SelectJutsuPayload} (F11/DJT-231). El cliente <b>solicita</b> un jutsu por nombre;
 * aqui se valida contra los aprendidos y se escribe en el estado. Mismo patron que
 * {@link EyeMenuHandler#isSelectRateLimited} y {@link CastJutsuHandler}.
 *
 * <p>Solo se sincroniza cuando el estado <b>cambia</b>: la rueda puede soltar varias selecciones seguidas
 * sobre el mismo jutsu y no tiene sentido empujar un delta identico por cada una.
 */
public final class SelectJutsuHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Intervalo minimo entre selecciones aceptadas por jugador (anti-spam de la rueda). */
    static final long MIN_INTERVAL_MS = 50L;

    /** Ultima seleccion aceptada por jugador. Se limpia al desconectar. */
    private static final Map<UUID, Long> LAST_SELECT_MS = new ConcurrentHashMap<>();

    private SelectJutsuHandler() {
    }

    /** Registra el payload C2S con su handler. Llamar durante la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, SelectJutsuPayload.TYPE,
                SelectJutsuPayload.CODEC, SelectJutsuHandler::onServerReceive);
    }

    /** Olvida el estado anti-spam de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LAST_SELECT_MS.remove(playerId);
    }

    /** Anti-spam: {@code true} si la seleccion llega demasiado pronto tras la anterior aceptada. */
    static boolean isRateLimited(UUID playerId, long nowMs) {
        Long last = LAST_SELECT_MS.get(playerId);
        if (last != null && nowMs - last < MIN_INTERVAL_MS) {
            return true;
        }
        LAST_SELECT_MS.put(playerId, nowMs);
        return false;
    }

    private static void onServerReceive(SelectJutsuPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return; // sin emisor valido (no deberia ocurrir en C2S)
            }
            if (isRateLimited(player.getUUID(), System.currentTimeMillis())) {
                return; // seleccion demasiado rapida: se ignora en silencio
            }
            handle(player, payload);
        });
    }

    private static void handle(ServerPlayer player, SelectJutsuPayload payload) {
        SelectOutcome outcome = payload.isClear()
                ? JutsuSelectionService.clear(NinjaData.get(player))
                : JutsuSelectionService.select(NinjaData.get(player), payload.jutsuId());
        switch (outcome) {
            case SELECTED -> {
                player.displayClientMessage(Component.translatable("jutsu.dojutsu.selected",
                        Component.translatable("jutsu.dojutsu." + payload.jutsuId()))
                        .withStyle(ChatFormatting.AQUA), true);
                NinjaSync.sendDelta(player); // la seleccion es observable: el HUD la pinta
            }
            case CLEARED -> {
                player.displayClientMessage(Component.translatable("jutsu.dojutsu.deselected")
                        .withStyle(ChatFormatting.GRAY), true);
                NinjaSync.sendDelta(player);
            }
            case NOT_KNOWN -> {
                // Cliente desfasado o payload manipulado: se rechaza sin efecto y se le devuelve la verdad.
                LOGGER.warn("[{}] {} intento seleccionar un jutsu no conocido: {}.",
                        DojutsuMod.MOD_NAME, player.getGameProfile().getName(), payload.jutsuId());
                NinjaSync.sendFull(player);
            }
            case UNCHANGED -> {
                // ya lo tenia seleccionado: ni mensaje ni trafico
            }
        }
    }
}
