package com.zamu.dojutsu.story;

import com.zamu.dojutsu.client.ClientDialogue;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.state.StoryState;
import com.zamu.dojutsu.domain.story.SageEncounter;
import com.zamu.dojutsu.domain.story.SagePath;
import com.zamu.dojutsu.domain.story.StoryTable;
import com.zamu.dojutsu.network.OpenDialoguePayload;
import com.zamu.dojutsu.network.PathChoicePayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import net.minecraft.server.level.ServerPlayer;

/**
 * Abre el dialogo del Sabio y recibe la eleccion {@code [S19]} (F9/DJT-200).
 *
 * <h2>Ni armor stand narrador ni triggers</h2>
 * El datapack necesita <b>dos</b> mecanismos prestados para algo que en un mod es una pantalla:
 * <ul>
 *   <li>un <b>armor stand invisible</b> por jugador ({@code main/internal/narr}) que lo sigue para poder pintar el
 *       dialogo, con su propio barrido por tick para crearlo, moverlo y matarlo;</li>
 *   <li>los <b>triggers</b> de scoreboard ({@code yin_path}, {@code yang_path}, {@code no_path} — y sus gemelos
 *       {@code *_path2} para Hamura) como canal de respuesta, con el jugador ejecutando comandos desde un
 *       {@code tellraw} clicable.</li>
 * </ul>
 * Aqui es un payload que abre una {@code Screen} y otro que trae la respuesta. Desaparecen el narrador, su barrido
 * y los seis triggers.
 *
 * <h2>Solo el primer encuentro pregunta</h2>
 * {@code dialogue/first} termina ofreciendo tres opciones; {@code dialogue/second} <b>no</b> — se despide y llama a
 * {@code gain_other_half}. Por eso {@link #farewell} no abre pantalla: no habria nada que elegir en ella.
 *
 * <h2>El servidor decide, siempre</h2>
 * La pantalla solo <b>manda la intencion</b> ({@link PathChoicePayload}); el servidor revalida que el jugador
 * tenga de verdad un encuentro abierto antes de conceder nada ({@code [ADD/07]}). Un cliente que invente un
 * {@code PathChoicePayload} sin encuentro no consigue poderes.
 */
public final class DialogueController {

    /** Lineas del segundo dialogo ({@code dialogue/second}), que va integro por chat. */
    private static final int FAREWELL_LINES = 2;

    private DialogueController() {
    }

    /** Abre el dialogo de eleccion en el cliente del jugador ({@code dialogue/first}). */
    public static void open(ServerPlayer player, SageEncounter sage) {
        PlatformServices.NETWORK.sendToPlayer(player, new OpenDialoguePayload(sage.id()));
    }

    /** Despedida del segundo encuentro ({@code dialogue/second}): solo chat, sin opciones. */
    public static void farewell(ServerPlayer player, SageEncounter sage) {
        for (int i = 0; i < FAREWELL_LINES; i++) {
            SagePathService.say(player, sage, "story.dojutsu." + sage.id() + ".second." + i);
        }
    }

    /** Registra los payloads del dialogo (1 S2C + 1 C2S). Llamar una vez en el arranque. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.S2C,
                OpenDialoguePayload.TYPE, OpenDialoguePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientDialogue.set(payload.sage())));
        PlatformServices.NETWORK.register(NetworkService.Side.C2S,
                PathChoicePayload.TYPE, PathChoicePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        onChoice(player, payload.path());
                    }
                }));
    }

    /**
     * Aplica la eleccion del jugador, revalidando en servidor: hace falta un encuentro <b>abierto</b> (el sabio ya
     * se aparecio). Elegir dos veces lo corta {@link SagePathService#grant}, que rechaza una mitad ya concedida.
     */
    private static void onChoice(ServerPlayer player, SagePath path) {
        StoryState state = NinjaData.get(player).story();
        if (state.sage().isEmpty() || state.stage() < StoryTable.STAGE_MET) {
            return; // sin encuentro abierto no hay nada que elegir
        }
        SagePathService.grant(player, path, false);
    }
}
