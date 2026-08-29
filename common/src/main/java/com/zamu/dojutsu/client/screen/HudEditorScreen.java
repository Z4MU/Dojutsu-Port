package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.hud.AbilityBarOverlay;
import com.zamu.dojutsu.client.hud.ChakraHudOverlay;
import com.zamu.dojutsu.client.hud.HudAnchor;
import com.zamu.dojutsu.client.hud.HudElement;
import com.zamu.dojutsu.client.hud.HudLayout;
import com.zamu.dojutsu.client.hud.HudPlacement;
import com.zamu.dojutsu.client.hud.RankHudOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Editor de la colocacion del HUD (F11/DJT-233): el jugador arrastra cada elemento donde quiera y lo ve
 * dibujado en su sitio mientras lo mueve.
 *
 * <p><b>Solo cliente y sin red.</b> Donde cada uno pone su barra de chakra no es estado de juego: no se manda
 * al servidor ni se guarda en la partida (regla 13). Se escribe a {@code config/dojutsu-hud.properties} al
 * cerrar.
 *
 * <h2>El anclaje se elige solo</h2>
 * Pedirle al jugador que entienda "anclaje" antes de mover nada seria hacerle explicar el problema tecnico en
 * vez de resolverselo. Al soltar, el elemento se queda anclado al <b>cuadrante de pantalla donde lo dejo</b>,
 * que es lo que hace que su colocacion aguante un cambio de resolucion. Ver {@link HudAnchor}.
 *
 * <h2>Rejilla magnetica</h2>
 * Al arrastrar cerca de un borde o del centro, el elemento se pega: alinear a ojo un HUD con el raton es
 * incomodo y el resultado casi siempre queda un par de pixeles torcido.
 */
public final class HudEditorScreen extends Screen {

    /** Distancia en pixeles a la que un borde o el centro atraen al elemento. */
    private static final int SNAP_DISTANCE = 8;

    /** Margen desde el borde en el que se pega un elemento. */
    private static final int EDGE_MARGIN = 4;

    private static final int COLOR_BOX = 0x40FFFFFF;
    private static final int COLOR_BOX_HOVER = 0x60FFD700;
    private static final int COLOR_BOX_HIDDEN = 0x30FF5555;
    private static final int COLOR_GUIDE = 0x30FFFFFF;

    private final Screen parent;

    private HudElement dragging;
    private int grabOffsetX;
    private int grabOffsetY;

