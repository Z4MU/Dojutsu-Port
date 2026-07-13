package com.zamu.dojutsu.neoforge.platform;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.platform.Registrar;
import com.zamu.dojutsu.platform.RegistryEntry;
import com.zamu.dojutsu.platform.SimpleRegistryEntry;
import dev.architectury.platform.hooks.EventBusesHooks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Registrar de NeoForge respaldado por un {@code DeferredRegister}. Las entradas se
 * acumulan en {@link #register} y se enganchan al bus de eventos del mod en
 * {@link #commit()}, dentro de la ventana de construccion del mod (antes de que se
 * dispare {@code RegisterEvent}).
 *
 * @param <T> tipo de los elementos del registro
 */
public final class NeoForgeRegistrar<T> implements Registrar<T> {

    private final DeferredRegister<T> deferredRegister;

    NeoForgeRegistrar(ResourceKey<? extends Registry<T>> registryKey) {
        this.deferredRegister = DeferredRegister.create(registryKey, DojutsuMod.MOD_ID);
    }

    @Override
    public <R extends T> RegistryEntry<R> register(ResourceLocation id, Supplier<R> factory) {
        DeferredHolder<T, R> holder = deferredRegister.register(id.getPath(), factory);
        return new SimpleRegistryEntry<>(id, holder::get);
    }

    @Override
    public void commit() {
        IEventBus modEventBus = EventBusesHooks.getModEventBus(DojutsuMod.MOD_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Bus de eventos del mod no disponible para " + DojutsuMod.MOD_ID));
        deferredRegister.register(modEventBus);
    }
}
