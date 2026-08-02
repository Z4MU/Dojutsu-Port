package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Se activo o desactivo un modo (sage, sellos, puertas, susanoo, forma...). Emisor: servicio de
 * {@code ToggleableMode}; suscriptores: sync del HUD, exclusiones (forma<->ojo). {@code [ADD/05 §2]}.
 *
 * @param player jugador afectado
 * @param modeId id data-driven del modo
 * @param active {@code true} si quedo activo; {@code false} si se desactivo
 */
public record ModeToggledEvent(UUID player, String modeId, boolean active) implements DomainEvent {

    public ModeToggledEvent {
        Objects.requireNonNull(player, "player");
        DomainEvent.requireId(modeId, "modeId");
    }
}
