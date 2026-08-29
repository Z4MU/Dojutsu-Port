package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientEyeMenu;
import com.zamu.dojutsu.network.AwakenEternalPayload;
import com.zamu.dojutsu.network.OpenEyeMenuPayload;
import com.zamu.dojutsu.network.SelectEyeAbilityPayload;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Pestaña "Ojo" del menu (F11/DJT-236): seleccion de la habilidad activa del dojutsu. Reemplaza al
 * "hotbar-as-selector" del datapack (sneak sobre un slot con el ojo activo) y, desde DJT-236, a la pantalla
 * suelta que tenia tecla propia ({@code N}).
 *
 * <p>Al mostrarse por primera vez pide la vista al servidor ({@link OpenEyeMenuPayload}); mientras llega,
 * muestra lo ultimo cacheado. La lista de habilidades viene del servidor ({@link ClientEyeMenu}: el
 * {@code EyeRegistry} es data-driven y server-side). Cada boton solo <b>envia la intencion</b>
 * ({@link SelectEyeAbilityPayload}); el servidor valida contra el ojo activo real y responde con una vista
 * refrescada, y el contenedor reconstruye al ver subir {@link #observedVersion()}.
 */
final class EyeAbilitiesTab implements MenuTab {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 24;

    private NinjaMenuScreen screen;

    @Override
    public Component title() {
        return Component.translatable("screen.dojutsu.tab.eye");
    }

    @Override
    public int observedVersion() {
        return ClientEyeMenu.version();
    }

    @Override
    public void request() {
        PlatformServices.NETWORK.sendToServer(new OpenEyeMenuPayload());
    }

    @Override
    public void init(NinjaMenuScreen screen) {
        this.screen = screen;
        if (!ClientEyeMenu.hasActiveEye()) {
            return; // sin ojo activo no hay nada que elegir; render() lo explica
        }
        List<String> abilities = ClientEyeMenu.abilities();
        String selected = ClientEyeMenu.selectedAbility();
        for (int i = 0; i < abilities.size(); i++) {
            addAbilityButton(abilities.get(i), abilities.get(i).equals(selected), i);
        }
        addAwakenEternalButton(abilities.size());
    }

    /**
     * Boton de despertar el EMS bajo la lista (F10, arreglo de la verificacion en partida).
     *
     * <p>Antes se mostraba con <b>cualquier</b> ojo activo, porque el cliente no sabia si el ojo era
     * eternalizable —el {@code EyeRegistry} vive en el servidor— y se dejaba que el servidor rechazara. En
     * partida eso era un boton de «despertar» permanente sobre el Rinnegan y el Rinne Sharingan, que <b>no
     * tienen forma eterna</b>, y ninguna pista de que hacia falta en los que si.
     *
     * <p>Ahora el servidor manda el estado junto con la vista: el boton <b>no aparece</b> donde no procede, y
     * cuando procede pero aun no se cumple sale <b>desactivado y diciendo que falta</b>. El servidor sigue
     * validando: esto es presentacion, no una puerta.
     */
    private void addAwakenEternalButton(int row) {
        String status = ClientEyeMenu.eternalStatus();
        if ("NO_ETERNAL_EYE".equals(status) || "ALREADY_ETERNAL".equals(status)) {
            return; // este ojo no puede ser eterno, o ya lo es
        }
        boolean ready = "AWAKENED".equals(status);
        Component label = ready
                ? Component.translatable("screen.dojutsu.awaken_eternal")
                : Component.translatable("screen.dojutsu.awaken_eternal.blocked",
                        Component.translatable("eternal.dojutsu.hint." + status.toLowerCase(Locale.ROOT)));
        Button button = Button.builder(label, b -> awakenEternal())
                .bounds(screen.width / 2 - BUTTON_WIDTH / 2, rowY(row) + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        button.active = ready;
        screen.addTabWidget(button);
    }

    /** Envia la intencion y cierra el menu para que el aviso del servidor (chat) sea visible. */
    private void awakenEternal() {
        PlatformServices.NETWORK.sendToServer(new AwakenEternalPayload());
        Minecraft.getInstance().setScreen(null);
    }

    private void addAbilityButton(String abilityId, boolean isSelected, int row) {
        Component label = Component.translatable("ability.dojutsu." + abilityId);
        if (isSelected) {
            label = label.copy().append(Component.literal("  *")); // marca de seleccionada (el boton queda inactivo)
        }
        Button button = Button.builder(label, b -> select(abilityId))
                .bounds(screen.width / 2 - BUTTON_WIDTH / 2, rowY(row), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        button.active = !isSelected; // la ya seleccionada no se re-selecciona
        screen.addTabWidget(button);
    }

    private int rowY(int row) {
        return screen.contentTop() + row * ROW_GAP;
    }

    /** Envia la intencion; la vista refrescada llegara por S2C y el contenedor reconstruira la pestaña. */
    private void select(String abilityId) {
        PlatformServices.NETWORK.sendToServer(new SelectEyeAbilityPayload(abilityId));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!ClientEyeMenu.hasActiveEye()) {
            graphics.drawCenteredString(screen.font(),
                    Component.translatable("screen.dojutsu.eye_abilities.none"),
                    screen.width / 2, screen.contentTop() + 20, 0xAAAAAA);
        }
    }
}
