package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientJutsuBook;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.items.JutsuItem;
import com.zamu.dojutsu.network.AssignSlotPayload;
import com.zamu.dojutsu.network.JutsuBookEntry;
import com.zamu.dojutsu.network.OpenJutsuBookPayload;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Pestaña "Jutsus" del menu (F11/DJT-235, movida a pestaña en DJT-236): todo lo aprendido con sus numeros, y
 * el reparto de los <b>ocho huecos</b> de la rueda.
 *
 * <p>Division de trabajo con la rueda: la rueda es para <b>combate</b> (cambiar rapido entre lo que ya
 * preparaste); esto es para <b>entre peleas</b> (comparar y preparar). Por eso aqui no se castea nada.
 *
 * <p><b>Se asigna por dos clics, no arrastrando.</b> El plan proponia arrastrar; elegi clic-clic porque
 * sobrevive a que se te escape el raton, funciona igual con la lista llena y no depende de acertar un
 * objetivo pequeño mientras mantienes el boton. Clic en la ficha, clic en el hueco. Clic derecho sobre un
 * hueco lo vacia.
 *
 * <p>Los numeros vienen del servidor ({@link ClientJutsuBook}) porque el {@code JutsuRegistry} es data-driven
 * y en un servidor dedicado el cliente no lo tiene. Toda asignacion se revalida alli.
 */
final class JutsuBookTab implements MenuTab {

    private static final int ROW_HEIGHT = 22;
    private static final int LIST_WIDTH = 250;
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 4;

    private static final int COLOR_ROW = 0x66000000;
    private static final int COLOR_ROW_HOVER = 0x88203040;
    private static final int COLOR_ROW_PICKED = 0xAA2B4C6F;
    private static final int COLOR_SLOT = 0x99101010;
    private static final int COLOR_SLOT_TARGET = 0x99207B3A; // resaltado mientras hay ficha elegida
    private static final int COLOR_BORDER = 0xFF000000;
    private static final int COLOR_DIM = 0xFFBFBFBF;

    /** Ficha elegida a la espera de hueco, o {@code null}. Es todo el estado de la interaccion. */
    private String picked;

    /** Filtro por categoria; cadena vacia = todas. */
    private String category = "";

    private NinjaMenuScreen screen;
    private EditBox search;
    private String query = "";
    private int scroll;

    @Override
    public Component title() {
        return Component.translatable("screen.dojutsu.tab.jutsus");
    }

    @Override
    public int observedVersion() {
        return ClientJutsuBook.version();
    }

    @Override
    public void request() {
        PlatformServices.NETWORK.sendToServer(new OpenJutsuBookPayload());
    }

    @Override
    public void init(NinjaMenuScreen screen) {
        this.screen = screen;
        int top = screen.contentTop();
        int left = screen.width / 2 - LIST_WIDTH / 2;

        search = new EditBox(screen.font(), left, top, LIST_WIDTH - 92, 18,
                Component.translatable("screen.dojutsu.jutsu_book.search"));
        search.setHint(Component.translatable("screen.dojutsu.jutsu_book.search"));
        // El texto se conserva entre reconstrucciones: llega una vista nueva del servidor cada vez que
        // asignas, y perder lo escrito en ese momento seria maddening.
        search.setValue(query);
        search.setResponder(text -> {
            query = text;
            scroll = 0; // al filtrar, volver arriba: si no, la lista parece vacia
        });
        screen.addTabWidget(search);

        screen.addTabWidget(Button.builder(categoryLabel(), b -> cycleCategory())
                .bounds(left + LIST_WIDTH - 88, top, 88, 18)
                .build());
    }

    /** Etiqueta del boton de filtro: la categoria actual, o "todas". */
    private Component categoryLabel() {
        return category.isEmpty()
                ? Component.translatable("screen.dojutsu.jutsu_book.all_categories")
                : Component.translatable("element.dojutsu." + category);
    }

