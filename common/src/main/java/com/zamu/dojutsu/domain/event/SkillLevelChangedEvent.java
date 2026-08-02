package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Cambio el nivel de una habilidad/afinidad de un jugador. Emisor: leveling/skill points; suscriptores:
 * desbloqueos (S20), recalculo de taijutsu. {@code [ADD/05 §2]}.
 *
 * @param player   jugador afectado
 * @param skillId  id data-driven de la habilidad/afinidad
 * @param oldLevel nivel anterior
 * @param newLevel nivel nuevo
 */
public record SkillLevelChangedEvent(UUID player, String skillId, int oldLevel, int newLevel)
        implements DomainEvent {

    public SkillLevelChangedEvent {
        Objects.requireNonNull(player, "player");
        DomainEvent.requireId(skillId, "skillId");
    }
}
