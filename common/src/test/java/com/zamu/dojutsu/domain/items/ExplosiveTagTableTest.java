package com.zamu.dojutsu.domain.items;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.throwing.ThrowingTable;
import org.junit.jupiter.api.Test;

/**
 * Tests del Sello Explosivo suelto (F10/DJT-225). Verificado contra {@code items/explosive_tag/place.mcfunction},
 * {@code items/explosive_tag/main.mcfunction} y los predicates {@code items/extagtimer{,_0}}.
 */
class ExplosiveTagTableTest {

    @Test
    void tardaDiezSegundosEnArmarse() {
        // place: "scoreboard players set @s d.extagtimer 200"
        assertEquals(200, ExplosiveTagTable.ARM_TICKS);
        assertEquals(10, ExplosiveTagTable.ARM_TICKS / 20);
    }

    @Test
    void soloEstaArmadoCuandoElContadorLlegaACero() {
        // los dos predicates del datapack: extagtimer = {min:1} (contando), extagtimer_0 = 0 (cebado)
        assertFalse(ExplosiveTagTable.isArmed(ExplosiveTagTable.ARM_TICKS));
        assertFalse(ExplosiveTagTable.isArmed(1));
        assertTrue(ExplosiveTagTable.isArmed(0));
    }

    @Test
    void unContadorNegativoTambienCuentaComoArmado() {
        // tolerancia: un NBT corrupto no deja una mina inerte para siempre
        assertTrue(ExplosiveTagTable.isArmed(-5));
    }

    @Test
    void detectaYDaniaEnElMismoRadio() {
        // main: "@e[distance=0..5]" para disparar y "@a[distance=0..5]" para el instant_damage
        assertEquals(5.0D, ExplosiveTagTable.TRIGGER_RADIUS);
    }

    @Test
    void laExplosionEsLaMismaQueLaDelKunaiConSello() {
        // los dos summonean el mismo "tnt_minecart{TNTFuse:0}" parado: potencia 4.0 exacta
        assertEquals(4.0F, ExplosiveTagTable.EXPLOSION_POWER);
        assertEquals(ThrowingTable.EXPLOSIVE_POWER, ExplosiveTagTable.EXPLOSION_POWER);
    }

    @Test
    void ademasRepartUnGolpeMagicoDeUnSegundo() {
        // main: "effect give @a[distance=0..5] instant_damage 1 1" -> 1 s, amplificador 1 = Daño II
        assertEquals(1, ExplosiveTagTable.HARM_AMPLIFIER);
        assertEquals(20, ExplosiveTagTable.HARM_DURATION_TICKS);
    }
}
