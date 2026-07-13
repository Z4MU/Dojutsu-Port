package com.zamu.dojutsu.platform;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Puerto de acceso a Data Attachments por entidad, independiente del loader. Unifica
 * el {@code AttachmentType} de NeoForge y el {@code AttachmentRegistry} de Fabric tras
 * una sola interfaz (ver {@code 06_PERSISTENCIA §2}).
 *
 * <p>La implementacion de cada loader se resuelve por {@link PlatformServices} via
 * {@link java.util.ServiceLoader}.
 */
public interface AttachmentService {

    /**
     * Registra un tipo de attachment persistente. Debe invocarse durante la
     * inicializacion del mod.
     *
     * @param id           identificador del attachment (namespace del mod + ruta)
     * @param defaultValue proveedor del valor por defecto (cuando la entidad no tiene dato)
     * @param codec        codec de serializacion para persistir el valor
     * @param copyOnDeath  si {@code true}, el valor se copia al respawnear tras morir
     * @param <T>          tipo del valor adjunto
     * @return un manejador para leer/escribir el attachment sobre una entidad
     */
    <T> AttachmentHandle<T> register(ResourceLocation id, Supplier<T> defaultValue, Codec<T> codec, boolean copyOnDeath);
}
