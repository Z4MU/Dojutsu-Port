package com.zamu.dojutsu.network.handler;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.onboarding.OnboardingController;
import com.zamu.dojutsu.network.OnboardingChoicePayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Handler servidor de {@link OnboardingChoicePayload} ({@code [ADD/07 "validar todo C2S"]}, DJT-059). El
 * cliente solo <b>solicita</b>; la validacion real (fase actual, clan/afinidad por nombre contra los
 * registros, bonus una sola vez) vive en {@code OnboardingController} y sus servicios — aqui solo se
 * comprueba el emisor y se normaliza la entrada. Un payload invalido se rechaza <b>sin efecto</b> y se
 * registra. No necesita rate-limit: la maquina de estados hace inocuos los repetidos (fase incorrecta = no-op).
 */
public final class OnboardingChoiceHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    private OnboardingChoiceHandler() {
    }

    /** Registra el payload C2S con su handler. Llamar durante la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, OnboardingChoicePayload.TYPE,
                OnboardingChoicePayload.CODEC, OnboardingChoiceHandler::onServerReceive);
    }

    private static void onServerReceive(OnboardingChoicePayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return; // sin emisor valido (no deberia ocurrir en C2S)
            }
            String choice = payload.choice() == null ? "" : payload.choice().toLowerCase(Locale.ROOT);
            boolean applied = switch (payload.kind()) {
                case CLAN -> OnboardingController.chooseClan(player, choice);
                case AFFINITY -> OnboardingController.chooseAffinity(player, choice);
            };
            if (!applied) {
                LOGGER.warn("[{}] Eleccion de onboarding rechazada de {}: {} '{}' (fase o eleccion invalidas).",
                        DojutsuMod.MOD_NAME, player.getGameProfile().getName(), payload.kind(), choice);
            }
        });
    }
}
