package com.zamu.dojutsu.client;

import com.zamu.dojutsu.network.JutsuBookEntry;
import java.util.List;
import java.util.Optional;

/**
 * Cache <b>de solo lectura</b> de la vista del libro de jutsus en el <b>cliente</b> (F11/DJT-235). La alimenta
 * el receptor de {@code JutsuBookPayload} (S2C); la consulta la {@code JutsuBookTab} del menu ninja.
 *
 * <p>Mismas reglas que {@link ClientEyeMenu}: solo presentacion (el servidor revalida toda asignacion), no se
 * persiste y se limpia al salir del mundo. Sin imports de MC-cliente, asi que es inocua aunque se cargue en un
 * servidor dedicado. El {@link #version()} sube en cada actualizacion; la Screen lo usa para reconstruirse
 * cuando llega una vista nueva.
 */
public final class ClientJutsuBook {

    private static List<JutsuBookEntry> entries = List.of();
    private static List<String> slots = List.of();
    private static int version;

    private ClientJutsuBook() {
    }

    /** Reemplaza la vista observada (lo llama el receptor del payload). Incrementa la version. */
    public static void set(List<JutsuBookEntry> entriesValue, List<String> slotsValue) {
        entries = List.copyOf(entriesValue);
        slots = List.copyOf(slotsValue);
        version++;
    }

    /** Fichas de las tecnicas conocidas, en orden estable. */
    public static List<JutsuBookEntry> entries() {
        return entries;
    }

    /** Reparto observado de la rueda ({@code ""} = hueco libre). */
    public static List<String> slots() {
        return slots;
    }

    /** Jutsu del hueco dado, o vacio si esta libre o el indice cae fuera. */
    public static Optional<String> slot(int index) {
        if (index < 0 || index >= slots.size()) {
            return Optional.empty();
        }
        String id = slots.get(index);
        return id.isEmpty() ? Optional.empty() : Optional.of(id);
    }

    /** Categorias presentes entre las tecnicas conocidas, en orden estable. Es por lo que filtra el libro. */
    public static List<String> categories() {
        return entries.stream().map(JutsuBookEntry::category).distinct().sorted().toList();
    }

    /** Contador de version: sube en cada {@link #set}/{@link #clear}; la Screen detecta cambios con el. */
    public static int version() {
        return version;
    }

    /** Limpia la cache (al cambiar de mundo / desconectar). */
    public static void clear() {
        entries = List.of();
        slots = List.of();
        version++;
    }
}
