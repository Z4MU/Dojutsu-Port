package com.zamu.dojutsu.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Version del esquema de {@link PlayerNinjaData} (el analogo del {@code d.version}=16 del datapack; el mod
 * arranca su propio versionado desde {@link #CURRENT}).
 *
 * <p>Centraliza la clave NBT y la lectura de la version almacenada. Un NBT sin la clave se interpreta como
 * {@link #INITIAL} (esquema previo al versionado), para que la migracion sepa que debe procesarlo.
 */
public final class DataVersion {

    /** Clave NBT donde se guarda la version del esquema. */
    public static final String KEY = "dataVersion";

    /** Version asumida cuando el NBT no trae la clave (esquema mas antiguo). */
    public static final int INITIAL = PlayerNinjaData.INITIAL_DATA_VERSION;

    /** Version del esquema que produce el codigo actual. */
    public static final int CURRENT = PlayerNinjaData.CURRENT_DATA_VERSION;

    private DataVersion() {
    }

    /** Lee la version del NBT; si la clave no esta presente, devuelve {@link #INITIAL}. */
    public static int read(CompoundTag tag) {
        return tag.contains(KEY) ? tag.getInt(KEY) : INITIAL;
    }
}
