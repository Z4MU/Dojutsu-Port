package com.zamu.dojutsu.fabric.platform;

import com.zamu.dojutsu.platform.NetworkService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * Helper client-only de red para Fabric. Aisla las clases {@code ClientPlayNetworking}
 * (solo cliente) para que nunca se carguen en el servidor dedicado: solo se referencia
 * desde ramas de {@link FabricNetworkService} protegidas por comprobacion de entorno.
 */
final class FabricClientNetworking {

    private FabricClientNetworking() {
    }

    static <T extends CustomPacketPayload> void registerReceiver(CustomPacketPayload.Type<T> type,
                                                                 NetworkService.Handler<T> handler) {
        ClientPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                handler.handle(payload, clientContext(context)));
    }

    static void sendToServer(CustomPacketPayload payload) {
        if (ClientPlayNetworking.canSend(payload.type())) {
            ClientPlayNetworking.send(payload);
        }
    }

    private static NetworkService.Context clientContext(ClientPlayNetworking.Context context) {
        return new NetworkService.Context() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public void enqueueWork(Runnable task) {
                // Los receptores de Fabric ya corren en el hilo principal del cliente.
                task.run();
            }
        };
    }
}
