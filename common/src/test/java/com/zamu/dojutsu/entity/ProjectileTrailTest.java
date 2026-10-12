package com.zamu.dojutsu.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests del estilo de estela de los proyectiles (F11/DJT-241). */
class ProjectileTrailTest {

    @Test
    void seLeeDelJsonSinImportarMayusculas() {
        assertEquals(ProjectileTrail.FIRE, ProjectileTrail.byName("fire"));
        assertEquals(ProjectileTrail.FIRE, ProjectileTrail.byName(" FIRE "));
    }

    @Test
    void unNombreDesconocidoOVacioEsSinEstela() {
        // un JSON con una estela que aun no existe no debe romper la carga del jutsu
        assertEquals(ProjectileTrail.NONE, ProjectileTrail.byName("lightning_que_no_existe"));
        assertEquals(ProjectileTrail.NONE, ProjectileTrail.byName(""));
        assertEquals(ProjectileTrail.NONE, ProjectileTrail.byName(null));
    }

    @Test
    void elByteDeRedVaYVuelve() {
        for (ProjectileTrail trail : ProjectileTrail.values()) {
            assertEquals(trail, ProjectileTrail.fromId(trail.id()));
        }
    }

    @Test
    void unByteFueraDeRangoEsSinEstela() {
        // un servidor con mas estilos que el cliente
        assertEquals(ProjectileTrail.NONE, ProjectileTrail.fromId((byte) 99));
        assertEquals(ProjectileTrail.NONE, ProjectileTrail.fromId((byte) -1));
    }

    @Test
    void elCrepitarDeVueloEmpiezaConLaEstelaYSeRepite() {
        // F11/DJT-242: no suena pegado a la cara de quien lanza, y luego cada FLIGHT_SOUND_INTERVAL_TICKS
        assertEquals(false, ProjectileTrail.flightSoundDue(0));
        assertEquals(false, ProjectileTrail.flightSoundDue(1));
        assertEquals(true, ProjectileTrail.flightSoundDue(ProjectileTrail.FLIGHT_SOUND_FIRST_TICK));
        assertEquals(false, ProjectileTrail.flightSoundDue(ProjectileTrail.FLIGHT_SOUND_FIRST_TICK + 1));
        assertEquals(true, ProjectileTrail.flightSoundDue(
                ProjectileTrail.FLIGHT_SOUND_FIRST_TICK + ProjectileTrail.FLIGHT_SOUND_INTERVAL_TICKS));
    }

    @Test
    void enCincoSegundosDeVueloSuenaUnPunadoDeVeces() {
        // ni silencio ni un enjambre de voces: 100 ticks de vida dan 10 crepitares
        int count = 0;
        for (int tick = 0; tick < 100; tick++) {
            if (ProjectileTrail.flightSoundDue(tick)) {
                count++;
            }
        }
        assertEquals(10, count);
    }
}
