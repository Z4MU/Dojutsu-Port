package com.zamu.dojutsu.domain.items;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests de la Bomba de Humo (F10/DJT-225). Verificado contra {@code items/smoke_bomb/use.mcfunction} y
 * {@code items/smoke_bomb/smoke.mcfunction}.
 */
class SmokeBombTableTest {

    @Test
    void laCortinaDuraSeisSegundos() {
        // use: "scoreboard players set ... d.smokebombtime 120"
        assertEquals(120, SmokeBombTable.DURATION_TICKS);
        assertEquals(6, SmokeBombTable.DURATION_TICKS / 20);
    }

    @Test
    void laNubeEsCienParticulasPorTickEnDosBloquesDeRadio() {
        // smoke: "particle large_smoke ~ ~1 ~ 2 2 2 0.05 100 force"
        assertEquals(100, SmokeBombTable.PARTICLE_COUNT);
        assertEquals(2.0D, SmokeBombTable.PARTICLE_SPREAD);
        assertEquals(0.05D, SmokeBombTable.PARTICLE_SPEED);
        assertEquals(1.0D, SmokeBombTable.PARTICLE_HEIGHT);
    }

    @Test
    void elMarcadorNaceMedioBloqueSobreLaBomba() {
        // use: "summon marker ~ ~.5 ~"
        assertEquals(0.5D, SmokeBombTable.SPAWN_HEIGHT);
    }

    @Test
    void elChisporroteoSeOyeADiezBloques() {
        // smoke: "playsound ... @a[distance=0..10] ~ ~ ~ 0.2 1.5"
        assertEquals(10.0D, SmokeBombTable.SOUND_RADIUS);
        assertEquals(0.2F, SmokeBombTable.SOUND_VOLUME);
        assertEquals(1.5F, SmokeBombTable.SOUND_PITCH);
    }
}
