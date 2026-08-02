package com.zamu.dojutsu.data.migration;

import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;

/**
 * Un paso de migracion del estado del jugador: transforma el NBT de una version a la siguiente.
 *
 * <p>Es una <b>funcion pura</b> {@code data->data}: no debe tener efectos secundarios ni depender de nada no
 * determinista (el mismo input produce el mismo output). Opera sobre el NBT (antes de decodificar con el
 * codec) porque ahi es donde viven aun los campos con la forma antigua. No versiona: eso lo gestiona la
 * {@link MigrationChain} (que escribe la nueva version tras aplicar el paso).
 */
public interface Migration {

    /** Version desde la que migra este paso (produce {@code fromVersion + 1}). */
    int fromVersion();

    /** Transforma el NBT al esquema de la version siguiente. No debe mutar el argumento. */
    CompoundTag migrate(CompoundTag input);

    /** Crea un paso a partir de una funcion, para el registro comodo de migraciones. */
    static Migration of(int fromVersion, UnaryOperator<CompoundTag> transform) {
        return new Migration() {
            @Override
            public int fromVersion() {
                return fromVersion;
            }

            @Override
            public CompoundTag migrate(CompoundTag input) {
                return transform.apply(input);
            }
        };
    }
}
