package com.zamu.dojutsu.fabric;

import com.zamu.dojutsu.chakra.SleepChakraHandler;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * Suscripcion de eventos <b>nativos de Fabric</b> (fabric-api) del lado servidor que Architectury no expone
 * cross-loader (ver {@code [ADD/05]}). Delegan en los handlers comunes de {@code com.zamu.dojutsu.chakra}.
 *
 * <p>Hoy: fin del sueno ({@code EntitySleepEvents.STOP_SLEEPING}) -&gt; {@link SleepChakraHandler} (el propio
 * handler filtra el despertar de noche completa). El equivalente NeoForge vive en
 * {@code DojutsuNeoForgeServerEvents}. Se registra desde el entrypoint {@link DojutsuFabric}.
 */
public final class DojutsuFabricServerEvents {

    private DojutsuFabricServerEvents() {
    }

    /** Registra los listeners de fabric-api. Llamar durante la inicializacion del mod. */
    public static void register() {
        EntitySleepEvents.STOP_SLEEPING.register((entity, sleepingPos) -> {
            if (entity instanceof ServerPlayer player) {
                SleepChakraHandler.onWakeUp(player);
            }
        });
    }
}
