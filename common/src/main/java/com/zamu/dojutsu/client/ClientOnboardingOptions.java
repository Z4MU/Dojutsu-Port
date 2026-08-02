package com.zamu.dojutsu.client;

import com.zamu.dojutsu.network.OnboardingOptionsPayload.ClanOption;
import java.util.List;

/**
 * Cache <b>de solo lectura</b> de las opciones de clan de la creacion del ninja en el <b>cliente</b>. La
 * alimenta {@code OnboardingSync} (S2C); la consulta la {@code NinjaCreationScreen}.
 *
 * <p>Mismas reglas que {@link ClientNinjaData}: solo cliente, solo presentacion (el servidor re-valida toda
 * eleccion contra su registro real), no se persiste y se limpia al salir del mundo. Sin imports de MC-cliente:
 * inocua aunque se cargue en servidor dedicado.
 */
public final class ClientOnboardingOptions {

    private static List<ClanOption> clans = List.of();

    private ClientOnboardingOptions() {
    }

    /** Reemplaza las opciones observadas (lo llama el receptor del payload). */
    public static void set(List<ClanOption> options) {
        clans = List.copyOf(options);
    }

    /** Opciones de clan observadas, en orden de presentacion; vacia si aun no llego el payload. */
    public static List<ClanOption> clans() {
        return clans;
    }

    /** Limpia la cache (al cambiar de mundo / desconectar). */
    public static void clear() {
        clans = List.of();
    }
}
