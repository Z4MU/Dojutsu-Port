package com.zamu.dojutsu.chakra;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.network.NinjaSync;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reposicion de chakra por dormir ({@code chakra/sleep/*} del datapack, {@code [S02]}). Al despertar de una
 * noche completa, deja el chakra al maximo y ejecuta los <b>handlers de despertar</b> registrados por otros
 * modulos (p. ej. el reset de fatiga ocular de {@code EyeStrainService}, S05).
 *
 * <p>Logica loader-agnostica: la <b>suscripcion</b> al evento nativo de despertar la hace cada adaptador
 * (NeoForge {@code PlayerWakeUpEvent}; Fabric {@code EntitySleepEvents.STOP_SLEEPING}), porque Architectury
 * 13.0.8 no ofrece un evento de sueno cross-loader (ver {@code [ADD/05]}: se abstrae en la plataforma).
 *
 * <p>Reemplaza el bucle {@code schedule 1s} del datapack por el evento real (sin polling). Sustituye el estado
 * de sueno {@code d.sleep*} y el advancement {@code slept_in_bed}.
 */
public final class SleepChakraHandler {

    /** Duracion de medio dia en ticks: la mañana es {@code timeOfDay < 12000} (fiel a {@code is_early_day}). */
    private static final long HALF_DAY_TICKS = 12000L;

    /** Ticks de un dia completo de Minecraft. */
    private static final long DAY_TICKS = 24000L;

    /**
     * Handlers a ejecutar al despertar de una noche completa, registrados por otros modulos (OCP, patron de los
     * proveedores de factor de {@code RegenService}): {@code chakra} no depende de {@code dojutsu}, es
     * {@code EyeStrainService} quien se registra aqui (dependencia descendente). Deben ser puros sobre
     * {@link PlayerNinjaData}.
     */
    private static final List<Consumer<PlayerNinjaData>> WAKE_HANDLERS = new CopyOnWriteArrayList<>();

    private SleepChakraHandler() {
    }

    /**
     * Registra un handler de despertar (una vez, en el arranque, desde el modulo que lo aporta). Se ejecuta tras
     * reponer el chakra cuando el jugador despierta de una noche completa.
     */
    public static void registerWakeHandler(Consumer<PlayerNinjaData> handler) {
        WAKE_HANDLERS.add(handler);
    }

    /**
     * Handler de despertar. Si es un despertar a la mañana (noche completa, fiel al {@code is_early_day} del
     * datapack) restaura el chakra al maximo; un sueno interrumpido de noche no repone nada.
     *
     * <p>La decision usa la <b>hora del mundo</b> ({@link net.minecraft.world.level.Level#getDayTime()}), que al
     * despertar ya esta avanzada a la mañana, y <b>no</b> {@code isDay()}: el brillo del cielo ({@code skyDarken})
     * se calcula al inicio del tick y todavia refleja la noche cuando el salto de sueno despierta al jugador en
     * ese mismo tick, por lo que {@code isDay()} daria un falso negativo.
     */
    public static void onWakeUp(ServerPlayer player) {
        long timeOfDay = Math.floorMod(player.level().getDayTime(), DAY_TICKS);
        boolean morning = timeOfDay < HALF_DAY_TICKS;
        if (!morning) {
            return; // sueno interrumpido (sigue de noche): sin restauracion
        }
        PlayerNinjaData data = NinjaData.get(player);
        ChakraService.restoreFull(data); // sleep/wake_up: chakra al maximo
        for (Consumer<PlayerNinjaData> handler : WAKE_HANDLERS) {
            handler.accept(data); // p. ej. EyeStrainService.resetStrain (S05); eg_strain de F6 se sumara aqui
        }
        NinjaSync.sendDelta(player);
    }
}
