package com.zamu.dojutsu.network.handler;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.client.ClientEyeMenu;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.dojutsu.EyeMenuService;
import com.zamu.dojutsu.dojutsu.EyeMenuService.EyeMenuView;
import com.zamu.dojutsu.network.EyeMenuPayload;
import com.zamu.dojutsu.network.OpenEyeMenuPayload;
import com.zamu.dojutsu.network.SelectEyeAbilityPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Red del menu de habilidades oculares (DJT-117): registra el request C2S de apertura, la seleccion C2S y el
 * envio S2C de la vista. El cliente <b>nunca</b> decide gameplay ({@code [ADD/07]}): la vista la construye el
 * servidor desde el {@code EyeState} real ({@link EyeMenuService}) y toda seleccion se re-valida contra el ojo
 * activo. Mismo patron que {@link MenuActionHandler}/{@code OnboardingSync}.
 */
public final class EyeMenuHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Intervalo minimo entre selecciones por jugador (anti-spam; solo la mutacion se limita). */
    static final long MIN_INTERVAL_MS = 50L;

    /** Ultima seleccion aceptada por jugador (anti-spam). Se limpia al desconectar. */
    private static final Map<UUID, Long> LAST_SELECT_MS = new ConcurrentHashMap<>();

    private EyeMenuHandler() {
    }

    /** Registra los payloads del menu ocular (2 C2S + 1 S2C). Llamar durante la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, OpenEyeMenuPayload.TYPE,
                OpenEyeMenuPayload.CODEC, EyeMenuHandler::onOpen);
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, SelectEyeAbilityPayload.TYPE,
                SelectEyeAbilityPayload.CODEC, EyeMenuHandler::onSelect);
        PlatformServices.NETWORK.register(NetworkService.Side.S2C, EyeMenuPayload.TYPE,
                EyeMenuPayload.CODEC, EyeMenuHandler::onClientReceive);
    }

    /** Olvida el estado anti-spam de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LAST_SELECT_MS.remove(playerId);
    }

    /** Anti-spam de la seleccion: {@code true} si llega demasiado pronto tras la anterior aceptada. */
    static boolean isSelectRateLimited(UUID playerId, long nowMs) {
        Long last = LAST_SELECT_MS.get(playerId);
        if (last != null && nowMs - last < MIN_INTERVAL_MS) {
            return true;
        }
        LAST_SELECT_MS.put(playerId, nowMs);
        return false;
    }

    /** Request de apertura: construye la vista del servidor y la envia (solo lectura, sin rate-limit). */
    private static void onOpen(OpenEyeMenuPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                sendMenu(player);
            }
        });
    }

    /** Seleccion: valida en servidor y refresca la vista. */
    private static void onSelect(SelectEyeAbilityPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (isSelectRateLimited(player.getUUID(), System.currentTimeMillis())) {
                LOGGER.warn("[{}] Seleccion de habilidad ocular ignorada por rate-limit ({}).",
                        DojutsuMod.MOD_NAME, player.getGameProfile().getName());
                return;
            }
            EyeMenuService.select(NinjaData.get(player), payload.abilityId()); // rechaza sin efecto si es ajena
            sendMenu(player); // refresca la vista (refleje o no el cambio, es el estado real del servidor)
        });
    }

    /** Construye la vista actual del menu ocular y la envia al jugador (S2C). */
    public static void sendMenu(ServerPlayer player) {
        EyeMenuView view = EyeMenuService.viewFor(NinjaData.get(player));
        PlatformServices.NETWORK.sendToPlayer(player,
                new EyeMenuPayload(view.activeEye(), view.abilities(), view.selectedAbility(), view.eternalStatus()));
    }

    /** Receptor de cliente: escribe la vista en la cache read-only que consulta la Screen. */
    private static void onClientReceive(EyeMenuPayload payload, NetworkService.Context context) {
        context.enqueueWork(
                () -> ClientEyeMenu.set(payload.activeEye(), payload.abilities(), payload.selectedAbility(),
                        payload.eternalStatus()));
    }
}
