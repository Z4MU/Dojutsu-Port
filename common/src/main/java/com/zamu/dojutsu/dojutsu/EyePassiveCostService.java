package com.zamu.dojutsu.dojutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import java.util.OptionalInt;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Drena chakra mientras llevas un ojo <b>encendido</b> (F11/DJT-238). Cierra la deuda que el javadoc de
 * {@link EyeDefinition} declaraba desde DJT-113: DJT-116 porto la fatiga y dejo el drenaje sin portar.
 *
 * <p><b>Solo cobra con el ojo ENCENDIDO</b>, y esa es una desviacion consciente del datapack, donde el coste
 * corre mientras <b>llevas</b> el ojo porque alli no existe el interruptor (F11/DJT-230). Con interruptor, un
 * ojo apagado no cuesta nada. Se acepta: la alternativa —cobrar por poseerlo— convertiria el interruptor en
 * un adorno. Registrado en {@code docs/development_log.md} (regla 16).
 *
 * <p><b>Si no puedes pagar, el ojo se apaga.</b> El datapack dejaba el chakra en negativo, que aqui no es
 * representable: {@code ChakraService} no permite consumos que no caben. Apagarlo es la lectura mas cercana
 * —te quedas sin fuerzas para sostenerlo— y encaja con lo que ya hace la fatiga al 100%. El ojo <b>no</b> se
 * pierde; se apaga, y se vuelve a encender cuando haya chakra.
 *
 * <p>El temporizador vive en {@link EyeState#timers}, no en un mapa de sesion: asi el cobro no se reinicia al
 * reconectar y no hace falta limpiar nada al salir.
 */
public final class EyePassiveCostService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Clave del temporizador de cobro dentro de {@code EyeState.timers}. */
    static final String TIMER_KEY = "passive_cost";

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        EyeState state = data.eye();
        if (state.poweredEye().isEmpty()) {
            // Sin ojo encendido no hay trabajo. Es el caso comun, y sale antes de tocar el registro.
            return;
        }
        EyeDefinition definition = state.poweredEye().flatMap(EyeRegistry::get).orElse(null);
        if (definition == null || definition.passiveCost().isEmpty()) {
            return; // ojo desconocido o sin coste declarado
        }
        advance(player, data, state, definition);
    }

    /** Cuenta el intervalo y, al cumplirse, cobra. Separado del tick para poder leerlo de un vistazo. */
    private void advance(ServerPlayer player, PlayerNinjaData data, EyeState state,
            EyeDefinition definition) {
        int interval = definition.passiveCost().orElseThrow().intervalTicks();
        int elapsed = state.timer(TIMER_KEY) + 1;
        if (elapsed < interval) {
            data.setEye(state.withTimer(TIMER_KEY, elapsed));
            return;
        }
        EyeState charged = charge(player, data, state.withTimer(TIMER_KEY, 0), definition);
        data.setEye(charged);
    }

    /**
     * Cobra el coste del intervalo. Devuelve el estado resultante: el mismo si se pago, o apagado si no
     * llegaba el chakra.
     */
    private EyeState charge(ServerPlayer player, PlayerNinjaData data, EyeState state,
            EyeDefinition definition) {
        OptionalInt cost = EyeCostResolver.costFor(definition,
                EyeCostResolver.contextOf(state, definition, data.clan().clan()));
        if (cost.isEmpty()) {
            // Ojo con coste declarado cuyas reglas no cubren a este jugador y sin fallback: es un dato
            // incompleto, no una situacion normal. Se avisa una vez por cobro y no se cobra nada.
            LOGGER.warn("[{}] El ojo {} no sabe cuanto cobrarle a {} (reglas sin caso ni fallback).",
                    DojutsuMod.MOD_NAME, definition.id(), player.getGameProfile().getName());
            return state;
        }
        int amount = cost.getAsInt();
        if (amount <= 0) {
            return state;
        }
        if (ChakraService.consume(data, amount)) {
            return state;
        }
        player.displayClientMessage(Component.translatable("dojutsu.dojutsu.exhausted",
                Component.translatable("eye.dojutsu." + definition.id())).withStyle(ChatFormatting.RED), true);
        LOGGER.info("[{}] {} se quedo sin chakra para sostener {}; el ojo se apaga.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), definition.id());
        return state.withEnabled(false);
    }
}
