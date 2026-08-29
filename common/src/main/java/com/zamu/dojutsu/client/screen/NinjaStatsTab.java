package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.network.MenuActionPayload;
import com.zamu.dojutsu.network.ProgressionSnapshot;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Pestaña "Ninja" del menu (F11/DJT-236): chakra, rango, puntos y niveles de habilidad, con el gasto de
 * puntos y el acceso al editor de HUD. Es el contenido que hasta ahora era la pantalla entera.
 *
 * <p>El boton "+" solo <b>solicita</b> el gasto con {@link MenuActionPayload}; el servidor valida (DJT-042) y,
 * si procede, aplica y sincroniza: la interfaz se refresca cuando llega el sync, no antes.
 *
 * <p>No declara {@code observedVersion}: se dibuja leyendo {@link ClientNinjaData} en cada frame, asi que un
 * sync nuevo se ve solo, sin reconstruir widgets.
 */
final class NinjaStatsTab implements MenuTab {

    private static final int ROW_HEIGHT = 22;
    private static final int PLUS_SIZE = 20;

    private final List<Button> plusButtons = new ArrayList<>();
    private NinjaMenuScreen screen;
    private int listTop;

    @Override
    public Component title() {
        return Component.translatable("screen.dojutsu.tab.ninja");
    }

    @Override
    public void init(NinjaMenuScreen screen) {
        this.screen = screen;
        plusButtons.clear();
        listTop = screen.contentTop() + 46; // debajo de las tres lineas de cabecera
        int left = screen.width / 2 - 80;
        for (Skill skill : Skill.values()) {
            int y = listTop + skill.ordinal() * ROW_HEIGHT;
            Button plus = Button.builder(Component.literal("+"), button -> requestSpend(skill))
                    .bounds(left + 140, y - 6, PLUS_SIZE, PLUS_SIZE)
                    .tooltip(Tooltip.create(Component.translatable("screen.dojutsu.spend_tooltip", label(skill))))
                    .build();
            screen.addTabWidget(plus);
            plusButtons.add(plus);
        }
        // Editor de HUD (F11/DJT-233): preferencia de interfaz, no gameplay; no manda nada al servidor.
        screen.addTabWidget(Button.builder(Component.translatable("screen.dojutsu.hud_editor"),
                        button -> Minecraft.getInstance().setScreen(new HudEditorScreen(screen)))
                .bounds(screen.width / 2 - 130, screen.height - 28, 100, 20)
                .build());
    }

    @Override
    public void tick() {
        // Solo habilita "+" si hay puntos observados. La autoridad sigue siendo el servidor (esto es UX).
        boolean hasPoints = ClientNinjaData.progression().skillPoints() > 0;
        for (Button plus : plusButtons) {
            plus.active = hasPoints;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var font = screen.font();
        int centerX = screen.width / 2;
        int top = listTop - 46;

        boolean synced = ClientNinjaData.hasData();
        ProgressionSnapshot progression = ClientNinjaData.progression();

        graphics.drawCenteredString(font, Component.literal(synced
                ? "Chakra: " + ClientNinjaData.chakra() + " / " + ClientNinjaData.maxChakra()
                : "Chakra: --"), centerX, top, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(synced
                ? "Rango: " + progression.rank().title() + "  (" + progression.ninjaXp() + " XP ninja)"
                : "Rango: --"), centerX, top + 12, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(
                "Skill Points: " + progression.skillPoints()), centerX, top + 24, 0xFFD700);

        int left = centerX - 80;
        for (Skill skill : Skill.values()) {
            int y = listTop + skill.ordinal() * ROW_HEIGHT;
            graphics.drawString(font, label(skill), left, y, 0xFFFFFF);
            graphics.drawString(font, Component.literal("Nv " + progression.levelOf(skill)),
                    left + 100, y, 0xAAAAAA);
        }
    }

    /** Envia la <b>intencion</b> de gastar 1 punto. No modifica nada en cliente: espera el sync del servidor. */
    private void requestSpend(Skill skill) {
        PlatformServices.NETWORK.sendToServer(new MenuActionPayload(
                MenuActionPayload.Action.SPEND_SKILL, skill.name().toLowerCase(Locale.ROOT), 1));
    }

    private static Component label(Skill skill) {
        return Component.translatable("skill.dojutsu." + skill.name().toLowerCase(Locale.ROOT));
    }
}
