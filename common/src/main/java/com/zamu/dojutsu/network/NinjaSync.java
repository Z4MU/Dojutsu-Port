package com.zamu.dojutsu.network;

import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import net.minecraft.server.level.ServerPlayer;

/**
 * Sincronizacion del estado ninja observable al cliente (S2C), sobre el puerto {@link NetworkService}.
 *
 * <p>Envia un snapshot <b>completo</b> en join/respawn/cambio de dimension ({@link #sendFull}) y un
 * <b>delta</b> tras un cambio ({@link #sendDelta}, no-op si no hay campos sucios). Solo viaja el estado
 * observable (nunca todo). El cliente <b>nunca</b> decide gameplay a partir de esto.
 */
public final class NinjaSync {

    private NinjaSync() {
    }

    /** Registra el payload S2C y su receptor de cliente. Llamar en la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.S2C, SyncNinjaDataPayload.TYPE,
                SyncNinjaDataPayload.CODEC, NinjaSync::onClientReceive);
    }

    /** Envia el snapshot completo del estado observable y limpia el tracker. */
    public static void sendFull(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        data.dirtyTracker().clear();
        PlatformServices.NETWORK.sendToPlayer(player, snapshot(true, data));
    }

    /** Envia un delta con los campos observables cambiados; no hace nada si no hay cambios. */
    public static void sendDelta(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        if (data.dirtyTracker().isEmpty()) {
            return;
        }
        data.dirtyTracker().clear();
        PlatformServices.NETWORK.sendToPlayer(player, snapshot(false, data));
    }

    /** Construye el payload con todo el estado observable actual (onboarding + chakra + progresion + cooldowns). */
    private static SyncNinjaDataPayload snapshot(boolean full, PlayerNinjaData data) {
        ChakraState chakra = data.chakra();
        return new SyncNinjaDataPayload(full, data.onboarding().phase(), chakra.chakra(), chakra.maxChakra(),
                ProgressionSnapshot.of(data.progression()), data.jutsu().cooldowns());
    }

    private static void onClientReceive(SyncNinjaDataPayload payload, NetworkService.Context context) {
        // Corre en el cliente: escribe en la cache read-only. Los deltas antes del primer full se ignoran.
        context.enqueueWork(() -> {
            if (payload.full()) {
                ClientNinjaData.applyFull(payload.onboarding(), payload.chakra(), payload.maxChakra(),
                        payload.progression(), payload.cooldowns());
            } else {
                ClientNinjaData.applyDelta(payload.onboarding(), payload.chakra(), payload.maxChakra(),
                        payload.progression(), payload.cooldowns());
            }
        });
    }
}
