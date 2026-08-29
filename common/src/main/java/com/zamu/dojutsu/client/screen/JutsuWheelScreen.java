package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.client.keybind.KeyBindings;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.items.JutsuItem;
import com.zamu.dojutsu.network.SelectJutsuPayload;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Rueda radial de jutsus (F11/DJT-234). <b>Solo cliente.</b> Muestra los <b>ocho huecos rapidos</b> del
 * jugador —no su catalogo entero— y al soltar la tecla selecciona el que este bajo el raton.
 *
 * <p><b>Por que solo los ocho huecos:</b> es la decision de diseno del proyecto. Una rueda con 128 sectores
 * es ilegible y lenta de leer en combate, que es justo cuando se usa. Elegir <i>que va</i> en cada hueco es
 * cosa del libro de jutsus (DJT-235); esta rueda es para cambiar rapido entre lo que ya preparaste.
 *
 * <p><b>El cliente no decide nada</b> (regla 12): al soltar solo se manda un {@link SelectJutsuPayload} con
 * el id <b>por nombre</b>, y el servidor revalida contra los jutsus aprendidos. Lo gris de un sector es
 * presentacion —una pista de que ahora mismo no saldria— y no una puerta: quien rechaza el cast sigue siendo
 * el {@code JutsuCastPipeline}.
 *
 * <p>Se dibuja con primitivas y con el <b>icono real del pergamino</b> ({@link JutsuItem#createStack}), asi
 * que los modelos por tecnica que lleguen despues aparecen aqui solos, sin tocar esta pantalla.
 */
public final class JutsuWheelScreen extends Screen {

    /** Radio del anillo donde se posan los iconos. */
    private static final int RADIUS = 62;

    /** Radio de la zona muerta central: soltar ahi no cambia nada (la salida sin elegir). */
    private static final int DEAD_ZONE = 22;

    private static final int SECTOR_HALF = 20;      // medio lado del recuadro de cada sector
    private static final int COLOR_SECTOR = 0x99101010;
    private static final int COLOR_SECTOR_HOVER = 0xCC2B4C6F;
    private static final int COLOR_SECTOR_CURRENT = 0x66207B3A; // el que ya llevas seleccionado
    private static final int COLOR_BORDER = 0xFF000000;
    private static final int COLOR_HINT = 0xFFBFBFBF;

    /**
     * Si la tecla llego a estar pulsada desde que se abrio la rueda. Sirve para distinguir el gesto de
     * <b>mantener</b> (se cierra al soltar) del <b>toque corto</b>, en el que la tecla ya esta suelta cuando
     * la pantalla aparece y cerrar de inmediato dejaria al jugador sin poder elegir nada.
     */
    private boolean everHeld;

    /** Cache de los iconos para no reconstruir ocho ItemStack por frame. */
    private final ItemStack[] icons = new ItemStack[JutsuKnowledge.SLOT_COUNT];
    private java.util.List<String> builtFrom = java.util.List.of();

    public JutsuWheelScreen() {
        super(Component.translatable("screen.dojutsu.jutsu_wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // en combate el mundo no puede congelarse detras de la rueda
    }

    /** Rehace los iconos solo cuando el reparto cambia (el sync puede llegar con la rueda abierta). */
    private void refreshIcons() {
        java.util.List<String> slots = ClientNinjaData.quickSlots();
        if (slots.equals(builtFrom)) {
            return;
        }
        builtFrom = java.util.List.copyOf(slots);
        for (int i = 0; i < JutsuKnowledge.SLOT_COUNT; i++) {
            icons[i] = ClientNinjaData.slot(i).map(JutsuItem::createStack).orElse(ItemStack.EMPTY);
        }
    }

    /** Centro del sector {@code index}, con el hueco 1 arriba y el resto en el sentido del reloj. */
    private int sectorX(int index) {
        double angle = Math.PI * 2 * index / JutsuKnowledge.SLOT_COUNT - Math.PI / 2;
        return this.width / 2 + (int) Math.round(Math.cos(angle) * RADIUS);
    }

    private int sectorY(int index) {
        double angle = Math.PI * 2 * index / JutsuKnowledge.SLOT_COUNT - Math.PI / 2;
        return this.height / 2 + (int) Math.round(Math.sin(angle) * RADIUS);
    }

    /**
     * Hueco bajo el raton, o {@code -1} si esta en la zona muerta central.
     *
     * <p>Se resuelve por <b>angulo</b> y no por la caja del icono: en una rueda el jugador apunta hacia un
     * lado, no a un cuadrado, y exigir puntería sobre el icono la vuelve incomoda justo cuando hay prisa.
     */
    int sectorAt(int mouseX, int mouseY) {
        double dx = mouseX - this.width / 2.0;
        double dy = mouseY - this.height / 2.0;
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE) {
            return -1;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2; // 0 = arriba, creciendo en el sentido del reloj
        double slice = Math.PI * 2 / JutsuKnowledge.SLOT_COUNT;
        int index = (int) Math.floor((angle + slice / 2) / slice);
        return Math.floorMod(index, JutsuKnowledge.SLOT_COUNT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // super.render() PRIMERO: aplica el fondo y dibuja los widgets. Llamarlo al final deja el desenfoque
        // ENCIMA de lo ya pintado (mismo orden que NinjaMenuScreen y HudEditorScreen).
        super.render(graphics, mouseX, mouseY, partialTick);
        refreshIcons();

        Minecraft minecraft = Minecraft.getInstance();
        long now = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        int hovered = sectorAt(mouseX, mouseY);
        Optional<String> current = ClientNinjaData.selectedJutsu();

        for (int i = 0; i < JutsuKnowledge.SLOT_COUNT; i++) {
            drawSector(graphics, i, hovered == i, current, now);
        }
        drawCenterHint(graphics, hovered, now);
    }

    private void drawSector(GuiGraphics graphics, int index, boolean hovered, Optional<String> current,
            long now) {
        int x = sectorX(index);
        int y = sectorY(index);
        Optional<String> jutsuId = ClientNinjaData.slot(index);
        boolean isCurrent = jutsuId.isPresent() && current.filter(jutsuId.get()::equals).isPresent();

        int background = hovered ? COLOR_SECTOR_HOVER : (isCurrent ? COLOR_SECTOR_CURRENT : COLOR_SECTOR);
        graphics.fill(x - SECTOR_HALF - 1, y - SECTOR_HALF - 1, x + SECTOR_HALF + 1, y + SECTOR_HALF + 1,
                COLOR_BORDER);
        graphics.fill(x - SECTOR_HALF, y - SECTOR_HALF, x + SECTOR_HALF, y + SECTOR_HALF, background);

        if (jutsuId.isEmpty()) {
            // Hueco libre: se dibuja igualmente, para que la rueda tenga siempre la misma forma y el jugador
            // aprenda las posiciones con el musculo, no leyendo.
            graphics.drawCenteredString(this.font, "-", x, y - 4, COLOR_HINT);
            return;
        }
        graphics.renderItem(icons[index], x - 8, y - 8);

        long remaining = ClientNinjaData.remainingCooldown(jutsuId.get(), now);
        if (remaining > 0) {
            // Velo oscuro + segundos: la misma pista que da la hotbar vanilla en cooldown.
            graphics.fill(x - SECTOR_HALF, y - SECTOR_HALF, x + SECTOR_HALF, y + SECTOR_HALF, 0xAA000000);
            graphics.drawCenteredString(this.font, tenths(remaining), x, y - 4, 0xFFFF7070);
        }
    }

    /** Nombre del jutsu apuntado en el centro; es donde el jugador mira mientras gira el raton. */
    private void drawCenterHint(GuiGraphics graphics, int hovered, long now) {
        Component label;
        int color = 0xFFFFFF;
        Optional<String> jutsuId = hovered < 0 ? Optional.empty() : ClientNinjaData.slot(hovered);
        if (hovered < 0) {
            label = Component.translatable("screen.dojutsu.jutsu_wheel.cancel");
            color = COLOR_HINT;
        } else if (jutsuId.isEmpty()) {
            label = Component.translatable("screen.dojutsu.jutsu_wheel.empty");
            color = COLOR_HINT;
        } else {
            label = Component.translatable("jutsu.dojutsu." + jutsuId.get());
            if (ClientNinjaData.remainingCooldown(jutsuId.get(), now) > 0) {
                color = 0xFFFF7070;
            }
        }
        graphics.drawCenteredString(this.font, label, this.width / 2, this.height / 2 - 4, color);
    }

    private static String tenths(long ticks) {
        long value = (ticks * 10 + 19) / 20;
        return (value / 10) + "." + (value % 10);
    }

    /** Elige el hueco apuntado y cierra. En la zona muerta cierra sin tocar la seleccion. */
    void chooseAndClose(int mouseX, int mouseY) {
        int index = sectorAt(mouseX, mouseY);
        ClientNinjaData.slot(index).ifPresent(
                jutsuId -> PlatformServices.NETWORK.sendToServer(new SelectJutsuPayload(jutsuId)));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            chooseAndClose((int) mouseX, (int) mouseY);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Cierra al soltar la tecla, que es el gesto natural de una rueda. El toque corto no cuenta: si la tecla
     * ya venia suelta al abrirse, se espera a que el jugador la pulse (o a que elija con el raton).
     */
    @Override
    public void tick() {
        if (KeyBindings.isWheelKeyHeld()) {
            everHeld = true;
            return;
        }
        if (everHeld) {
            // mouseHandler da pixeles de ventana; la pantalla trabaja en pixeles de GUI (escala del juego).
            Minecraft minecraft = Minecraft.getInstance();
            double scaleX = (double) this.width / minecraft.getWindow().getScreenWidth();
            double scaleY = (double) this.height / minecraft.getWindow().getScreenHeight();
            chooseAndClose((int) (minecraft.mouseHandler.xpos() * scaleX),
                    (int) (minecraft.mouseHandler.ypos() * scaleY));
        }
    }
}
