package com.zamu.dojutsu.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.api.DojutsuRegistryEvents.Contributor;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import dev.architectury.event.Event;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * <b>El criterio de validación de DJT-220</b>: «un mod de prueba registra un jutsu sin tocar el core».
 *
 * <p>Los tests se escriben desde fuera a propósito — sólo usan {@code com.zamu.dojutsu.api} y los tipos que la
 * API expone. Si algún día hiciera falta importar un servicio interno para conseguir algo de aquí, la API se
 * habría quedado corta, y este archivo es el que lo diría.
 *
 * <p>Cada test usa <b>su propio evento</b> ({@link DojutsuRegistryEvents#create}) porque los eventos de
 * Architectury no se pueden desuscribir: suscribirse a {@code JUTSU} desde un test dejaría el aportador puesto
 * para todos los demás. La mecánica que se prueba es la misma.
 *
 * <p>No se cubre lo que necesita un servidor (leer un jugador, cobrar chakra): eso es glue de Minecraft y va al
 * checklist de verificación en partida, como todo lo que no ve un test.
 */
class ThirdPartyModTest {

    private static JutsuEntry unJutsu(String id) {
        return new JutsuEntry(
                new JutsuDefinition(id, 10, 20, List.of(), Optional.empty(), Optional.empty(), 0,
                        "fire", "projectile"),
                new com.google.gson.JsonObject());
    }

    @Test
    void unModDeTerceroPuedeAportarUnJutsu() {
        Event<Contributor<JutsuEntry>> evento = DojutsuRegistryEvents.create();
        Map<String, JutsuEntry> registro = new LinkedHashMap<>();
        registro.put("fire_bullet", unJutsu("fire_bullet")); // lo que ya trae el mod

        evento.register(sink -> sink.add("mimod_llamarada", unJutsu("mimod_llamarada")));
        int aportadas = DojutsuRegistryEvents.contribute(evento, registro);

        assertEquals(1, aportadas);
        assertTrue(registro.containsKey("mimod_llamarada"), "la tecnica del tercero entra en el registro");
        assertTrue(registro.containsKey("fire_bullet"), "y no pisa lo que ya habia");
    }

    @Test
    void variosModsPuedenAportarALaVez() {
        Event<Contributor<JutsuEntry>> evento = DojutsuRegistryEvents.create();
        Map<String, JutsuEntry> registro = new LinkedHashMap<>();

        evento.register(sink -> sink.add("modA_uno", unJutsu("modA_uno")));
        evento.register(sink -> {
            sink.add("modB_uno", unJutsu("modB_uno"));
            sink.add("modB_dos", unJutsu("modB_dos"));
        });

        assertEquals(3, DojutsuRegistryEvents.contribute(evento, registro));
        assertEquals(3, registro.size());
    }

    @Test
    void unTerceroPuedeSustituirContenidoBaseAProposito() {
        // Rebalancear un jutsu del mod es una capacidad declarada, no un accidente: gana el ultimo en escribir.
        Event<Contributor<JutsuEntry>> evento = DojutsuRegistryEvents.create();
        Map<String, JutsuEntry> registro = new LinkedHashMap<>();
        registro.put("fire_bullet", unJutsu("fire_bullet"));

        JutsuEntry mio = unJutsu("fire_bullet");
        evento.register(sink -> sink.add("fire_bullet", mio));
        DojutsuRegistryEvents.contribute(evento, registro);

        assertEquals(1, registro.size());
        assertEquals(mio, registro.get("fire_bullet"));
    }

    @Test
    void unIdVacioSeRechazaEnVezDeEnvenenarElRegistro() {
        Event<Contributor<JutsuEntry>> evento = DojutsuRegistryEvents.create();
        Map<String, JutsuEntry> registro = new LinkedHashMap<>();
        evento.register(sink -> sink.add("  ", unJutsu("x")));

        assertThrows(IllegalArgumentException.class,
                () -> DojutsuRegistryEvents.contribute(evento, registro));
        assertTrue(registro.isEmpty());
    }

    @Test
    void unValorNuloSeRechaza() {
        Event<Contributor<JutsuEntry>> evento = DojutsuRegistryEvents.create();
        evento.register(sink -> sink.add("roto", null));

        assertThrows(NullPointerException.class,
                () -> DojutsuRegistryEvents.contribute(evento, new LinkedHashMap<>()));
    }

    @Test
    void sinSuscriptoresNoSeAportaNada() {
        Event<Contributor<JutsuEntry>> evento = DojutsuRegistryEvents.create();
        Map<String, JutsuEntry> registro = new LinkedHashMap<>();
        registro.put("fire_bullet", unJutsu("fire_bullet"));

        assertEquals(0, DojutsuRegistryEvents.contribute(evento, registro));
        assertEquals(1, registro.size(), "lo del mod sigue intacto");
    }

    @Test
    void losCuatroEventosDeContenidoExisten() {
        // Son los cuatro tipos de contenido data-driven del mod; si alguien anade uno, va aqui.
        assertTrue(DojutsuRegistryEvents.JUTSU != null && DojutsuRegistryEvents.EYE != null
                && DojutsuRegistryEvents.CLAN != null && DojutsuRegistryEvents.CONTRACT != null);
    }

    @Test
    void laVersionDeLaApiEsSemverYSuMayorSeLeeSola() {
        assertTrue(DojutsuApi.API_VERSION.matches("\\d+\\.\\d+\\.\\d+"), DojutsuApi.API_VERSION);
        assertEquals(1, DojutsuApi.majorVersion());
    }

    @Test
    void laApiNoExponeElAgregadoMutable() {
        // NinjaView es de solo lectura. Si alguien le anadiera un getter que devolviera el PlayerNinjaData, un
        // tercero podria escribir el estado saltandose las puertas del mod — que es justo lo que no debe pasar.
        boolean tieneFugas = java.util.Arrays.stream(NinjaView.class.getMethods())
                .anyMatch(m -> m.getReturnType().getSimpleName().equals("PlayerNinjaData"));
        assertFalse(tieneFugas, "NinjaView no debe devolver el agregado real");
    }
}
