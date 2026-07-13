package com.zamu.dojutsu.fabric.platform;

import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistrarService;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;

/**
 * Implementacion Fabric del puerto {@link RegistrarService}. Resuelve el registro
 * destino desde el registro raiz y registra las entradas de forma inmediata
 * (ver {@link FabricRegistrar}).
 */
public final class FabricRegistrarService implements RegistrarService {

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey) {
        Registry<T> registry = (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
        if (registry == null) {
            throw new IllegalStateException("Registro no encontrado: " + registryKey.location());
        }
        return new FabricRegistrar<>(registry);
    }
}
