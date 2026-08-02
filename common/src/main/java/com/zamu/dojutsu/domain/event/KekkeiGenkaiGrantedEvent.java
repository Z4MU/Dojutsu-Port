package com.zamu.dojutsu.domain.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Se otorgo un Kekkei Genkai a un jugador. Emisor: {@code KekkeiGenkaiService}; suscriptor:
 * {@code LearnerService} (entrega el Skill Learner del KG, igual que {@code give/kg/<kg>} llamaba a
 * {@code give/learner/<kg>} en el datapack).
 *
 * <p><b>Noveno evento del dominio</b> (los 8 de F1/DJT-016 estan en {@code [ADD/05 §2]}, que no preveia uno de
 * KG). Se anade en DJT-055 porque el KG se otorga por <b>varios caminos</b> (comando, prestamo de bijuu,
 * onboarding, clan) y el learner debe llegar en todos: sin evento, la entrega habria que repetirla en cada
 * llamador — el acoplamiento que el bus existe para evitar. La decision quedo diferida a este ticket en la
 * entrada de DJT-052 del development_log.
 *
 * @param player         jugador que recibe el KG
 * @param kekkeiGenkaiId id data-driven del KG ({@code "wood"}, {@code "jugo_clan"}, ...)
 */
public record KekkeiGenkaiGrantedEvent(UUID player, String kekkeiGenkaiId) implements DomainEvent {

    public KekkeiGenkaiGrantedEvent {
        Objects.requireNonNull(player, "player");
        DomainEvent.requireId(kekkeiGenkaiId, "kekkeiGenkaiId");
    }
}
