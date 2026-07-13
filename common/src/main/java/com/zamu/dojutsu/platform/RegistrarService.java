package com.zamu.dojutsu.platform;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

/**
 * Puerto de registro diferido, independiente del loader. Permite al codigo comun
 * declarar contenido (items, entidades, atributos, sonidos, particulas...) sin
 * conocer el mecanismo de registro de NeoForge o Fabric.
 *
 * <p>Cada loader aporta su implementacion (resuelta por {@link PlatformServices}
 * via {@link java.util.ServiceLoader}): NeoForge usa {@code DeferredRegister};
 * Fabric registra de forma inmediata.
 */
public interface RegistrarService {

    /**
     * Crea un {@link Registrar} para el registro identificado por {@code registryKey}
     * (p. ej. {@code Registries.ITEM}).
     *
     * @param registryKey clave del registro destino
     * @param <T>         tipo de los elementos del registro
     * @return un registrar sobre el que declarar entradas
     */
    <T> Registrar<T> create(ResourceKey<? extends Registry<T>> registryKey);
}
