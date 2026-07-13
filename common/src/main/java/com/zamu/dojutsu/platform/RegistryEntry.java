package com.zamu.dojutsu.platform;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Referencia perezosa a un objeto registrado. Es un {@link Supplier} del objeto mas
 * su identificador. Permite mantener una referencia estable antes de que el objeto
 * exista realmente (registro diferido de NeoForge).
 *
 * @param <T> tipo del objeto registrado
 */
public interface RegistryEntry<T> extends Supplier<T> {

    /** @return el identificador con el que se registro el objeto. */
    ResourceLocation id();
}
