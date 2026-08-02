package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Se lanzo una tecnica. Emisor: pipeline de cast; suscriptores: registro de ultima tecnica (copia
 * Sharingan), progresion (XP de afinidad). {@code [ADD/05 §2]}.
 *
 * <p>El caster es un {@link UUID} generico: puede ser un jugador o un NPC con IA (S17-R3).
 *
 * @param caster  entidad que lanza la tecnica
 * @param jutsuId id data-driven de la tecnica
 */
public record JutsuCastEvent(UUID caster, String jutsuId) implements DomainEvent {

    public JutsuCastEvent {
        Objects.requireNonNull(caster, "caster");
        DomainEvent.requireId(jutsuId, "jutsuId");
    }
}
