package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Se otorgo una naturaleza a un jugador. Emisor: {@code NatureService}; suscriptores: progresion (XP),
 * learner (item). {@code [ADD/05 §2]}.
 *
 * @param player   jugador que recibe la naturaleza
 * @param natureId id data-driven de la naturaleza
 */
public record NatureGrantedEvent(UUID player, String natureId) implements DomainEvent {

    public NatureGrantedEvent {
        Objects.requireNonNull(player, "player");
        DomainEvent.requireId(natureId, "natureId");
    }
}
