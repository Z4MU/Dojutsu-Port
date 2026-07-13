package com.zamu.dojutsu.platform;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Puerto de red cliente<->servidor sobre el sistema {@code CustomPacketPayload} de 1.21.
 * Permite registrar payloads con su manejador y enviarlos en ambos sentidos sin conocer
 * el mecanismo de red de cada loader.
 */
public interface NetworkService {

    /** Sentido de un payload. */
    enum Side {
        /** Cliente -> Servidor. */
        C2S,
        /** Servidor -> Cliente. */
        S2C
    }

    /** Contexto de recepcion de un payload. */
    interface Context {

        /**
         * @return el jugador asociado (emisor en C2S, jugador local en S2C); puede ser nulo
         *         si aun no existe.
         */
        Player player();

        /**
         * Encola una tarea para ejecutarse en el hilo principal (los payloads llegan en el
         * hilo de red).
         *
         * @param task tarea a ejecutar
         */
        void enqueueWork(Runnable task);
    }

    /** Manejador de un payload recibido. */
    @FunctionalInterface
    interface Handler<T extends CustomPacketPayload> {
        void handle(T payload, Context context);
    }

    /**
     * Registra un tipo de payload y su manejador en un sentido. Debe llamarse durante la
     * inicializacion del mod.
     *
     * @param side    sentido del payload
     * @param type    tipo del payload
     * @param codec   codec de serializacion
     * @param handler manejador de recepcion
     * @param <T>     tipo del payload
     */
    <T extends CustomPacketPayload> void register(Side side, CustomPacketPayload.Type<T> type,
                                                  StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                  Handler<T> handler);

    /**
     * Envia un payload del cliente al servidor. Debe llamarse desde el entorno de cliente.
     *
     * @param payload payload a enviar
     */
    void sendToServer(CustomPacketPayload payload);

    /**
     * Envia un payload del servidor a un jugador concreto.
     *
     * @param player  destinatario
     * @param payload payload a enviar
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
