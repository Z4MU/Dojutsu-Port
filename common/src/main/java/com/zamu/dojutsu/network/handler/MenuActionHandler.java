package com.zamu.dojutsu.network.handler;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.progression.XpCurve;
import com.zamu.dojutsu.network.MenuActionPayload;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.progression.SkillPointService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Handler servidor de {@link MenuActionPayload} ({@code [ADD/07 "validar todo C2S"]}, {@code [S13]}). El
 * cliente solo <b>solicita</b>; aqui se valida y se aplica. <b>Nunca</b> se confia en el cliente.
 *
 * <p>Validaciones: emisor real ({@link ServerPlayer}), anti-spam basico, accion soportada, habilidad resuelta
 * <b>por nombre</b> (no por indice del cliente), cantidad en rango, y legalidad del gasto (puntos/cap, que
 * comprueba {@link SkillPointService#spend}). Un payload invalido se rechaza <b>sin efecto</b> y se registra.
 * Tras aplicar, se sincroniza el resultado al cliente.
 */
public final class MenuActionHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Intervalo minimo entre acciones de menu por jugador (anti-spam basico). */
    static final long MIN_INTERVAL_MS = 50L;

    /** Ultima accion aceptada por jugador (anti-spam). Se limpia al desconectar. */
    private static final Map<UUID, Long> LAST_ACTION_MS = new ConcurrentHashMap<>();

    private MenuActionHandler() {
    }

    /** Registra el payload C2S con su handler. Llamar durante la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, MenuActionPayload.TYPE,
                MenuActionPayload.CODEC, MenuActionHandler::onServerReceive);
    }

    /** Olvida el estado anti-spam de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LAST_ACTION_MS.remove(playerId);
    }

    /**
     * Anti-spam basico: indica si la accion llega demasiado pronto tras la anterior aceptada. Si no lo esta,
     * registra el instante. Visible para tests.
     */
    static boolean isRateLimited(UUID playerId, long nowMs) {
        Long last = LAST_ACTION_MS.get(playerId);
        if (last != null && nowMs - last < MIN_INTERVAL_MS) {
            return true;
        }
        LAST_ACTION_MS.put(playerId, nowMs);
        return false;
    }

    /** Resuelve la habilidad <b>por nombre</b> (nunca por indice del cliente); {@code null} si es invalida. */
    static Skill parseSkill(String skillId) {
        if (skillId == null) {
            return null;
        }
        for (Skill skill : Skill.values()) {
            if (skill.name().equalsIgnoreCase(skillId)) {
                return skill;
            }
        }
        return null;
    }

    private static void onServerReceive(MenuActionPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return; // sin emisor valido (no deberia ocurrir en C2S)
            }
            if (isRateLimited(player.getUUID(), System.currentTimeMillis())) {
                LOGGER.warn("[{}] Accion de menu ignorada por rate-limit ({}).",
                        DojutsuMod.MOD_NAME, player.getGameProfile().getName());
                return;
            }
            handle(player, payload);
        });
    }

    private static void handle(ServerPlayer player, MenuActionPayload payload) {
        if (payload.action() != MenuActionPayload.Action.SPEND_SKILL) {
            reject(player, "accion no soportada: " + payload.action());
            return;
        }
        Skill skill = parseSkill(payload.skillId());
        if (skill == null) {
            reject(player, "habilidad invalida: " + payload.skillId());
            return;
        }
        // Cota de sanidad: nunca tiene sentido gastar mas puntos que el cap de nivel.
        if (payload.amount() <= 0 || payload.amount() > XpCurve.MAX_LEVEL) {
            reject(player, "cantidad invalida: " + payload.amount());
            return;
        }
        if (!SkillPointService.spend(player, skill, payload.amount())) {
            reject(player, "gasto rechazado (puntos insuficientes o cap) en " + skill);
            return;
        }
        NinjaSync.sendDelta(player); // sincroniza el resultado al cliente
    }

    private static void reject(ServerPlayer player, String reason) {
        LOGGER.warn("[{}] Accion de menu rechazada de {}: {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), reason);
    }
}
