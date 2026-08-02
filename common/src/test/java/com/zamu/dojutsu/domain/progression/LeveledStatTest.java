package com.zamu.dojutsu.domain.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests de la estadistica con nivel+xp por bandas (DJT-014, RA-4) e inmutabilidad (DJT-063). */
class LeveledStatTest {

    @Test
    void nuevaEstadisticaEmpiezaEnCero() {
        LeveledStat s = new LeveledStat();
        assertEquals(0, s.level());
        assertEquals(0, s.xp());
        assertFalse(s.isMaxLevel());
    }

    @Test
    void xpPorDebajoDelUmbralNoSube() {
        LeveledStat s = new LeveledStat().withXpAdded(9); // umbral del nivel 0 es 10
        assertEquals(0, s.level());
        assertEquals(9, s.xp());
    }

    @Test
    void xpExactaSubeUnNivelSinSobrante() {
        LeveledStat s = new LeveledStat().withXpAdded(10);
        assertEquals(1, s.level());
        assertEquals(0, s.xp());
    }

    @Test
    void arrastraElSobranteTrasSubir() {
        LeveledStat s = new LeveledStat().withXpAdded(13); // 10 sube a nivel 1, sobran 3
        assertEquals(1, s.level());
        assertEquals(3, s.xp());
    }

    @Test
    void unaSolaLlamadaSubeVariosNiveles() {
        // Sin guardia de 1/tick: 35 XP desde nivel 0. Los niveles 0-10 cuestan 10 cada uno.
        // 35 -10(niv0)=25 -> niv1 ; -10(niv1)=15 -> niv2 ; -10(niv2)=5 -> niv3 ; 5 < 10 para.
        LeveledStat s = new LeveledStat().withXpAdded(35);
        assertEquals(3, s.level());
        assertEquals(5, s.xp());
    }

    @Test
    void elSaltoUsaElCosteDeCadaBandaAtravesada() {
        // De nivel 9 a traves de la frontera 10/11: coste 10 en niveles 0-10, 15 en 11-20.
        LeveledStat s = new LeveledStat(9, 0).withXpAdded(10 + 10 + 15); // 9->10 (10), 10->11 (10), 11->12 (15)
        assertEquals(12, s.level());
        assertEquals(0, s.xp());
    }

    @Test
    void alcanzarElCapDetieneLaSubidaYDescartaSobrante() {
        LeveledStat s = new LeveledStat(499, 0).withXpAdded(100 + 999); // 100 sube a 500; el resto se descarta
        assertEquals(500, s.level());
        assertEquals(0, s.xp());
        assertTrue(s.isMaxLevel());
    }

    @Test
    void enElCapNoAcumulaMasXp() {
        LeveledStat cap = new LeveledStat(500, 0);
        LeveledStat s = cap.withXpAdded(1000);
        assertEquals(500, s.level());
        assertEquals(0, s.xp());
        assertSame(cap, s); // en el cap no hay progreso: se devuelve la misma instancia
    }

    @Test
    void xpNegativaEsRechazada() {
        LeveledStat s = new LeveledStat();
        assertThrows(IllegalArgumentException.class, () -> s.withXpAdded(-1));
    }

    @Test
    void constructorRechazaEstadoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new LeveledStat(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new LeveledStat(501, 0));
        assertThrows(IllegalArgumentException.class, () -> new LeveledStat(0, -1));
    }

    @Test
    void sumarXpNoModificaLaInstanciaOriginal() {
        // DJT-063: la razon de ser del ticket. Antes esto corrompia la instancia compartida.
        LeveledStat original = new LeveledStat(5, 3);
        LeveledStat subida = original.withXpAdded(50);

        assertEquals(new LeveledStat(5, 3), original); // intacta
        assertNotSame(original, subida);
        assertEquals(10, subida.level());
    }

    @Test
    void igualdadPorValor() {
        assertEquals(new LeveledStat(7, 2), new LeveledStat(7, 2));
        assertEquals(new LeveledStat(7, 2).hashCode(), new LeveledStat(7, 2).hashCode());
        assertFalse(new LeveledStat(7, 2).equals(new LeveledStat(7, 3)));
    }
}
