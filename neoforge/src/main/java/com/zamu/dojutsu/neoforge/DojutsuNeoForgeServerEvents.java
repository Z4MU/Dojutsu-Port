package com.zamu.dojutsu.neoforge;

import com.zamu.dojutsu.chakra.FoodChakraHandler;
import com.zamu.dojutsu.chakra.SleepChakraHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/**
 * Suscripcion de eventos <b>nativos de NeoForge</b> del lado servidor que Architectury no expone cross-loader
 * (ver {@code [ADD/05]}). Delegan en los handlers comunes de {@code com.zamu.dojutsu.chakra}.
 *
 * <p>Hoy: despertar ({@code PlayerWakeUpEvent}) -&gt; {@link SleepChakraHandler}; fin de comer
 * ({@code LivingEntityUseItemEvent.Finish}) -&gt; {@link FoodChakraHandler}. El equivalente Fabric vive en
 * {@code DojutsuFabricServerEvents} (dormir) y un mixin (comer). Se registra desde el entrypoint
 * {@link DojutsuNeoForge}.
 */
public final class DojutsuNeoForgeServerEvents {

    private DojutsuNeoForgeServerEvents() {
    }

    /** Registra los listeners en el bus de eventos del juego. Llamar durante la inicializacion del mod. */
    public static void register() {
        NeoForge.EVENT_BUS.addListener((PlayerWakeUpEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SleepChakraHandler.onWakeUp(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingEntityUseItemEvent.Finish event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                FoodChakraHandler.onFinishEating(player, event.getItem());
            }
        });
    }
}