    public HudEditorScreen(Screen parent) {
        super(Component.translatable("screen.dojutsu.hud_editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = this.height - 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.dojutsu.hud_editor.reset"),
                        button -> HudLayout.reset())
                .bounds(this.width / 2 - 104, y, 100, 20)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(this.width / 2 + 4, y, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // super.render() PRIMERO: aplica el desenfoque de fondo y dibuja los botones. Si se llama al final
        // (o se invoca renderBackground a mano antes), el desenfoque cae ENCIMA de lo ya dibujado y los
        // elementos del HUD salen borrosos — que es justo el bug que tenia esta pantalla. Mismo orden que
        // NinjaMenuScreen y JutsuWheelScreen.
        super.render(graphics, mouseX, mouseY, partialTick);

        if (dragging != null) {
            drawGuides(graphics);
        }

        for (HudElement element : HudElement.all()) {
            HudPlacement placement = HudLayout.get(element);
            int x = placement.resolveX(this.width, element.width());
            int y = placement.resolveY(this.height, element.height());
            boolean hovered = dragging == element || isOver(element, x, y, mouseX, mouseY);

            int color = !placement.visible() ? COLOR_BOX_HIDDEN : hovered ? COLOR_BOX_HOVER : COLOR_BOX;
            graphics.fill(x - 2, y - 2, x + element.width() + 2, y + element.height() + 2, color);

            if (placement.visible()) {
                drawElement(graphics, element, x, y);
            } else {
                graphics.drawString(this.font, Component.translatable("screen.dojutsu.hud_editor.hidden"),
                        x, y, 0xFF8888);
            }
        }

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("screen.dojutsu.hud_editor.hint"), this.width / 2, 26, 0xAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        HudElement hit = elementAt((int) mouseX, (int) mouseY);
        if (hit == null) {
            return false;
        }
        HudPlacement placement = HudLayout.get(hit);
        if (button == 1) { // clic derecho: mostrar/ocultar
            HudLayout.set(hit, placement.withVisible(!placement.visible()));
            return true;
        }
        dragging = hit;
        grabOffsetX = (int) mouseX - placement.resolveX(this.width, hit.width());
        grabOffsetY = (int) mouseY - placement.resolveY(this.height, hit.height());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging == null) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        int x = snapX((int) mouseX - grabOffsetX, dragging);
        int y = snapY((int) mouseY - grabOffsetY, dragging);
        // El anclaje se recalcula mientras se arrastra: cruzar a la mitad derecha de la pantalla deja el
        // elemento anclado a la derecha, que es lo que el jugador espera al cambiar de resolucion.
        HudAnchor anchor = anchorFor(x + dragging.width() / 2, y + dragging.height() / 2);
        HudPlacement moved = HudLayout.get(dragging)
                .withAnchor(anchor)
                .movedTo(x, y, this.width, this.height, dragging.width(), dragging.height());
        HudLayout.set(dragging, moved);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        HudLayout.save();
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Dibuja el elemento real (no un rectangulo de mentira): asi se ve exactamente como quedara en partida. */
    private void drawElement(GuiGraphics graphics, HudElement element, int x, int y) {
        switch (element) {
            case CHAKRA -> ChakraHudOverlay.render(graphics, this.font, x, y);
            case RANK -> RankHudOverlay.render(graphics, this.font, x, y);
            case ABILITY_BAR -> AbilityBarOverlay.render(graphics, this.font, x, y);
        }
    }

    /** Lineas guia de los bordes y del centro, visibles solo mientras se arrastra. */
    private void drawGuides(GuiGraphics graphics) {
        graphics.fill(this.width / 2, 0, this.width / 2 + 1, this.height, COLOR_GUIDE);
        graphics.fill(0, this.height / 2, this.width, this.height / 2 + 1, COLOR_GUIDE);
    }

    /** Elemento bajo el raton, el ultimo dibujado primero (para poder coger el de encima). */
    private HudElement elementAt(int mouseX, int mouseY) {
        HudElement[] elements = HudElement.all();
        for (int i = elements.length - 1; i >= 0; i--) {
            HudElement element = elements[i];
            HudPlacement placement = HudLayout.get(element);
            int x = placement.resolveX(this.width, element.width());
            int y = placement.resolveY(this.height, element.height());
            if (isOver(element, x, y, mouseX, mouseY)) {
                return element;
            }
        }
        return null;
    }

    private static boolean isOver(HudElement element, int x, int y, int mouseX, int mouseY) {
        return mouseX >= x - 2 && mouseX <= x + element.width() + 2
                && mouseY >= y - 2 && mouseY <= y + element.height() + 2;
    }

    private int snapX(int x, HudElement element) {
        if (Math.abs(x - EDGE_MARGIN) <= SNAP_DISTANCE) {
            return EDGE_MARGIN;
        }
        int right = this.width - element.width() - EDGE_MARGIN;
        if (Math.abs(x - right) <= SNAP_DISTANCE) {
            return right;
        }
        int center = this.width / 2 - element.width() / 2;
        return Math.abs(x - center) <= SNAP_DISTANCE ? center : x;
    }

    private int snapY(int y, HudElement element) {
        if (Math.abs(y - EDGE_MARGIN) <= SNAP_DISTANCE) {
            return EDGE_MARGIN;
        }
        int bottom = this.height - element.height() - EDGE_MARGIN;
        if (Math.abs(y - bottom) <= SNAP_DISTANCE) {
            return bottom;
        }
        int center = this.height / 2 - element.height() / 2;
        return Math.abs(y - center) <= SNAP_DISTANCE ? center : y;
    }

    /** Cuadrante de pantalla en el que cae un punto; determina a que esquina queda anclado el elemento. */
    private HudAnchor anchorFor(int centerX, int centerY) {
        boolean top = centerY < this.height / 2;
        int third = this.width / 3;
        if (centerX < third) {
            return top ? HudAnchor.TOP_LEFT : HudAnchor.BOTTOM_LEFT;
        }
        if (centerX > third * 2) {
            return top ? HudAnchor.TOP_RIGHT : HudAnchor.BOTTOM_RIGHT;
        }
        return top ? HudAnchor.TOP_CENTER : HudAnchor.BOTTOM_CENTER;
    }
}
