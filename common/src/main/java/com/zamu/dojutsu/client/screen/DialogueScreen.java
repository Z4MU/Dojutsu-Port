package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.domain.story.SageEncounter;
import com.zamu.dojutsu.domain.story.SagePath;
import com.zamu.dojutsu.network.PathChoicePayload;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dialogo del primer encuentro con el Sabio y eleccion de camino (F9/DJT-200). <b>Solo cliente.</b>
 *
 * <p>Reemplaza el {@code tellraw} clicable sobre un armor stand narrador: el mismo contenido —las tres lineas, las
 * tres opciones y sus {@code hoverEvent} explicando que da cada mitad— pero como pantalla. Cada boton solo
 * <b>envia la intencion</b> ({@link PathChoicePayload}); el servidor revalida el encuentro antes de conceder nada,
 * asi que un cliente parcheado no consigue el chakra de los Seis Caminos ({@code [ADD/07]}).
 *
 * <p><b>Cerrar con Esc no penaliza.</b> No hay respuesta por defecto: el encuentro queda anotado y el sabio
 * volvera una segunda vez. Rechazar explicitamente tampoco cierra la puerta ({@code no_path} devuelve la etapa a
 * cero); por eso el tercer boton es una opcion legitima y no una trampa.
 */
public final class DialogueScreen extends Screen {

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 24;

    /** Lineas del primer dialogo ({@code dialogue/first/1..3}); la 4a era el menu de opciones. */
    private static final int LINES = 3;

    private static final int LINE_TOP = 50;
    private static final int LINE_HEIGHT = 14;

    private final SageEncounter sage;

    public DialogueScreen(SageEncounter sage) {
        super(Component.translatable("story.dojutsu.sage." + sage.id()));
        this.sage = sage;
    }

    @Override
    protected void init() {
        List<SagePath> paths = List.of(SagePath.YIN, SagePath.YANG, SagePath.NONE);
        int top = this.height - 40 - (paths.size() - 1) * ROW_GAP;
        for (int i = 0; i < paths.size(); i++) {
            addPathButton(paths.get(i), top + i * ROW_GAP);
        }
    }

    /** Boton de un camino, con su tooltip; el de Yin nombra el ojo del sabio (Rinnegan vs Tenseigan). */
    private void addPathButton(SagePath path, int y) {
        Component eye = Component.translatable("eye.dojutsu." + sage.yinEye());
        Component label = path == SagePath.YIN
                ? Component.translatable("story.dojutsu.path.yin")
                : Component.translatable("story.dojutsu.path." + path.id());
        Component hint = path == SagePath.YIN
                ? Component.translatable("story.dojutsu.path.yin.hint", eye)
                : Component.translatable("story.dojutsu.path." + path.id() + ".hint");
        addRenderableWidget(Button.builder(label, b -> choose(path))
                .tooltip(Tooltip.create(hint))
                .bounds(this.width / 2 - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    /** Envia la intencion y cierra, para que la respuesta del sabio se lea en el chat. */
    private void choose(SagePath path) {
        PlatformServices.NETWORK.sendToServer(new PathChoicePayload(path));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, sage.nameColor());
        for (int i = 0; i < LINES; i++) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("story.dojutsu." + sage.id() + ".first." + i),
                    this.width / 2, LINE_TOP + i * LINE_HEIGHT, 0xDDDDDD);
        }
        graphics.drawCenteredString(this.font,
                Component.translatable("story.dojutsu.hover_note").withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2, LINE_TOP + LINES * LINE_HEIGHT + 10, 0x555555);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
