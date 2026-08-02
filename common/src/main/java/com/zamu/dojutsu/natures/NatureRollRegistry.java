package com.zamu.dojutsu.natures;

/**
 * Registro de la tabla de asignacion aleatoria ({@link NatureRollTable}) cargada desde
 * {@code data/dojutsu/rng/nature_roll.json}. La puebla {@link NatureRollReloadListener} en cada carga y en
 * {@code /reload}; la consulta {@link NatureRoll}.
 *
 * <p>Mismo patron que {@code KekkeiGenkaiRegistry}/{@code UnlockRegistry}, pero con <b>una sola</b> tabla.
 */
public final class NatureRollRegistry {

    private static volatile NatureRollTable table = NatureRollTable.EMPTY;

    private NatureRollRegistry() {
    }

    /** Reemplaza la tabla cargada (lo llama el reload listener). */
    public static void load(NatureRollTable loaded) {
        table = loaded;
    }

    /** Tabla cargada; {@link NatureRollTable#EMPTY} si el datapack no la trae (no se otorgaria nada). */
    public static NatureRollTable table() {
        return table;
    }
}
