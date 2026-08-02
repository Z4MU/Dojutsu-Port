package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Un jugador desperto un ojo (dojutsu). Emisor: servicio de despertar; suscriptores: progresion (XP),
 * notificaciones. {@code [ADD/05 §2]}.
 *
 * @param player jugador que desperto el ojo
 * @param eyeId  id data-driven del ojo
 */
public record EyeAwakenedEvent(UUID player, String eyeId) implements DomainEvent {

    public EyeAwakenedEvent {
        Objects.requireNonNull(player, "player");
        DomainEvent.requireId(eyeId, "eyeId");
    }
}
