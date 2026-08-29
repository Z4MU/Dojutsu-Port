package com.zamu.dojutsu.network.handler;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.client.ClientJutsuBook;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.jutsu.JutsuBookService;
import com.zamu.dojutsu.network.AssignSlotPayload;
import com.zamu.dojutsu.network.JutsuBookPayload;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.network.OpenJutsuBookPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Red del libro de jutsus (F11/DJT-235): peticion C2S de apertura, asignacion C2S de huecos, y envio S2C de
 * la vista. Mismo patron que {@link EyeMenuHandler}.
 *
 * <p>El cliente <b>nunca</b> decide gameplay: la vista la construye el servidor desde el estado real y toda
 * asignacion se revalida contra los jutsus aprendidos.
 */
public final class JutsuBookHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Intervalo minimo entre asignaciones por jugador (anti-spam; solo la mutacion se limita). */
    static final long MIN_INTERVAL_MS = 50L;

    /** Ultima asignacion aceptada por jugador. Se limpia al desconectar. */
    private static final Map<UUID, Long> LAST_ASSIGN_MS = new ConcurrentHashMap<>();

    private JutsuBookHandler() {
    }

    /** Registra los payloads del libro (2 C2S + 1 S2C). Llamar durante la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, OpenJutsuBookPayload.TYPE,
                OpenJutsuBookPayload.CODEC, JutsuBookHandler::onOpen);
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, AssignSlotPayload.TYPE,
                AssignSlotPayload.CODEC, JutsuBookHandler::onAssign);
        PlatformServices.NETWORK.register(NetworkService.Side.S2C, JutsuBookPayload.TYPE,
                JutsuBookPayload.CODEC, JutsuBookHandler::onClientReceive);
    }

    /** Olvida el estado anti-spam de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LAST_ASSIGN_MS.remove(playerId);
    }

    /** Anti-spam de la asignacion: {@code true} si llega demasiado pronto tras la anterior aceptada. */
    static boolean isAssignRateLimited(UUID playerId, long nowMs) {
        Long last = LAST_ASSIGN_MS.get(playerId);
        if (last != null && nowMs - last < MIN_INTERVAL_MS) {
            return true;
        }
        LAST_ASSIGN_MS.put(playerId, nowMs);
        return false;
    }

    /** Apertura: construye la vista y la envia (solo lectura, sin rate-limit). */
    private static void onOpen(OpenJutsuBookPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendBook(player);
            }
        });
    }

    /** Asignacion: valida en servidor, escribe y refresca. */
    private static void onAssign(AssignSlotPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (isAssignRateLimited(player.getUUID(), System.currentTimeMillis())) {
                return;
            }
            apply(player, payload);
            // Se refresca SIEMPRE, aceptada o no: asi el libro acaba mostrando el estado real del servidor,
            // que es la unica forma de que un cliente desfasado se entere de que su idea era vieja.
            sendBook(player);
            NinjaSync.sendDelta(player); // la rueda del HUD tambien lo lee
        });
    }

    private static void apply(ServerPlayer player, AssignSlotPayload payload) {
        PlayerNinjaData data = NinjaData.get(player);
        JutsuKnowledge knowledge = data.jutsu();
        if (payload.slot() < 0 || payload.slot() >= JutsuKnowledge.SLOT_COUNT) {
            LOGGER.warn("[{}] {} pidio un hueco fuera de rango: {}.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), payload.slot());
            return;
        }
        if (payload.isClear()) {
            data.setJutsu(knowledge.withoutSlot(payload.slot()));
            return;
        }
        if (!knowledge.knows(payload.jutsuId())) {
            LOGGER.warn("[{}] {} intento colocar un jutsu no conocido: {}.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), payload.jutsuId());
            return;
        }
        data.setJutsu(knowledge.withSlot(payload.slot(), payload.jutsuId()));
    }

    /** Construye la vista actual del libro y la envia al jugador (S2C). */
    public static void sendBook(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        PlatformServices.NETWORK.sendToPlayer(player,
                new JutsuBookPayload(JutsuBookService.entriesFor(data), data.jutsu().quickSlots()));
    }

    /** Receptor de cliente: escribe la vista en la cache read-only que consulta la Screen. */
    private static void onClientReceive(JutsuBookPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> ClientJutsuBook.set(payload.entries(), payload.slots()));
    }
}
