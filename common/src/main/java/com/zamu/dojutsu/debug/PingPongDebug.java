package com.zamu.dojutsu.debug;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.network.PingPayload;
import com.zamu.dojutsu.network.PongPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/**
 * Andamiaje de F0 para validar el puerto {@link NetworkService}: registra los receptores
 * del ping-pong dummy. El servidor recibe {@link PingPayload} y responde con
 * {@link PongPayload}; el cliente registra el receptor del Pong.
 *
 * <p>Temporal; se retira con el resto del apoyo dummy.
 */
public final class PingPongDebug {

    private static final Logger LOGGER = LogUtils.getLogger();

    private PingPongDebug() {
    }

    /** Registra los receptores C2S (servidor) y S2C (cliente). Llamar en {@code init()}. */
    public static void registerReceivers() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, PingPayload.TYPE, PingPayload.CODEC,
                PingPongDebug::onPing);
        PlatformServices.NETWORK.register(NetworkService.Side.S2C, PongPayload.TYPE, PongPayload.CODEC,
                PingPongDebug::onPong);
    }

    private static void onPing(PingPayload ping, NetworkService.Context context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            String name = player != null ? player.getName().getString() : "?";
            LOGGER.info("[{}] Ping recibido (nonce={}) de {}.", DojutsuMod.MOD_NAME, ping.nonce(), name);
            if (player instanceof ServerPlayer serverPlayer) {
                PlatformServices.NETWORK.sendToPlayer(serverPlayer, new PongPayload(ping.nonce()));
            }
        });
    }

    private static void onPong(PongPayload pong, NetworkService.Context context) {
        context.enqueueWork(() ->
                LOGGER.info("[{}] Pong recibido (nonce={}).", DojutsuMod.MOD_NAME, pong.nonce()));
    }
}