    /**
     * Pasa a la siguiente categoria presente entre lo aprendido, y vuelve a "todas" al final.
     *
     * <p>Se cicla en vez de listar todas las categorias del datapack porque el libro es el inventario del
     * jugador: ofrecer filtros que no dan resultados es ruido.
     */
    private void cycleCategory() {
        List<String> categories = ClientJutsuBook.categories();
        if (categories.isEmpty()) {
            return;
        }
        int next = category.isEmpty() ? 0 : categories.indexOf(category) + 1;
        category = next >= categories.size() ? "" : categories.get(next);
        scroll = 0;
        screen.rebuild(); // refresca la etiqueta del boton
    }

    /** Fichas que pasan el filtro de categoria y el buscador, en orden estable. */
    private List<JutsuBookEntry> visibleEntries() {
        String text = query.toLowerCase(Locale.ROOT).trim();
        return ClientJutsuBook.entries().stream()
                .filter(entry -> category.isEmpty() || entry.category().equals(category))
                .filter(entry -> text.isEmpty() || matches(entry, text))
                .toList();
    }

    /**
     * Busca por el <b>nombre traducido</b> ademas de por el id: el jugador ve "Great Fireball" en pantalla y
     * es lo que va a escribir; buscar solo por {@code great_fireball} no encontraria nada al teclear "fire".
     */
    private boolean matches(JutsuBookEntry entry, String text) {
        if (entry.id().contains(text)) {
            return true;
        }
        return Component.translatable("jutsu.dojutsu." + entry.id()).getString()
                .toLowerCase(Locale.ROOT).contains(text);
    }

    private int listTop() {
        return screen.contentTop() + 24;
    }

    private int visibleRows() {
        return Math.max(1, (slotsY() - listTop() - 8) / ROW_HEIGHT);
    }

    private int slotsY() {
        return screen.contentBottom() - SLOT_SIZE - 12;
    }

    private int slotsX() {
        return screen.width / 2 - (JutsuKnowledge.SLOT_COUNT * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderList(graphics, mouseX, mouseY);
        renderSlots(graphics, mouseX, mouseY);
        renderHint(graphics);
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        List<JutsuBookEntry> entries = visibleEntries();
        if (entries.isEmpty()) {
            graphics.drawCenteredString(screen.font(),
                    Component.translatable("screen.dojutsu.jutsu_book.empty"),
                    screen.width / 2, listTop() + 12, COLOR_DIM);
            return;
        }
        int x = screen.width / 2 - LIST_WIDTH / 2;
        int rows = Math.min(visibleRows(), entries.size() - scroll);
        for (int i = 0; i < rows; i++) {
            renderRow(graphics, entries.get(scroll + i), x, listTop() + i * ROW_HEIGHT, mouseX, mouseY);
        }
        if (entries.size() > visibleRows()) {
            graphics.drawString(screen.font(), (scroll + rows) + "/" + entries.size(),
                    x + LIST_WIDTH + 6, listTop(), COLOR_DIM);
        }
    }

    private void renderRow(GuiGraphics graphics, JutsuBookEntry entry, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH && mouseY >= y && mouseY < y + ROW_HEIGHT - 2;
        boolean isPicked = entry.id().equals(picked);
        graphics.fill(x, y, x + LIST_WIDTH, y + ROW_HEIGHT - 2,
                isPicked ? COLOR_ROW_PICKED : (hovered ? COLOR_ROW_HOVER : COLOR_ROW));

        graphics.renderItem(JutsuItem.createStack(entry.id()), x + 2, y + 2);
        graphics.drawString(screen.font(), Component.translatable("jutsu.dojutsu." + entry.id()),
                x + 22, y + 2, 0xFFFFFF);
        graphics.drawString(screen.font(), statsOf(entry), x + 22, y + 12, COLOR_DIM);
    }

    /** Linea de numeros de una ficha: coste, enfriamiento, dano si lo declara, y afinidad si la exige. */
    private Component statsOf(JutsuBookEntry entry) {
        var line = Component.empty()
                .append(Component.translatable("screen.dojutsu.jutsu_book.cost", entry.chakraCost()))
                .append("  ")
                .append(Component.translatable("screen.dojutsu.jutsu_book.cooldown", entry.cooldownSeconds()));
        if (entry.damage() > 0) {
            line.append("  ").append(Component.translatable("screen.dojutsu.jutsu_book.damage",
                    trim(entry.damage())));
        }
        entry.affinity().ifPresent(affinity -> line.append("  ")
                .append(Component.translatable("screen.dojutsu.jutsu_book.affinity",
                        Component.translatable("element.dojutsu." + affinity.nature()), affinity.amount())));
        return line;
    }

    /** Formatea el dano sin decimal cuando es entero (12 en vez de 12.0). */
    private static String trim(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private void renderSlots(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = slotsX();
        int y = slotsY();
        for (int i = 0; i < JutsuKnowledge.SLOT_COUNT; i++) {
            int slotX = x + i * (SLOT_SIZE + SLOT_GAP);
            boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= y && mouseY < y + SLOT_SIZE;
            graphics.fill(slotX - 1, y - 1, slotX + SLOT_SIZE + 1, y + SLOT_SIZE + 1, COLOR_BORDER);
            graphics.fill(slotX, y, slotX + SLOT_SIZE, y + SLOT_SIZE,
                    (picked != null && hovered) ? COLOR_SLOT_TARGET : COLOR_SLOT);

            ItemStack icon = ClientJutsuBook.slot(i).map(JutsuItem::createStack).orElse(ItemStack.EMPTY);
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, slotX + 4, y + 4);
            }
            graphics.drawString(screen.font(), String.valueOf(i + 1), slotX + 1, y + SLOT_SIZE - 9, COLOR_DIM);
        }
    }

