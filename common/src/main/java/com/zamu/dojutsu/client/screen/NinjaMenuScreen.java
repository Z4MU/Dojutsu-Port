package com.zamu.dojutsu.client.screen;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Menu ninja: la <b>unica</b> pantalla de preparacion del mod ({@code [S13]}). <b>Solo cliente.</b> Reemplaza
 * el "fake-GUI" por {@code title}/{@code actionbar} + hotbar secuestrado del datapack.
 *
 * <p><b>F11/DJT-236 — las tres bajo una tecla.</b> Antes habia tres pantallas de preparacion con tres teclas:
 * el menu ({@code M}), las habilidades oculares ({@code N}) y el libro de jutsus ({@code P}). Ninguna se usa en
 * combate y las tres responden a la misma pregunta —"que tengo, y que va a lanzar cada tecla"—, asi que ahora
 * son <b>pestañas</b> de esta: Ninja / Jutsus / Ojo. {@code N} y {@code P} quedan libres.
 *
 * <p>El precedente ya estaba en el propio proyecto: el editor de HUD nunca tuvo tecla, se abre con un boton
 * desde aqui. Sigue igual.
 *
 * <p>La <b>rueda</b> ({@code Z}) se queda fuera a proposito: es la unica pantalla de combate, se usa
 * manteniendo la tecla y su virtud es no hacerte pensar. Meterla aqui la habria matado.
 *
 * <p>Reglas ({@code [ADD/07]}): las pestañas leen <b>solo</b> caches read-only de cliente y <b>nunca</b>
 * aplican gameplay; toda accion viaja como intencion y el servidor valida.
 */
public final class NinjaMenuScreen extends Screen {

    /** Indices de pestaña, para abrir el menu directamente donde interese. */
    public static final int TAB_NINJA = 0;
    public static final int TAB_JUTSUS = 1;
    public static final int TAB_EYE = 2;

    private static final int TAB_BAR_HEIGHT = 22;
    private static final int TAB_TOP = 30;
    private static final int TAB_WIDTH = 90;
    private static final int TAB_GAP = 2;

    private final List<MenuTab> tabs = List.of(new NinjaStatsTab(), new JutsuBookTab(), new EyeAbilitiesTab());

    /** Pestañas cuya peticion inicial ya se mando (una vez por apertura, no en cada cambio de pestaña). */
    private final boolean[] requested = new boolean[3];

    private int active;
    private int builtVersion = Integer.MIN_VALUE;

    public NinjaMenuScreen() {
        this(TAB_NINJA);
    }

    /** Abre el menu directamente en una pestaña. */
    public NinjaMenuScreen(int tab) {
        super(Component.translatable("screen.dojutsu.ninja_menu"));
        this.active = Math.clamp(tab, TAB_NINJA, TAB_EYE);
    }

    private MenuTab current() {
        return tabs.get(active);
    }

    // ------------------------------------------------------------------
    // Servicios que las pestañas necesitan del contenedor
    // ------------------------------------------------------------------

    /** Registra un widget de la pestaña activa (los miembros de {@code Screen} son inaccesibles desde fuera). */
    void addTabWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    /** Fuente del juego. */
    Font font() {
        return this.font;
    }

    /** Primera fila util por debajo de la barra de pestañas. */
    int contentTop() {
        return TAB_TOP + TAB_BAR_HEIGHT + 8;
    }

    /** Ultima fila util por encima del pie (donde vive el boton de cerrar). */
    int contentBottom() {
        return this.height - 34;
    }

    /** Reconstruye la pestaña activa. Lo usan las pestañas cuando cambia algo suyo (un filtro, por ejemplo). */
    void rebuild() {
        init();
    }

    // ------------------------------------------------------------------

    @Override
    protected void init() {
        clearWidgets();
        buildTabBar();
        if (!requested[active]) {
            current().request(); // pide su vista al servidor la primera vez que se muestra
            requested[active] = true;
        }
        builtVersion = current().observedVersion();
        current().init(this);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
    }

    private void buildTabBar() {
        int total = tabs.size() * TAB_WIDTH + (tabs.size() - 1) * TAB_GAP;
        int left = this.width / 2 - total / 2;
        for (int i = 0; i < tabs.size(); i++) {
            int index = i;
            Button button = Button.builder(tabs.get(i).title(), b -> switchTo(index))
                    .bounds(left + i * (TAB_WIDTH + TAB_GAP), TAB_TOP, TAB_WIDTH, TAB_BAR_HEIGHT - 2)
                    .build();
            // La pestaña abierta queda inactiva: se ve cual es sin pintar un marco aparte, y de paso no se
            // puede reconstruir la pantalla pulsandola una y otra vez.
            button.active = i != active;
            addRenderableWidget(button);
        }
    }

    private void switchTo(int index) {
        if (index == active) {
            return;
        }
        active = index;
        init();
    }

    @Override
    public void tick() {
        current().tick();
        if (current().observedVersion() != builtVersion) {
            init(); // llego una vista nueva del servidor: los widgets ya creados no se enteran solos
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // super.render() PRIMERO: aplica el desenfoque de fondo y dibuja los widgets. Si se llama al final, el
        // desenfoque cae ENCIMA de lo ya dibujado (el bug que tuvo el editor de HUD en su dia).
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        current().render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true; // lo consumio un widget (pestaña, boton "+", buscador...)
        }
        return current().mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (super.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) {
            return true;
        }
        return current().mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // no pausa el juego en un solo jugador
    }
}
