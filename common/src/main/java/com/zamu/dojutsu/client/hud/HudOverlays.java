package com.zamu.dojutsu.client.hud;

import com.zamu.dojutsu.client.ClientNinjaData;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Registro y guards comunes de los overlays del HUD ({@code [ADD/10 client]}). <b>Solo cliente:</b> se invoca
 * desde {@code DojutsuMod.initClient()}.
 *
 * <p>Una unica suscripcion a {@code ClientGuiEvent.RENDER_HUD} (cross-loader) dibuja todos los overlays, con
 * los guards en un solo sitio: se oculta con F1 ({@code hideGui}), en espectador y antes del primer sync.
 */
public final class HudOverlays {

    private HudOverlays() {
    }

    /** Suscribe el dibujado del HUD. Llamar una vez en la inicializacion de cliente. */
    public static void register() {
        ClientGuiEvent.RENDER_HUD.register(HudOverlays::onRenderHud);
    }

    private static void onRenderHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return; // F1: el jugador oculto la interfaz
        }
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            return; // sin jugador o en espectador
        }
        if (!ClientNinjaData.hasData()) {
            return; // aun no llego el primer sync: no dibujar valores falsos
        }
        ChakraHudOverlay.render(graphics, minecraft.font);
        RankHudOverlay.render(graphics, minecraft.font);
        AbilityBarOverlay.render(graphics, minecraft.font); // cd del jutsu equipado (DJT-081)
    }
}
