package com.zamu.dojutsu.client.hud;

import com.zamu.dojutsu.client.ClientNinjaData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Overlay del cooldown del jutsu <b>seleccionado</b> ({@code [ADD/10 client]}, {@code [S06]}, F4/DJT-081).
 * <b>Solo cliente.</b> Muestra su nombre y una barra que decrece con su cooldown restante.
 *
 * <p><b>F11/DJT-232:</b> antes leia el componente del item de la mano principal, porque el jutsu que
 * lanzabas era el que llevabas. Al retirarse el item como via de lanzamiento eso paso a ser una mentira:
 * habria pintado el cooldown de algo que la tecla no lanza, y nada mientras tuvieras uno seleccionado. Ahora
 * sigue a {@link ClientNinjaData#selectedJutsu()}, o sea a lo que la tecla lanza de verdad.
 *
 * <p>Tanto la seleccion como el <b>cooldown</b> vienen del sync ({@link ClientNinjaData}); el restante se
 * calcula con el tick de mundo del cliente ({@code level.getGameTime}, sincronizado con el servidor). El
 * cliente nunca decide gameplay: solo pinta lo observado.
 *
 * <p>Se dibuja con primitivas (sin texturas todavia): los assets reales llegan con {@code EP-ASSETS}.
 */
public final class AbilityBarOverlay {


    private static final int BAR_WIDTH = 80;
    private static final int BAR_HEIGHT = 4;

    private static final int COLOR_BORDER = 0xFF303030;
    private static final int COLOR_BACKGROUND = 0xAA000000;
    private static final int COLOR_READY = 0xFF00E676;    // verde: listo
    private static final int COLOR_COOLDOWN = 0xFFEF5350;  // rojo: en cooldown

    private AbilityBarOverlay() {
    }

    /** Dibuja el indicador si hay un jutsu equipado. Los guards comunes los aplica {@link HudOverlays}. */
    public static void render(GuiGraphics graphics, Font font) {
        HudPlacement placement = HudLayout.get(HudElement.ABILITY_BAR);
        if (!placement.visible()) {
            return;
        }
        render(graphics, font,
                placement.resolveX(graphics.guiWidth(), HudElement.ABILITY_BAR.width()),
                placement.resolveY(graphics.guiHeight(), HudElement.ABILITY_BAR.height()));
    }

    /** Dibuja la barra en una posicion dada. Lo usa tambien el editor de HUD para la vista previa. */
    public static void render(GuiGraphics graphics, Font font, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        String jutsuId = ClientNinjaData.selectedJutsu().orElse(null);
        if (jutsuId == null) {
            return; // sin jutsu seleccionado: el overlay no ocupa espacio
        }

        long now = minecraft.level.getGameTime();
        long remaining = ClientNinjaData.remainingCooldown(jutsuId, now);
        boolean ready = remaining <= 0;

        // Fondo + barra: llena cuando esta listo, decreciendo con el cooldown.
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, COLOR_BORDER);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, COLOR_BACKGROUND);
        int filled = ready ? BAR_WIDTH : BAR_WIDTH - emptyPixels(remaining);
        graphics.fill(x, y, x + filled, y + BAR_HEIGHT, ready ? COLOR_READY : COLOR_COOLDOWN);

        // Texto: nombre del jutsu + "Ready" o los segundos restantes.
        Component label = Component.translatable("jutsu.dojutsu." + jutsuId);
        String status = ready ? " (Ready)" : " (" + tenths(remaining) + "s)";
        graphics.drawString(font, label.getString() + status, x + BAR_WIDTH + 6, y - 2,
                ready ? 0xFFFFFF : 0xFFBBBB);
    }

    /**
     * Pixeles vacios de la barra en funcion del cooldown restante. Sin conocer el cooldown total (no viaja),
     * se usa una escala fija (200 ticks = barra vacia) que acota el segmento: la barra se rellena a medida que
     * expira el cd. Indicador cualitativo hasta que el cd total/assets viajen (deuda menor documentada).
     */
    private static int emptyPixels(long remaining) {
        long scale = 200L; // 10 s de referencia visual
        return (int) Math.min(BAR_WIDTH, remaining * BAR_WIDTH / scale);
    }

    /** Formatea los ticks restantes como segundos con un decimal (20 ticks = 1 s). */
    private static String tenths(long ticks) {
        long tenths = (ticks * 10 + 19) / 20; // redondeo hacia arriba a decimas de segundo
        return (tenths / 10) + "." + (tenths % 10);
    }
}
