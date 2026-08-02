package com.zamu.dojutsu.domain.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/** Tests del bus de eventos internos del dominio (DJT-016). */
class DomainEventBusTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static JutsuCastEvent jutsu() {
        return new JutsuCastEvent(PLAYER, "rasengan");
    }

    @Test
    void elSuscriptorRecibeElEventoPublicado() {
        DomainEventBus bus = new DomainEventBus();
        List<JutsuCastEvent> received = new ArrayList<>();
        bus.subscribe(JutsuCastEvent.class, received::add);
        JutsuCastEvent event = jutsu();
        bus.publish(event);
        assertEquals(List.of(event), received);
    }

    @Test
    void publicarSinSuscriptoresNoRompe() {
        DomainEventBus bus = new DomainEventBus();
        bus.publish(jutsu()); // no debe lanzar
    }

    @Test
    void soloRecibeSuTipo() {
        DomainEventBus bus = new DomainEventBus();
        List<DomainEvent> received = new ArrayList<>();
        bus.subscribe(NinjaXpChangedEvent.class, received::add);
        bus.publish(jutsu()); // otro tipo
        assertTrue(received.isEmpty());
    }

    @Test
    void suscribirseAlaBaseRecibeTodosLosEventos() {
        DomainEventBus bus = new DomainEventBus();
        List<DomainEvent> received = new ArrayList<>();
        bus.subscribe(DomainEvent.class, received::add);
        bus.publish(jutsu());
        bus.publish(new NinjaXpChangedEvent(PLAYER, 0, 100));
        assertEquals(2, received.size());
    }

    @Test
    void elFalloDeUnHandlerNoRompeALosDemas() {
        DomainEventBus bus = new DomainEventBus();
        List<String> log = new ArrayList<>();
        bus.subscribe(JutsuCastEvent.class, e -> {
            throw new RuntimeException("boom");
        });
        bus.subscribe(JutsuCastEvent.class, e -> log.add("segundo"));
        bus.publish(jutsu());
        assertEquals(List.of("segundo"), log, "el segundo handler corre pese al fallo del primero");
    }

    @Test
    void variosHandlersRespetanElOrdenDeRegistro() {
        DomainEventBus bus = new DomainEventBus();
        List<String> log = new ArrayList<>();
        bus.subscribe(JutsuCastEvent.class, e -> log.add("a"));
        bus.subscribe(JutsuCastEvent.class, e -> log.add("b"));
        bus.publish(jutsu());
        assertEquals(List.of("a", "b"), log);
    }

    @Test
    void unsubscribeQuitaElHandler() {
        DomainEventBus bus = new DomainEventBus();
        List<DomainEvent> received = new ArrayList<>();
        Consumer<JutsuCastEvent> handler = received::add;
        bus.subscribe(JutsuCastEvent.class, handler);
        bus.unsubscribe(JutsuCastEvent.class, handler);
        bus.publish(jutsu());
        assertTrue(received.isEmpty());
    }

    @Test
    void desuscribirDuranteElDispatchNoRompe() {
        DomainEventBus bus = new DomainEventBus();
        List<String> log = new ArrayList<>();
        Consumer<JutsuCastEvent> second = e -> log.add("segundo");
        // El primer handler desuscribe al segundo durante el dispatch: no debe lanzar (instantanea).
        bus.subscribe(JutsuCastEvent.class, e -> {
            log.add("primero");
            bus.unsubscribe(JutsuCastEvent.class, second);
        });
        bus.subscribe(JutsuCastEvent.class, second);
        bus.publish(jutsu());
        assertEquals(List.of("primero", "segundo"), log, "la instantanea del dispatch no crashea");
        // En una segunda publicacion, el segundo ya no esta.
        log.clear();
        bus.publish(jutsu());
        assertEquals(List.of("primero"), log);
    }

    @Test
    void reentranciaSincronaFunciona() {
        DomainEventBus bus = new DomainEventBus();
        List<String> log = new ArrayList<>();
        bus.subscribe(JutsuCastEvent.class, e -> {
            log.add("jutsu");
            bus.publish(new NinjaXpChangedEvent(PLAYER, 0, 10)); // publica dentro de un handler
        });
        bus.subscribe(NinjaXpChangedEvent.class, e -> log.add("xp"));
        bus.publish(jutsu());
        assertEquals(List.of("jutsu", "xp"), log);
    }

    @Test
    void losEventosValidanSusDatos() {
        assertThrows(NullPointerException.class, () -> new JutsuCastEvent(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> new JutsuCastEvent(PLAYER, " "));
        assertThrows(IllegalArgumentException.class, () -> new EyeEvolvedEvent(PLAYER, "a", ""));
    }
}
