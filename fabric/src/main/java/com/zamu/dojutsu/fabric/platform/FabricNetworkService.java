package com.zamu.dojutsu.fabric.platform;

import com.zamu.dojutsu.platform.NetworkService;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Implementacion Fabric del puerto {@link NetworkService} sobre las APIs nativas de red.
 * El tipo de payload y el receptor C2S (servidor) se registran aqui; el receptor S2C y el
 * envio C2S son client-only y se delegan en {@link FabricClientNetworking}, invocado solo
 * en el entorno de cliente para no cargar clases de cliente en el servidor dedicado.
 */
public final class FabricNetworkService implements NetworkService {

    @Override
    public <T extends CustomPacketPayload> void register(Side side, CustomPacketPayload.Type<T> type,
                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                         Handler<T> handler) {
        if (side == Side.C2S) {
            PayloadTypeRegistry.playC2S().register(type, codec);
            ServerPlayNetworking.registerGlobalReceiver(type, (payload, context) ->
                    handler.handle(payload, serverContext(context)));
        } else {
            PayloadTypeRegistry.playS2C().register(type, codec);
            if (isClient()) {
                FabricClientNetworking.registerReceiver(type, handler);
            }
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (isClient()) {
            FabricClientNetworking.sendToServer(payload);
        }
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    private static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    private static Context serverContext(ServerPlayNetworking.Context context) {
        return new Context() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public void enqueueWork(Runnable task) {
                // Los receptores de Fabric ya corren en el hilo principal del servidor.
                task.run();
            }
        };
    }
}
