package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.client.ClientNinjaData;
import com.zamu.dojutsu.client.ClientOnboardingOptions;
import com.zamu.dojutsu.data.state.OnboardingState.Phase;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.network.OnboardingChoicePayload;
import com.zamu.dojutsu.network.OnboardingOptionsPayload.ClanOption;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Pantalla de la creacion del ninja (DJT-060): reemplaza el fake-GUI del datapack ({@code join_main} +
 * {@code join_clan_ab}/{@code ab2} + {@code nature_select_main}: titulos "Select Clan"/"Select Affinity" +
 * hotbar secuestrado con sneak-para-elegir y paginas de 9 slots). <b>Solo cliente.</b>
 *
 * <p>Reglas ({@code [ADD/07]}): la pagina la decide la <b>fase sincronizada</b> ({@link ClientNinjaData}),
 * nunca el cliente — {@code SELECTING_CLAN} muestra los clanes (de {@link ClientOnboardingOptions}, enviados
 * por el servidor) y {@code SELECTING_AFFINITY} las 5 basicas + Random. Cada boton solo <b>envia la
 * intencion</b> ({@link OnboardingChoicePayload}); el servidor valida (DJT-059) y la pantalla avanza cuando
 * llega el sync. El <b>caso Lee</b> sale gratis: el servidor pasa directo a {@code COMPLETED} y la pantalla
 * se cierra sin ensenar la pagina de afinidad.
 *
 * <p>Colores: los clanes traen el RGB de su {@code teamColor} en el payload; las afinidades usan los colores
 * exactos del actionbar de {@code nature_select_main} (RA-4). El {@code join_delay} de 14 ticks del datapack
 * no se porta: era el anti doble-click del truco por slots (aqui hay botones + gate de fase en el servidor).
 */
public final class NinjaCreationScreen extends Screen {

    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int LIST_TOP = 60;
    /** Si el servidor rechazara la eleccion (no deberia desde esta UI), se reactiva tras este margen. */
    private static final int AWAIT_TIMEOUT_TICKS = 40;

    /** Colores del actionbar de {@code nature_select_main} (RA-4), por ordinal de las 5 basicas. */
    private static final int[] AFFINITY_COLORS = {
            0xFF5555, // fire: "red"
            0x00D789, // wind
            0xFFFF55, // lightning: "yellow"
            0xB5741B, // earth
            0x389BFE  // water
    };
    private static final int RANDOM_COLOR = 0xFFFFFF; // "Random Clan"/"Random Affinity": "white"

    /** Lado del icono de clan al pintarlo; el PNG es 32x32 y se escala a la mitad para caber en el boton. */
    private static final int ICON_SIZE = 16;
    /** Lado real de la textura del icono en el resource pack original. */
    private static final int ICON_TEXTURE_SIZE = 32;

    private final List<Button> choiceButtons = new ArrayList<>();
    /** Iconos de clan a pintar dentro de su boton (F10/DJT-224); vacio en la pagina de afinidad. */
    private final List<ClanIcon> clanIcons = new ArrayList<>();
    private Phase builtPhase;
    private boolean awaitingServer;
    private int awaitTicks;

    /**
     * Icono de un clan ya situado. Se resuelve al construir la pagina y no en cada frame: la lista de clanes
     * la manda el servidor y no cambia mientras la pantalla vive.
     *
     * @param texture textura del icono ({@code textures/gui/clan/<id>.png})
     * @param x       esquina izquierda dentro del boton
     * @param y       esquina superior dentro del boton
     */
    private record ClanIcon(ResourceLocation texture, int x, int y) {
    }

    public NinjaCreationScreen() {
        super(Component.translatable("screen.dojutsu.ninja_creation"));
    }

    @Override
    protected void init() {
        choiceButtons.clear();
        clanIcons.clear(); // init() vuelve a correr al redimensionar: las posiciones cacheadas se recalculan
        builtPhase = ClientNinjaData.onboarding().orElse(null);
        if (builtPhase == Phase.SELECTING_CLAN) {
            buildClanPage();
        } else if (builtPhase == Phase.SELECTING_AFFINITY) {
            buildAffinityPage();
        }
        // Otras fases: sin botones; tick() cierra la pantalla en cuanto corre.
    }

    /**
     * Pagina de clan: Random primero (slot 0 del datapack) y los clanes del servidor, en grid de 3.
     *
     * <p>Cada clan lleva su <b>icono</b> (F10/DJT-224), el mismo que el datapack mostraba como modelo de item
     * en el menu ({@code icons/clans/*}, {@code CustomModelData} 102-115). "Random" no tiene: en el original
     * tampoco, porque no es un clan.
     */
    private void buildClanPage() {
        addChoice(0, Component.translatable("onboarding.dojutsu.random_clan").withColor(RANDOM_COLOR),
                OnboardingChoicePayload.Kind.CLAN, "random");
        List<ClanOption> clans = ClientOnboardingOptions.clans();
        for (int i = 0; i < clans.size(); i++) {
            ClanOption clan = clans.get(i);
            int cell = i + 1;
            addChoice(cell, Component.translatable("clan.dojutsu." + clan.id()).withColor(clan.color()),
                    OnboardingChoicePayload.Kind.CLAN, clan.id());
            clanIcons.add(new ClanIcon(DojutsuMod.id("textures/gui/clan/" + clan.id() + ".png"),
                    cellX(cell) + 2, cellY(cell) + (BUTTON_HEIGHT - ICON_SIZE) / 2));
        }
    }

