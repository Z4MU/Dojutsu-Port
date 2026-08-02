package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Se sello una bestia con cola en un jinchuriki. Emisor: {@code SealingService} (F7/DJT-165).
 *
 * <p>El ADD anotaba aqui "chakra (+8000)"; el datapack presta <b>1000 por cola</b> (1000…10 000), no una cifra
 * fija — ver {@code SealingTable}. El evento es el punto de enganche para lo que reaccione al sellado.
 * {@code [ADD/05 §2]}.
 *
 * @param player  jinchuriki (jugador) que recibe la bestia
 * @param beastId id data-driven de la bestia con cola
 */
public record BeastSealedEvent(UUID player, String beastId) implements DomainEvent {

    public BeastSealedEvent {
        Objects.requireNonNull(player, "player");
        DomainEvent.requireId(beastId, "beastId");
    }
}
