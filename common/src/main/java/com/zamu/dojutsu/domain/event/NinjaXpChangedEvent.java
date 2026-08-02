package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Cambio la XP ninja global de un jugador. Emisor: cualquier sumador de XP; suscriptor: servicio de rango
 * (recalcula el rango, S20). {@code [ADD/05 §2]}.
 *
 * @param player jugador afectado
 * @param oldXp  XP ninja anterior
 * @param newXp  XP ninja nueva
 */
public record NinjaXpChangedEvent(UUID player, int oldXp, int newXp) implements DomainEvent {

    public NinjaXpChangedEvent {
        Objects.requireNonNull(player, "player");
    }
}
