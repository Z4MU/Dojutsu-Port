package com.zamu.dojutsu.client.hud;

import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.items.JutsuComponent;
import com.zamu.dojutsu.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Overlay del cooldown del jutsu equipado ({@code [ADD/10 client]}, {@code [S06]}, F4/DJT-081).
 * <b>Solo cliente.</b> Muestra el nombre del jutsu de la mano principal y una barra que decrece con
 * su cooldown restante.
 *
 * <p><b>El jutsu equipado se lee del componente del ItemStack</b> (sincronizado por vanilla), no del
 * sync del mod; el <b>cooldown</b> viene de {@link ClientNinjaData} (alimentado por el sync) y el
 * restante se calcula con el tick de mundo del cliente ({@code level.getGameTime}, sincronizado con
 * el servidor). El cliente nunca decide gameplay: solo pinta lo observado.
 *
 * <p>Se dibuja con primitivas (sin texturas todavia): los assets reales llegan con {@code EP-ASSETS}.
 */
public final class AbilityBarOverlay {

    private static final int X = 10;
    private static final int Y = 34;
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
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        ItemStack held = minecraft.player.getMainHandItem();
        JutsuComponent component = held.get(ModDataComponents.JUTSU.get());
        if (component == null) {
            return; // no hay jutsu equipado: el overlay no ocupa espacio
        }

        long now = minecraft.level.getGameTime();
        long remaining = ClientNinjaData.remainingCooldown(component.jutsuId(), now);
        boolean ready = remaining <= 0;

        // Fondo + barra: llena cuando esta listo, decreciendo con el cooldown.
        graphics.fill(X - 1, Y - 1, X + BAR_WIDTH + 1, Y + BAR_HEIGHT + 1, COLOR_BORDER);
        graphics.fill(X, Y, X + BAR_WIDTH, Y + BAR_HEIGHT, COLOR_BACKGROUND);
        int filled = ready ? BAR_WIDTH : BAR_WIDTH - emptyPixels(remaining);
        graphics.fill(X, Y, X + filled, Y + BAR_HEIGHT, ready ? COLOR_READY : COLOR_COOLDOWN);

        // Texto: nombre del jutsu + "Ready" o los segundos restantes.
        Component label = Component.translatable("jutsu.dojutsu." + component.jutsuId());
        String status = ready ? " (Ready)" : " (" + tenths(remaining) + "s)";
        graphics.drawString(font, label.getString() + status, X + BAR_WIDTH + 6, Y - 2,
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
