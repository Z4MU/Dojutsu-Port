package com.zamu.dojutsu.fabric.platform;

import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import com.zamu.dojutsu.platform.SimpleRegistryEntry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Registrar de Fabric. Fabric permite registrar directamente en los registros
 * durante la inicializacion del mod, por lo que cada entrada se crea y registra de
 * inmediato y {@link #commit()} no tiene fase diferida que confirmar.
 *
 * @param <T> tipo de los elementos del registro
 */
public final class FabricRegistrar<T> implements Registrar<T> {

    private final Registry<T> registry;

    FabricRegistrar(Registry<T> registry) {
        this.registry = registry;
    }

    @Override
    public <R extends T> RegistryEntry<R> register(ResourceLocation id, Supplier<R> factory) {
        R value = Registry.register(registry, id, factory.get());
        return new SimpleRegistryEntry<>(id, () -> value);
    }

    @Override
    public void commit() {
        // Fabric registra de forma inmediata; no hay fase diferida que confirmar.
    }
}
