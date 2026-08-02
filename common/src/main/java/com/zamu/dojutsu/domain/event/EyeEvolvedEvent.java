package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Un ojo evoluciono a una forma superior. Emisor: servicio de evolucion; suscriptores: progresion (XP),
 * notificaciones. {@code [ADD/05 §2]}.
 *
 * @param player   jugador afectado
 * @param fromEye  id data-driven del ojo previo
 * @param toEye    id data-driven del ojo resultante
 */
public record EyeEvolvedEvent(UUID player, String fromEye, String toEye) implements DomainEvent {

    public EyeEvolvedEvent {
        Objects.requireNonNull(player, "player");
        DomainEvent.requireId(fromEye, "fromEye");
        DomainEvent.requireId(toEye, "toEye");
    }
}
