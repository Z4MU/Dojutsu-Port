package com.zamu.dojutsu.debug;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.network.PingPayload;
import com.zamu.dojutsu.platform.PlatformServices;
import dev.architectury.event.events.client.ClientPlayerEvent;
import org.slf4j.Logger;

/**
 * Disparador de cliente del ping-pong dummy (F0). Al entrar el jugador local a un mundo,
 * envia un {@link PingPayload} al servidor. Solo debe invocarse desde el entorno de
 * cliente ({@code initClient()}).
 *
 * <p>La impl de red de cada loader ignora el envio si el servidor no puede recibir el
 * payload (p. ej. servidor vanilla), asi que no hace falta comprobarlo aqui.
 *
 * <p>Temporal; se retira con el resto del apoyo dummy.
 */
public final class PingPongClientDebug {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NONCE = 7;

    private PingPongClientDebug() {
    }

    /** Suscribe el envio del Ping al unirse el jugador local. Llamar en {@code initClient()}. */
    public static void registerClientTrigger() {
        ClientPlayerEvent.CLIENT_PLAYER_JOIN.register(player -> {
            LOGGER.info("[{}] Enviando Ping (nonce={}) al servidor.", DojutsuMod.MOD_NAME, NONCE);
            PlatformServices.NETWORK.sendToServer(new PingPayload(NONCE));
        });
    }
}