    /** Pie: que hacer ahora. Cambia segun haya ficha elegida o no. */
    private void renderHint(GuiGraphics graphics) {
        Component hint = picked == null
                ? Component.translatable("screen.dojutsu.jutsu_book.hint_pick")
                : Component.translatable("screen.dojutsu.jutsu_book.hint_place",
                        Component.translatable("jutsu.dojutsu." + picked).withStyle(ChatFormatting.AQUA));
        graphics.drawCenteredString(screen.font(), hint, screen.width / 2, slotsY() + SLOT_SIZE + 6, COLOR_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return clickedSlot(mouseX, mouseY, button) || clickedRow(mouseX, mouseY, button);
    }

    /** Clic en un hueco: coloca la ficha elegida (izquierdo) o vacia el hueco (derecho). */
    private boolean clickedSlot(double mouseX, double mouseY, int button) {
        int y = slotsY();
        if (mouseY < y || mouseY >= y + SLOT_SIZE) {
            return false;
        }
        int offset = (int) mouseX - slotsX();
        int index = offset / (SLOT_SIZE + SLOT_GAP);
        if (offset < 0 || index >= JutsuKnowledge.SLOT_COUNT
                || offset % (SLOT_SIZE + SLOT_GAP) >= SLOT_SIZE) {
            return false; // el hueco entre recuadros no es de nadie
        }
        if (button == 1) {
            send(index, "");
            return true;
        }
        if (button == 0 && picked != null) {
            send(index, picked);
            picked = null; // colocada: se suelta, para no colocarla sin querer en el siguiente clic
            return true;
        }
        return false;
    }

    /** Clic en una fila: la elige (o la suelta si ya estaba elegida). */
    private boolean clickedRow(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }
        int x = screen.width / 2 - LIST_WIDTH / 2;
        if (mouseX < x || mouseX >= x + LIST_WIDTH || mouseY < listTop()) {
            return false;
        }
        int row = ((int) mouseY - listTop()) / ROW_HEIGHT;
        List<JutsuBookEntry> entries = visibleEntries();
        if (row < 0 || row >= visibleRows() || scroll + row >= entries.size()) {
            return false;
        }
        String id = entries.get(scroll + row).id();
        picked = id.equals(picked) ? null : id;
        return true;
    }

    private void send(int slot, String jutsuId) {
        PlatformServices.NETWORK.sendToServer(new AssignSlotPayload(slot, jutsuId));
        // No se toca la vista local: se espera al JutsuBookPayload de vuelta. Si se pintara el cambio antes
        // de que el servidor lo acepte, un rechazo dejaria la pantalla mintiendo hasta el siguiente refresco.
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        int max = Math.max(0, visibleEntries().size() - visibleRows());
        scroll = Math.clamp(scroll - (int) Math.signum(deltaY), 0, max);
        return true;
    }
}
