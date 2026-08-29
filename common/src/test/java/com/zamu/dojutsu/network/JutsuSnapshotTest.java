package com.zamu.dojutsu.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests de la proyeccion observable de jutsus (F11/DJT-231).
 *
 * <p>Lo que se protege aqui es la decision de tamano: la lista de aprendidos <b>solo</b> viaja cuando cambia.
 * Los deltas los dispara la regeneracion de chakra, asi que meterla en todos seria mandarla constantemente.
 */
class JutsuSnapshotTest {

    @Test
    void elSnapshotVacioNoTraeLista() {
        assertTrue(JutsuSnapshot.EMPTY.known().isEmpty());
        assertTrue(JutsuSnapshot.EMPTY.cooldowns().isEmpty());
    }

    @Test
    void elDeltaCorrienteNoLlevaLaLista() {
        JutsuSnapshot delta = JutsuSnapshot.withoutKnown(Map.of("chidori", 1400L), Optional.empty());

        assertTrue(delta.known().isEmpty(), "el delta corriente no debe arrastrar los jutsus aprendidos");
        assertEquals(1, delta.cooldowns().size());
    }

    @Test
    void elSnapshotCompletoSiLlevaEllaLista() {
        JutsuSnapshot full = JutsuSnapshot.withKnown(Map.of(), Set.of("chidori", "great_fireball"), Optional.empty());

        assertEquals(List.of("chidori", "great_fireball"), full.known().orElseThrow());
    }

    @Test
    void laListaViajaEnOrdenEstable() {
        // el selector la dibuja tal cual: si el orden cambiara entre syncs, los jutsus bailarian en pantalla
        JutsuSnapshot a = JutsuSnapshot.withKnown(Map.of(), Set.of("rasengan", "chidori", "great_fireball"), Optional.empty());
        JutsuSnapshot b = JutsuSnapshot.withKnown(Map.of(), Set.of("great_fireball", "rasengan", "chidori"), Optional.empty());

        assertEquals(a.known().orElseThrow(), b.known().orElseThrow());
        assertEquals(List.of("chidori", "great_fireball", "rasengan"), a.known().orElseThrow());
    }

    // --- La rueda (F11/DJT-233) tiene el mismo tratamiento que la lista de aprendidos ---

    @Test
    void elDeltaCorrienteTampocoLlevaLaRueda() {
        JutsuSnapshot delta = JutsuSnapshot.withoutKnown(Map.of("chidori", 1400L), Optional.empty());

        assertTrue(delta.slots().isEmpty(), "el reparto de la rueda no cambia con la regeneracion de chakra");
    }

    @Test
    void lasDosListasViajanPorSeparado() {
        // asignar un hueco no ensena ningun jutsu: debe poder mandarse la rueda sin la lista entera
        JutsuSnapshot soloRueda = JutsuSnapshot.of(Map.of(), Optional.empty(), Optional.empty(),
                Optional.of(List.of("chidori", "", "", "", "", "", "", "")));

        assertTrue(soloRueda.known().isEmpty());
        assertEquals("chidori", soloRueda.slots().orElseThrow().get(0));
    }

    @Test
    void elSnapshotEsInmutableAunqueLeDenColeccionesMutables() {
        Map<String, Long> cooldowns = new java.util.HashMap<>(Map.of("chidori", 1400L));
        Set<String> known = new java.util.HashSet<>(Set.of("chidori"));

        JutsuSnapshot snapshot = JutsuSnapshot.withKnown(cooldowns, known, Optional.empty());
        cooldowns.put("rasengan", 99L);
        known.add("rasengan");

        assertEquals(1, snapshot.cooldowns().size());
        assertEquals(List.of("chidori"), snapshot.known().orElseThrow());
    }
}
