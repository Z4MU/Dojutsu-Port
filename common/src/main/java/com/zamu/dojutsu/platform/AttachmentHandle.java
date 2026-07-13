package com.zamu.dojutsu.platform;

import net.minecraft.world.entity.Entity;

/**
 * Manejador de lectura/escritura de un attachment concreto sobre una entidad.
 * Obtenido de {@link AttachmentService#register}.
 *
 * @param <T> tipo del valor adjunto
 */
public interface AttachmentHandle<T> {

    /**
     * Devuelve el valor adjunto a la entidad, creando el valor por defecto si aun no existe.
     *
     * @param holder entidad portadora (p. ej. un jugador)
     * @return el valor actual (nunca nulo si el default no lo es)
     */
    T get(Entity holder);

    /**
     * Asigna el valor adjunto a la entidad.
     *
     * @param holder entidad portadora
     * @param value  nuevo valor
     */
    void set(Entity holder, T value);
}
