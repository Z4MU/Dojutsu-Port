package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.network.MenuActionPayload;
import com.zamu.dojutsu.network.ProgressionSnapshot;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Menu ninja: GUI real que reemplaza el "fake-GUI" por {@code title}/{@code actionbar} + hotbar secuestrado del
 * datapack ({@code [S13]}). <b>Solo cliente.</b>
 *
 * <p>Reglas ({@code [ADD/07]}): lee <b>solo</b> de {@link ClientNinjaData} (cache read-only) y <b>nunca</b>
 * aplica gameplay. El boton "+" solo <b>solicita</b> el gasto con {@link MenuActionPayload}; el servidor valida
 * (DJT-042) y, si procede, aplica y sincroniza: la UI se refresca cuando llega el sync, no antes.
 *
 * <p>Version minima y enriquecible (RM-2): info + lista de habilidades. El gasto es de 1 punto por clic.
 */
public final class NinjaMenuScreen extends Screen {

    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 92;
    private static final int PLUS_SIZE = 20;

    private final List<Button> plusButtons = new ArrayList<>();

    public NinjaMenuScreen() {
        super(Component.translatable("screen.dojutsu.ninja_menu"));
    }

    @Override
    protected void init() {
        plusButtons.clear();
        int left = this.width / 2 - 80;
        for (Skill skill : Skill.values()) {
            int y = LIST_TOP + skill.ordinal() * ROW_HEIGHT;
            Button plus = Button.builder(Component.literal("+"), button -> requestSpend(skill))
                    .bounds(left + 140, y - 6, PLUS_SIZE, PLUS_SIZE)
                    .tooltip(Tooltip.create(Component.translatable("screen.dojutsu.spend_tooltip", label(skill))))
                    .build();
            addRenderableWidget(plus);
            plusButtons.add(plus);
        }
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
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);

        boolean synced = ClientNinjaData.hasData();
        ProgressionSnapshot progression = ClientNinjaData.progression();

        graphics.drawCenteredString(this.font, Component.literal(synced
                ? "Chakra: " + ClientNinjaData.chakra() + " / " + ClientNinjaData.maxChakra()
                : "Chakra: --"), centerX, 42, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal(synced
                ? "Rango: " + progression.rank().title() + "  (" + progression.ninjaXp() + " XP ninja)"
                : "Rango: --"), centerX, 54, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Component.literal(
                "Skill Points: " + progression.skillPoints()), centerX, 66, 0xFFD700);

        int left = centerX - 80;
        for (Skill skill : Skill.values()) {
            int y = LIST_TOP + skill.ordinal() * ROW_HEIGHT;
            graphics.drawString(this.font, label(skill), left, y, 0xFFFFFF);
            graphics.drawString(this.font, Component.literal("Nv " + progression.levelOf(skill)),
                    left + 100, y, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false; // no pausa el juego en un solo jugador
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
