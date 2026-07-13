package com.zamu.dojutsu.neoforge.platform;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.NetworkService;
import dev.architectury.platform.hooks.EventBusesHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementacion NeoForge del puerto {@link NetworkService} sobre el sistema de payloads
 * nativo. Los registros se acumulan y se aplican en {@code RegisterPayloadHandlersEvent}
 * (enganchado al bus del mod), la unica ventana valida para registrar payloads.
 */
public final class NeoForgeNetworkService implements NetworkService {

    private final List<PendingRegistration<?>> pending = new ArrayList<>();
    private boolean subscribed;

    @Override
    public <T extends CustomPacketPayload> void register(Side side, CustomPacketPayload.Type<T> type,
                                                         StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
                                                         Handler<T> handler) {
        pending.add(new PendingRegistration<>(side, type, codec, handler));
        subscribeOnce();
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    private void subscribeOnce() {
        if (subscribed) {
            return;
        }
        IEventBus modEventBus = EventBusesHooks.getModEventBus(DojutsuMod.MOD_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Bus de eventos del mod no disponible para " + DojutsuMod.MOD_ID));
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, this::onRegisterPayloads);
        subscribed = true;
    }

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(DojutsuMod.MOD_ID);
        for (PendingRegistration<?> registration : pending) {
            apply(registrar, registration);
        }
    }

    private <T extends CustomPacketPayload> void apply(PayloadRegistrar registrar, PendingRegistration<T> registration) {
        if (registration.side() == Side.C2S) {
            registrar.playToServer(registration.type(), registration.codec(),
                    (payload, context) -> registration.handler().handle(payload, wrap(context)));
        } else {
            registrar.playToClient(registration.type(), registration.codec(),
                    (payload, context) -> registration.handler().handle(payload, wrap(context)));
        }
    }

    private static Context wrap(IPayloadContext context) {
        return new Context() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public void enqueueWork(Runnable task) {
                context.enqueueWork(task);
            }
        };
    }

    private record PendingRegistration<T extends CustomPacketPayload>(
            Side side,
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
            Handler<T> handler) {
    }
}
