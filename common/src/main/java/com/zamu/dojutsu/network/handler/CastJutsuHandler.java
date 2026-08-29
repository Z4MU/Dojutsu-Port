package com.zamu.dojutsu.network.handler;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.jutsu.JutsuCastPipeline;
import com.zamu.dojutsu.jutsu.JutsuSelectionService;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.network.CastJutsuPayload;
import com.zamu.dojutsu.network.CastSlotPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handler servidor de {@link CastJutsuPayload} (F4/DJT-080, {@code [ADD/07 "validar todo C2S"]}). El cliente
 * solo <b>solicita</b> el cast; aqui se decide que se lanza y se valida. <b>Nunca</b> se confia en el cliente.
 *
 * <p>Validaciones (en orden): emisor real, anti-spam de la tecla, y que haya un jutsu seleccionado. El jutsu
 * sale de {@code JutsuKnowledge.selectedJutsu}, que ya esta validado contra los aprendidos por invariante del
 * propio estado, asi que aqui no hace falta repetir la comprobacion de "lo conoce". Si hay seleccion, delega
 * en {@link JutsuCastPipeline#cast(ServerPlayer, String)}, que aplica las puertas de chakra/cd/afinidad y
 * sincroniza.
 *
 * <p><b>F11/DJT-234:</b> este handler atiende tambien {@link CastSlotPayload}, el atajo de las teclas de
 * hueco. Comparte <b>el mismo limitador</b> a proposito: si cada tecla tuviera el suyo, alternarlas doblaria
 * el ritmo de peticiones aceptadas y el anti-spam no serviria de nada.
 *
 * <p><b>Cambio en F11/DJT-231:</b> antes el jutsu salia del {@code JutsuComponent} del item de la mano y la
 * puerta era "solo se castea lo que se conoce" —un item de jutsu dado por comando no servia—. Esa puerta no
 * desaparece: se mudo al momento de <b>seleccionar</b> ({@link SelectJutsuHandler}), y el estado la mantiene
 * despues, porque una seleccion deja de ser valida en cuanto deja de estar entre los conocidos.
 */
public final class CastJutsuHandler {

    /** Intervalo minimo entre pulsaciones aceptadas por jugador (anti-spam; el cd real lo pone el pipeline). */
    static final long MIN_INTERVAL_MS = 50L;

    /** Ultima pulsacion aceptada por jugador (anti-spam). Se limpia al desconectar. */
    private static final Map<UUID, Long> LAST_CAST_MS = new ConcurrentHashMap<>();

    private CastJutsuHandler() {
    }

    /** Registra el payload C2S con su handler. Llamar durante la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, CastJutsuPayload.TYPE,
                CastJutsuPayload.CODEC, CastJutsuHandler::onServerReceive);
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, CastSlotPayload.TYPE,
                CastSlotPayload.CODEC, CastJutsuHandler::onSlotReceive);
    }

    /** Olvida el estado anti-spam de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LAST_CAST_MS.remove(playerId);
    }

    /** Anti-spam basico: true si la pulsacion llega demasiado pronto tras la anterior aceptada. Visible para tests. */
    static boolean isRateLimited(UUID playerId, long nowMs) {
        Long last = LAST_CAST_MS.get(playerId);
        if (last != null && nowMs - last < MIN_INTERVAL_MS) {
            return true;
        }
        LAST_CAST_MS.put(playerId, nowMs);
        return false;
    }

    private static void onServerReceive(CastJutsuPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return; // sin emisor valido (no deberia ocurrir en C2S)
            }
            if (isRateLimited(player.getUUID(), System.currentTimeMillis())) {
                return; // pulsacion demasiado rapida: se ignora en silencio
            }
            handle(player);
        });
    }

    private static void onSlotReceive(CastSlotPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (isRateLimited(player.getUUID(), System.currentTimeMillis())) {
                return;
            }
            handleSlot(player, payload.slot());
        });
    }

    /**
     * Lanza el jutsu de un hueco. De paso lo deja <b>seleccionado</b>: el jugador acaba de decir cual es su
     * tecnica activa, y si no se reflejara, el HUD y la tecla de cast seguirian apuntando a otra cosa.
     */
    private static void handleSlot(ServerPlayer player, int slot) {
        PlayerNinjaData data = NinjaData.get(player);
        Optional<String> jutsuId = data.jutsu().slot(slot);
        if (jutsuId.isEmpty()) {
            // Hueco vacio (o indice fuera de rango): sin mensaje. Pulsar un hueco que sabes que esta libre no
            // es un error que merezca ruido en pantalla en mitad de un combate.
            return;
        }
        JutsuSelectionService.select(data, jutsuId.get());
        JutsuCastPipeline.cast(player, jutsuId.get()); // el pipeline ya manda el delta con todo
    }

    private static void handle(ServerPlayer player) {
        Optional<String> selected = JutsuSelectionService.selected(NinjaData.get(player));
        if (selected.isEmpty()) {
            // Sin seleccion no hay nada que lanzar. Se avisa en vez de fallar en silencio; cuando exista la
            // rueda (F11/DJT-232) el cliente la abrira ademas, pero el aviso se queda como red de seguridad.
            player.displayClientMessage(Component.translatable("jutsu.dojutsu.none_selected")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        JutsuCastPipeline.cast(player, selected.get()); // el pipeline aplica puertas + feedback + sync
    }
}
