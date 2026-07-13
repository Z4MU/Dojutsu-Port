package com.zamu.dojutsu.platform;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Acumulador de registros para un registro concreto. Las entradas se declaran con
 * {@link #register(ResourceLocation, Supplier)} y se confirman una vez con
 * {@link #commit()}.
 *
 * @param <T> tipo de los elementos del registro
 */
public interface Registrar<T> {

    /**
     * Declara una entrada del registro.
     *
     * @param id      identificador (namespace del mod + ruta)
     * @param factory proveedor perezoso del objeto a registrar
     * @param <R>     subtipo concreto de la entrada
     * @return una referencia perezosa al objeto registrado
     */
    <R extends T> RegistryEntry<R> register(ResourceLocation id, Supplier<R> factory);

    /**
     * Confirma las entradas declaradas. En NeoForge engancha el {@code DeferredRegister}
     * al bus del mod; en Fabric no hace nada (el registro es inmediato). Debe llamarse
     * una sola vez durante la inicializacion del mod.
     */
    void commit();
}
