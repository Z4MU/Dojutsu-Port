package com.zamu.dojutsu.neoforge.platform;

import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistrarService;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Implementacion NeoForge del puerto {@link RegistrarService}. Cada registro se
 * respalda con un {@code DeferredRegister} de NeoForge (ver {@link NeoForgeRegistrar}).
 */
public final class NeoForgeRegistrarService implements RegistrarService {

    @Override
    public <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey) {
        return new NeoForgeRegistrar<>(registryKey);
    }
}
