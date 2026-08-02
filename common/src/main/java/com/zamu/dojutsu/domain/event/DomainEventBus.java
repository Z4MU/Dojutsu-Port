package com.zamu.dojutsu.domain.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bus de eventos internos del dominio: ligero, <b>sincrono y del lado servidor</b> {@code [ADD/05 §2]}.
 *
 * <p>Un sistema publica un {@link DomainEvent}; otros reaccionan sin conocerse (bajo acoplamiento). El
 * dispatch es inmediato en el hilo que publica; nunca es asincrono ni cruza la red.
 *
 * <p>Garantias:
 * <ul>
 *   <li><b>Aislamiento de fallos:</b> si un handler lanza, se registra y los demas siguen ejecutandose.</li>
 *   <li><b>Suscripcion por tipo o supertipo:</b> un handler recibe el evento si su tipo registrado es
 *       asignable desde el evento (p. ej. suscribirse a {@link DomainEvent} recibe todos).</li>
 *   <li><b>Seguro ante (des)suscripcion y reentrancia durante el dispatch:</b> se notifica sobre una
 *       instantanea de los handlers, y las estructuras son concurrentes.</li>
 * </ul>
 *
 * <p>Dominio puro (usa {@link System.Logger} del JDK, no el logger de Mojang).
 */
public final class DomainEventBus {

    private static final System.Logger LOGGER = System.getLogger("dojutsu.domain.event");

    private final Map<Class<?>, List<Consumer<?>>> handlersByType = new ConcurrentHashMap<>();

    /** Suscribe un handler a un tipo de evento (o supertipo). */
    public <E extends DomainEvent> void subscribe(Class<E> type, Consumer<? super E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        handlersByType.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /** Desuscribe un handler previamente registrado. */
    public <E extends DomainEvent> void unsubscribe(Class<E> type, Consumer<? super E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        List<Consumer<?>> handlers = handlersByType.get(type);
        if (handlers != null) {
            handlers.remove(handler);
        }
    }

    /**
     * Publica un evento y notifica sincronamente a todos los handlers cuyo tipo lo acepte. El fallo de un
     * handler no impide notificar a los demas.
     */
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event");
        // Instantanea de handlers a notificar: tolera (des)suscripcion y reentrancia durante el dispatch.
        List<Consumer<?>> toNotify = new ArrayList<>();
        for (Map.Entry<Class<?>, List<Consumer<?>>> entry : handlersByType.entrySet()) {
            if (entry.getKey().isInstance(event)) {
                toNotify.addAll(entry.getValue());
            }
        }
        for (Consumer<?> handler : toNotify) {
            try {
                @SuppressWarnings("unchecked")
                Consumer<DomainEvent> typed = (Consumer<DomainEvent>) handler;
                typed.accept(event);
            } catch (RuntimeException ex) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "Handler de evento de dominio fallo para " + event, ex);
            }
        }
    }
}
