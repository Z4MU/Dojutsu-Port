package com.zamu.dojutsu.network.handler;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.items.JutsuComponent;
import com.zamu.dojutsu.jutsu.JutsuCastPipeline;
import com.zamu.dojutsu.network.CastJutsuPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.registry.ModDataComponents;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Handler servidor de {@link CastJutsuPayload} (F4/DJT-080, {@code [ADD/07 "validar todo C2S"]}). El cliente solo
 * <b>solicita</b> el cast con una mano; aquí se valida y se aplica. <b>Nunca</b> se confía en el cliente.
 *
 * <p>Validaciones (en orden): emisor real, anti-spam de la tecla, que el ítem de esa mano sea un ítem de jutsu
 * ({@link JutsuComponent}), y que el jugador <b>conozca</b> ese jutsu ({@code JutsuKnowledge.known}) — un ítem
 * dado por comando o residual no se puede castear sin haberlo aprendido. El jutsu real sale del <b>componente
 * del ítem</b>, no del cliente. Si todo pasa, delega en {@link JutsuCastPipeline#cast(ServerPlayer, String)},
 * que aplica las puertas de chakra/cd/afinidad y sincroniza.
 */
public final class CastJutsuHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Intervalo mínimo entre pulsaciones aceptadas por jugador (anti-spam; el cd real lo pone el pipeline). */
    static final long MIN_INTERVAL_MS = 50L;

    /** Última pulsación aceptada por jugador (anti-spam). Se limpia al desconectar. */
    private static final Map<UUID, Long> LAST_CAST_MS = new ConcurrentHashMap<>();

    private CastJutsuHandler() {
    }

    /** Registra el payload C2S con su handler. Llamar durante la inicialización del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, CastJutsuPayload.TYPE,
                CastJutsuPayload.CODEC, CastJutsuHandler::onServerReceive);
    }

    /** Olvida el estado anti-spam de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LAST_CAST_MS.remove(playerId);
    }

    /** Anti-spam básico: true si la pulsación llega demasiado pronto tras la anterior aceptada. Visible para tests. */
    static boolean isRateLimited(UUID playerId, long nowMs) {
        Long last = LAST_CAST_MS.get(playerId);
        if (last != null && nowMs - last < MIN_INTERVAL_MS) {
            return true;
        }
        LAST_CAST_MS.put(playerId, nowMs);
        return false;
    }

    private static void onServerReceive(CastJutsuPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return; // sin emisor valido (no deberia ocurrir en C2S)
            }
            if (isRateLimited(player.getUUID(), System.currentTimeMillis())) {
                return; // pulsacion demasiado rapida: se ignora en silencio
            }
            handle(player, payload.hand());
        });
    }

    private static void handle(ServerPlayer player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        JutsuComponent component = stack.get(ModDataComponents.JUTSU.get());
        if (component == null) {
            return; // el item de esa mano no es un jutsu: sin efecto (la tecla se pulso sin jutsu equipado)
        }
        if (!NinjaData.get(player).jutsu().knows(component.jutsuId())) {
            // Item de jutsu no aprendido (comando/residuo): no se puede castear sin conocerlo.
            LOGGER.warn("[{}] {} intento castear un jutsu no conocido: {}.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), component.jutsuId());
            return;
        }
        JutsuCastPipeline.cast(player, component.jutsuId()); // el pipeline aplica puertas + feedback + sync
    }
}
