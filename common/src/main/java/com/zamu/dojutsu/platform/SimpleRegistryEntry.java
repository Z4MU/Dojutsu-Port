package com.zamu.dojutsu.platform;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Implementacion inmutable y compartida de {@link RegistryEntry}: un identificador
 * mas un proveedor del objeto registrado. La usan los registrars de cada loader para
 * no duplicar la referencia perezosa.
 *
 * @param id       identificador del objeto registrado
 * @param delegate proveedor del objeto (holder diferido en NeoForge, valor fijo en Fabric)
 * @param <T>      tipo del objeto registrado
 */
public record SimpleRegistryEntry<T>(ResourceLocation id, Supplier<T> delegate) implements RegistryEntry<T> {

    @Override
    public T get() {
        return delegate.get();
    }
}
