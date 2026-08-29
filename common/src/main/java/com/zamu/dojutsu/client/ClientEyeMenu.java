package com.zamu.dojutsu.client;

import java.util.List;

/**
 * Cache <b>de solo lectura</b> de la vista del menu de habilidades oculares en el <b>cliente</b> (DJT-117). La
 * alimenta el receptor de {@code EyeMenuPayload} (S2C); la consulta la {@code EyeAbilitiesTab} del menu ninja.
 *
 * <p>Mismas reglas que {@link ClientNinjaData}/{@link ClientOnboardingOptions}: solo cliente, solo presentacion
 * (el servidor re-valida toda seleccion), no se persiste y se limpia al salir del mundo. Sin imports de
 * MC-cliente: inocua aunque se cargue en servidor dedicado. El {@link #version()} sube en cada actualizacion;
 * la Screen lo usa para reconstruirse cuando llega una vista nueva (p. ej. tras seleccionar).
 */
public final class ClientEyeMenu {

    private static String activeEye = "";
    private static List<String> abilities = List.of();
    private static String selectedAbility = "";
    private static String eternalStatus = "NO_ETERNAL_EYE";
    private static int version;

    private ClientEyeMenu() {
    }

    /** Reemplaza la vista observada (lo llama el receptor del payload). Incrementa la version. */
    public static void set(String activeEyeValue, List<String> abilitiesValue, String selectedAbilityValue,
            String eternalStatusValue) {
        activeEye = activeEyeValue;
        abilities = List.copyOf(abilitiesValue);
        selectedAbility = selectedAbilityValue;
        eternalStatus = eternalStatusValue;
        version++;
    }

    /** {@code true} si hay un ojo activo con habilidades que mostrar. */
    public static boolean hasActiveEye() {
        return !activeEye.isEmpty();
    }

    /** Id del ojo activo observado (cadena vacia si ninguno). */
    public static String activeEye() {
        return activeEye;
    }

    /** Habilidades del ojo activo, en orden de presentacion. */
    public static List<String> abilities() {
        return abilities;
    }

    /** Id de la habilidad seleccionada (cadena vacia si ninguna). */
    public static String selectedAbility() {
        return selectedAbility;
    }

    /** Estado del despertar del EMS que mando el servidor (nombre de { AwakenOutcome}). */
    public static String eternalStatus() {
        return eternalStatus;
    }

    /** Contador de version: sube en cada {@link #set}/{@link #clear}; la Screen detecta cambios con el. */
    public static int version() {
        return version;
    }

    /** Limpia la cache (al cambiar de mundo / desconectar). */
    public static void clear() {
        activeEye = "";
        abilities = List.of();
        selectedAbility = "";
        eternalStatus = "NO_ETERNAL_EYE";
        version++;
    }
}
