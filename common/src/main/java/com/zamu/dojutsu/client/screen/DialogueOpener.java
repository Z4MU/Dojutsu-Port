package com.zamu.dojutsu.client.screen;

import com.zamu.dojutsu.client.ClientDialogue;
import com.zamu.dojutsu.domain.story.SageEncounter;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

/**
 * Abre la {@link DialogueScreen} cuando el servidor anuncia un encuentro (F9/DJT-200). <b>Solo cliente.</b>
 *
 * <p>Mismo patron que {@link NinjaCreationOpener}: el receptor del payload solo escribe en {@link ClientDialogue}
 * —una cache sin imports de MC-cliente, inocua en servidor dedicado— y quien toca {@code Minecraft} es este tick.
 *
 * <p>A diferencia de la creacion de ninja, aqui <b>no se insiste</b>: el dialogo se consume al abrirse. Cerrarlo
 * con Esc es una respuesta valida (el sabio volvera a aparecerse), asi que reabrirlo seria secuestrar la partida.
 */
public final class DialogueOpener {

    private DialogueOpener() {
    }

    /** Suscribe el tick de cliente. Llamar una vez en la inicializacion de cliente. */
    public static void register() {
        ClientTickEvent.CLIENT_POST.register(DialogueOpener::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.screen != null) {
            return; // sin mundo, o hay otra pantalla abierta: esperar
        }
        ClientDialogue.pending()
                .flatMap(SageEncounter::byId)
                .ifPresent(sage -> {
                    ClientDialogue.clear(); // se consume: no se reabre solo
                    minecraft.setScreen(new DialogueScreen(sage));
                });
    }
}