    /** Pagina de afinidad: Random primero y las 5 basicas (slots 0..5 de {@code nature_select_main}). */
    private void buildAffinityPage() {
        addChoice(0, Component.translatable("onboarding.dojutsu.random_affinity").withColor(RANDOM_COLOR),
                OnboardingChoicePayload.Kind.AFFINITY, "random");
        int cell = 1;
        for (NatureType nature : NatureType.basics()) {
            String id = nature.name().toLowerCase(Locale.ROOT);
            addChoice(cell, Component.translatable("element.dojutsu." + id)
                            .withColor(AFFINITY_COLORS[nature.ordinal()]),
                    OnboardingChoicePayload.Kind.AFFINITY, id);
            cell++;
        }
    }

    /** Esquina izquierda de la celda {@code cell} del grid de 3 columnas. */
    private int cellX(int cell) {
        int gridLeft = this.width / 2 - (3 * BUTTON_WIDTH + 2 * GAP) / 2;
        return gridLeft + (cell % 3) * (BUTTON_WIDTH + GAP);
    }

    /** Esquina superior de la celda {@code cell} del grid de 3 columnas. */
    private int cellY(int cell) {
        return LIST_TOP + (cell / 3) * (BUTTON_HEIGHT + GAP);
    }

    /** Anade el boton de una opcion en la celda {@code cell} del grid de 3 columnas. */
    private void addChoice(int cell, Component label, OnboardingChoicePayload.Kind kind, String choice) {
        int x = cellX(cell);
        int y = cellY(cell);
        Button button = Button.builder(label, b -> sendChoice(kind, choice))
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(button);
        choiceButtons.add(button);
    }

    /** Envia la <b>intencion</b> al servidor y bloquea los botones hasta que el sync cambie la fase. */
    private void sendChoice(OnboardingChoicePayload.Kind kind, String choice) {
        if (awaitingServer) {
            return;
        }
        PlatformServices.NETWORK.sendToServer(new OnboardingChoicePayload(kind, choice));
        awaitingServer = true;
        awaitTicks = AWAIT_TIMEOUT_TICKS;
        choiceButtons.forEach(button -> button.active = false);
    }

    @Override
    public void tick() {
        Phase phase = ClientNinjaData.onboarding().orElse(null);
        if (phase != Phase.SELECTING_CLAN && phase != Phase.SELECTING_AFFINITY) {
            onClose(); // COMPLETED (o sin sync): la creacion termino, fuera de la pantalla
            return;
        }
        if (phase != builtPhase) {
            awaitingServer = false;
            rebuildWidgets(); // el servidor avanzo de fase: reconstruye la pagina
            return;
        }
        if (awaitingServer && --awaitTicks <= 0) {
            awaitingServer = false; // el servidor no avanzo (rechazo): reactivar en vez de dejarla muerta
            choiceButtons.forEach(button -> button.active = true);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);
        if (builtPhase == Phase.SELECTING_CLAN) {
            // Titulos del datapack: "Select Clan" azul / "Select Affinity" dorado (join_main).
            graphics.drawCenteredString(this.font,
                    Component.translatable("onboarding.dojutsu.select_clan"), centerX, 40, 0x5555FF);
            // Los iconos van DESPUES del super.render: si no, el fondo del boton los taparia.
            for (ClanIcon icon : clanIcons) {
                graphics.blit(icon.texture(), icon.x(), icon.y(), ICON_SIZE, ICON_SIZE,
                        0.0F, 0.0F, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE, ICON_TEXTURE_SIZE);
            }
            if (ClientOnboardingOptions.clans().isEmpty()) {
                graphics.drawCenteredString(this.font,
                        Component.translatable("onboarding.dojutsu.waiting"), centerX, 96, 0xAAAAAA);
            }
        } else if (builtPhase == Phase.SELECTING_AFFINITY) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("onboarding.dojutsu.select_affinity"), centerX, 40, 0xFFAA00);
        }
    }

    @Override
    public void removed() {
        // Cerrada sin elegir (Esc, muerte...): el opener la reabre tras un margen que deja usar el menu de
        // pausa (guardar/salir). Si se cerro por COMPLETED, el opener ya no actua.
        NinjaCreationOpener.delayReopen();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // no pausa el juego en un solo jugador
    }
}
