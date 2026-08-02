package com.zamu.dojutsu.client;

import java.util.Optional;

/**
 * Cache <b>de solo lectura</b> del dialogo del Sabio pendiente en el <b>cliente</b> (F9/DJT-200). La alimenta el
 * receptor de {@code OpenDialoguePayload} (S2C); la consume {@code DialogueOpener} para abrir la pantalla.
 *
 * <p>Mismas reglas que {@link ClientEyeMenu}: solo cliente, solo presentacion (el servidor re-valida la eleccion),
 * no se persiste y se limpia al salir del mundo. <b>Sin imports de MC-cliente</b>: inocua aunque se cargue en un
 * servidor dedicado — por eso el receptor del payload no toca {@code Minecraft} y quien abre la pantalla es el
 * tick de cliente.
 */
public final class ClientDialogue {

    private static String sage = "";

    private ClientDialogue() {
    }

    /** Anota un dialogo pendiente de abrir (lo llama el receptor del payload). */
    public static void set(String sageId) {
        sage = sageId;
    }

    /** El dialogo pendiente, o vacio si no hay ninguno. */
    public static Optional<String> pending() {
        return sage.isEmpty() ? Optional.empty() : Optional.of(sage);
    }

    /** Consume el pendiente (al abrir la pantalla) o lo limpia (al desconectar). */
    public static void clear() {
        sage = "";
    }
}
