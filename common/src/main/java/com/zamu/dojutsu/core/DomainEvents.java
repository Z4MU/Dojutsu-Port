package com.zamu.dojutsu.core;

import com.zamu.dojutsu.domain.event.DomainEventBus;

/**
 * Punto de acceso al {@link DomainEventBus} <b>compartido del lado servidor</b>. Los sistemas que emiten
 * eventos internos ({@code XpApi}, etc.) publican aqui; los que reaccionan ({@code RankService},
 * {@code UnlockService}, ...) se suscriben una vez en el arranque del mod.
 *
 * <p>Una unica instancia por proceso (servidor integrado o dedicado): el bus solo mantiene registros de
 * handlers sin estado, validos durante toda la vida del proceso. Es infraestructura de {@code core}; el bus en
 * si es dominio puro.
 */
public final class DomainEvents {

    private static final DomainEventBus BUS = new DomainEventBus();

    private DomainEvents() {
    }

    /** Bus de eventos internos del dominio (server-side, sincrono). */
    public static DomainEventBus bus() {
        return BUS;
    }
}
