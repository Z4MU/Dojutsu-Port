package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientEyeMenu;
import com.zamu.dojutsu.network.AwakenEternalPayload;
import com.zamu.dojutsu.network.OpenEyeMenuPayload;
import com.zamu.dojutsu.network.SelectEyeAbilityPayload;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Pantalla de seleccion de la habilidad activa del ojo (DJT-117): reemplaza el "hotbar-as-selector" del
 * datapack (sneak sobre un slot mientras el ojo esta activo). <b>Solo cliente.</b>
 *
 * <p>Al abrirse pide la vista al servidor ({@link OpenEyeMenuPayload}); mientras llega, muestra lo ultimo
 * cacheado. La lista de habilidades viene del servidor ({@link ClientEyeMenu}, el {@code EyeRegistry} es
 * data-driven y server-side). Cada boton solo <b>envia la intencion</b> ({@link SelectEyeAbilityPayload}); el
 * servidor valida contra el ojo activo real y responde con una vista refrescada, con la que la pantalla se
 * reconstruye (nueva seleccion marcada). Si no hay ojo activo, muestra un aviso y ningun boton.
 */
public final class AbilitySelectScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private static final int LIST_TOP = 50;

    /** Version de {@link ClientEyeMenu} con la que se construyeron los botones (para detectar refrescos). */
    private int builtVersion = Integer.MIN_VALUE;
    private boolean requested;

    public AbilitySelectScreen() {
        super(Component.translatable("screen.dojutsu.eye_abilities"));
    }

    @Override
    protected void init() {
        if (!requested) {
            PlatformServices.NETWORK.sendToServer(new OpenEyeMenuPayload()); // pide la vista una vez al abrir
            requested = true;
        }
        buildContent();
    }

    /** (Re)construye los botones desde la cache; deja constancia de la version usada. */
    private void buildContent() {
        clearWidgets();
        builtVersion = ClientEyeMenu.version();
        if (ClientEyeMenu.hasActiveEye()) {
            List<String> abilities = ClientEyeMenu.abilities();
            String selected = ClientEyeMenu.selectedAbility();
            for (int i = 0; i < abilities.size(); i++) {
                addAbilityButton(abilities.get(i), abilities.get(i).equals(selected), i);
            }
            addAwakenEternalButton(abilities.size());
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - BUTTON_WIDTH / 2, this.height - 40, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
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
                .bounds(this.width / 2 - BUTTON_WIDTH / 2, LIST_TOP + row * ROW_GAP + 8, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        button.active = ready;
        addRenderableWidget(button);
    }

    /** Envia la intencion y cierra para que el aviso del servidor (chat) sea visible. */
    private void awakenEternal() {
        PlatformServices.NETWORK.sendToServer(new AwakenEternalPayload());
        onClose();
    }

    private void addAbilityButton(String abilityId, boolean isSelected, int row) {
        Component label = Component.translatable("ability.dojutsu." + abilityId);
        if (isSelected) {
            label = label.copy().append(Component.literal("  *")); // marca de seleccionada (el boton queda inactivo)
        }
        Button button = Button.builder(label, b -> select(abilityId))
                .bounds(this.width / 2 - BUTTON_WIDTH / 2, LIST_TOP + row * ROW_GAP, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        button.active = !isSelected; // la ya seleccionada no se re-selecciona
        addRenderableWidget(button);
    }

    /** Envia la intencion; la vista refrescada llegara por S2C y {@link #tick()} reconstruira la pantalla. */
    private void select(String abilityId) {
        PlatformServices.NETWORK.sendToServer(new SelectEyeAbilityPayload(abilityId));
    }

    @Override
    public void tick() {
        if (ClientEyeMenu.version() != builtVersion) {
            buildContent(); // llego una vista nueva (respuesta a abrir o a seleccionar): reconstruir
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        if (!ClientEyeMenu.hasActiveEye()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("screen.dojutsu.eye_abilities.none"),
                    this.width / 2, this.height / 2, 0xAAAAAA);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
