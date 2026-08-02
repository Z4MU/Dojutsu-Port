package com.zamu.dojutsu.empowerment;

import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.domain.event.ModeToggledEvent;
import com.zamu.dojutsu.domain.mode.ModeContext;
import com.zamu.dojutsu.domain.mode.ToggleableMode;
import com.zamu.dojutsu.network.NinjaSync;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Arnes compartido de los <b>modos activables</b> (F6/DJT-130, abre `EP-TOGGLE-FRAMEWORK`): mantiene un
 * {@link ToggleableMode} <b>por jugador</b>, lo tickea, detecta cuando <b>cae solo</b> (coste impagable) y emite
 * el {@link ModeToggledEvent} y el sync en un unico sitio.
 *
 * <p><b>Salda deuda real, no especulativa:</b> {@code SusanooService} (F5/DJT-120) y
 * {@code TenseiganChakraModeService} (fleco F5) ya repetian este bucle palabra por palabra —
 * "coge el modo, si no esta activo no hagas nada, tickea, y si dejo de estar activo limpia y avisa". Los modos de
 * F6 (sage, sellos, puertas, SOAH) serian el tercero al septimo. El framework {@link ToggleableMode} (F1/DJT-012)
 * define el <b>ciclo</b>; esto define el <b>hospedaje</b>: quien guarda el modo, cuando se tickea y que pasa al
 * caer.
 *
 * <p><b>Lo que NO hace</b> (a proposito): no sabe de chakra, fatiga ni efectos — eso vive en el
 * {@link ModeContext} de cada modo, como hasta ahora. Aqui solo esta lo que era identico en los dos servicios.
 *
 * @param <M> tipo concreto de modo
 * @param <C> contexto que necesita ese modo
 */
public final class ToggleableModeService<M extends ToggleableMode<C>, C extends ModeContext> {

    /** Id del modo para {@link ModeToggledEvent} ({@code "susanoo"}, {@code "sage"}, {@code "eight_gates"}…). */
    private final String modeId;

    /** Crea el modo la primera vez que un jugador lo enciende. */
    private final Supplier<M> modeFactory;

    /** Construye el contexto de un jugador (lo unico que sabe de Minecraft). */
    private final Function<ServerPlayer, C> contextFactory;

    /** Limpieza extra cuando el modo <b>cae solo</b> (no paso por {@code deactivate}); opcional. */
    private final OnFall<C> onFall;

    private final Map<UUID, M> modes = new ConcurrentHashMap<>();

    /** Limpieza propia de cada modo cuando cae por coste impagable (el avatar del Susanoo, el vuelo del TCM…). */
    @FunctionalInterface
    public interface OnFall<C extends ModeContext> {

        /** Invocado tras caer el modo, con el contexto del jugador. */
        void onFall(ServerPlayer player, C context);
    }

    public ToggleableModeService(String modeId, Supplier<M> modeFactory,
            Function<ServerPlayer, C> contextFactory, OnFall<C> onFall) {
        this.modeId = modeId;
        this.modeFactory = modeFactory;
        this.contextFactory = contextFactory;
        this.onFall = onFall;
    }

    /** Variante sin limpieza extra al caer. */
    public ToggleableModeService(String modeId, Supplier<M> modeFactory, Function<ServerPlayer, C> contextFactory) {
        this(modeId, modeFactory, contextFactory, (player, context) -> { });
    }

    /**
     * Tickea el modo del jugador si esta activo. Si el modo <b>cae</b> durante el tick (el framework lo apago por
     * coste impagable), corre la limpieza del modo, emite {@link ModeToggledEvent} y sincroniza.
     *
     * @return {@code true} si el modo seguia activo al terminar el tick
     */
    public boolean tick(ServerPlayer player) {
        M mode = modes.get(player.getUUID());
        if (mode == null || !mode.isActive()) {
            return false; // caso comun: sin trabajo por tick
        }
        C context = contextFactory.apply(player);
        mode.tick(context);
        if (!mode.isActive()) {
            onFall.onFall(player, context); // cayo: el apagado no paso por deactivate
            publish(player, false);
        }
        NinjaSync.sendDelta(player); // empuja el recurso consumido al HUD
        return mode.isActive();
    }

    /**
     * Alterna el modo del jugador (lo crea si es su primera vez) y emite el evento.
     *
     * @return {@code true} si quedo encendido
     */
    public boolean toggle(ServerPlayer player) {
        M mode = modeOf(player);
        boolean active = mode.toggle(contextFactory.apply(player));
        publish(player, active);
        return active;
    }

    /** Intenta activar el modo (sin apagarlo si ya lo estaba); {@code true} si se activo en esta llamada. */
    public boolean activate(ServerPlayer player) {
        M mode = modeOf(player);
        if (mode.isActive() || !mode.activate(contextFactory.apply(player))) {
            return false;
        }
        publish(player, true);
        return true;
    }

    /** Apaga el modo del jugador si estaba activo (limpieza via {@code onDeactivate} del propio modo). */
    public void deactivate(ServerPlayer player) {
        M mode = modes.get(player.getUUID());
        if (mode == null || !mode.isActive()) {
            return;
        }
        mode.deactivate(contextFactory.apply(player));
        publish(player, false);
    }

    /** Indica si el jugador tiene el modo activo. */
    public boolean isActive(UUID playerId) {
        M mode = modes.get(playerId);
        return mode != null && mode.isActive();
    }

    /** El modo del jugador (creandolo si aun no existe): para que el servicio concreto lea su estado. */
    public M modeOf(ServerPlayer player) {
        return modes.computeIfAbsent(player.getUUID(), id -> modeFactory.get());
    }

    /**
     * Olvida el modo de un jugador (al desconectar; los modos no sobreviven a la sesion). Devuelve el modo que
     * habia, por si el servicio concreto necesita limpiar algo suyo.
     */
    public M forget(UUID playerId) {
        return modes.remove(playerId);
    }

    private void publish(ServerPlayer player, boolean active) {
        DomainEvents.bus().publish(new ModeToggledEvent(player.getUUID(), modeId, active));
    }
}
