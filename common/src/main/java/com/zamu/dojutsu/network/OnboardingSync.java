package com.zamu.dojutsu.network;

import com.zamu.dojutsu.clans.ClanDefinition;
import com.zamu.dojutsu.clans.ClanRegistry;
import com.zamu.dojutsu.client.ClientOnboardingOptions;
import com.zamu.dojutsu.network.OnboardingOptionsPayload.ClanOption;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

/**
 * Envio de las opciones de la creacion del ninja al cliente (S2C), sobre el puerto {@link NetworkService}.
 * Mismo patron que {@link NinjaSync}: registro + emisor en servidor + receptor que escribe en la cache
 * read-only del cliente ({@link ClientOnboardingOptions}).
 *
 * <p>Lo envia {@code OnboardingController.refresh} cuando el jugador queda en {@code SELECTING_CLAN}
 * (login de un nuevo, reanudacion en esa fase o re-embarque tras reset). La pantalla de afinidad no
 * necesita datos del servidor: las 5 basicas son la constante de dominio {@code NatureType.basics()},
 * la misma contra la que valida el servidor.
 */
public final class OnboardingSync {

    private static final int DEFAULT_COLOR = 0xFFFFFF; // clan sin teamColor (no pasa con los 14 actuales)

    private OnboardingSync() {
    }

    /** Registra el payload S2C y su receptor de cliente. Llamar en la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.S2C, OnboardingOptionsPayload.TYPE,
                OnboardingOptionsPayload.CODEC, OnboardingSync::onClientReceive);
    }

    /** Envia al jugador las opciones de clan actuales (ids ordenados + color del team). */
    public static void sendOptions(ServerPlayer player) {
        List<ClanOption> options = new ArrayList<>();
        for (String id : ClanRegistry.all().keySet().stream().sorted().toList()) {
            int color = ClanRegistry.get(id)
                    .flatMap(ClanDefinition::teamColor)
                    .map(ChatFormatting::getColor)
                    .orElse(DEFAULT_COLOR);
            options.add(new ClanOption(id, color));
        }
        PlatformServices.NETWORK.sendToPlayer(player, new OnboardingOptionsPayload(options));
    }

    private static void onClientReceive(OnboardingOptionsPayload payload, NetworkService.Context context) {
        // Corre en el cliente: escribe en la cache read-only que consulta la NinjaCreationScreen.
        context.enqueueWork(() -> ClientOnboardingOptions.set(payload.clans()));
    }
}
