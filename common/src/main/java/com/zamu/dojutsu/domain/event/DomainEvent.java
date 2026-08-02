package com.zamu.dojutsu.domain.event;

import java.util.Objects;

/**
 * Marcador de los eventos internos del dominio {@code [ADD/05 §2]}.
 *
 * <p>Los eventos son <b>sincronos y del lado servidor</b>: no cruzan la red (la sync al cliente es un paso
 * aparte). Sirven para desacoplar emisor y observadores. Dominio puro: la identidad se referencia por
 * {@link java.util.UUID} y los conceptos de dominio por id data-driven ({@code String}).
 */
public interface DomainEvent {

    /** Valida un id data-driven (no nulo ni vacio) para los constructores de eventos. */
    static String requireId(String id, String name) {
        Objects.requireNonNull(id, name);
        if (id.isBlank()) {
            throw new IllegalArgumentException(name + " no puede estar vacio");
        }
        return id;
    }
}
