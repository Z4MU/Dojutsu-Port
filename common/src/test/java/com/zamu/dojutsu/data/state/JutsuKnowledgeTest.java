package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

/** Tests del sub-estado de conocimiento de jutsus (F4/DJT-075). */
class JutsuKnowledgeTest {

    @Test
    void elDefaultNoConoceNada() {
        assertTrue(JutsuKnowledge.DEFAULT.known().isEmpty());
        assertEquals(0, JutsuKnowledge.DEFAULT.progressOf("fire"));
        assertFalse(JutsuKnowledge.DEFAULT.knows("great_fireball"));
    }

    @Test
    void withKnownAnadeYEsIdempotente() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withKnown("fire_bullet").withKnown("fire_bullet");
        assertTrue(k.knows("fire_bullet"));
        assertEquals(1, k.known().size());
        // El default no se ve afectado (inmutabilidad, regla 10b).
        assertFalse(JutsuKnowledge.DEFAULT.knows("fire_bullet"));
    }

    @Test
    void elProgresoSeFijaYSeResetea() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withProgress("fire", 3);
        assertEquals(3, k.progressOf("fire"));
        assertEquals(0, k.progressOf("lightning"));
        assertEquals(0, k.withProgressReset("fire").progressOf("fire"));
    }

    @Test
    void elProgresoNegativoEsRechazadoEnElWither() {
        assertThrows(IllegalArgumentException.class,
                () -> JutsuKnowledge.DEFAULT.withProgress("fire", -1));
    }

    @Test
    void elNbtCorruptoConProgresoNegativoSeSanea() {
        // El canonico constructor sanea (codec de estado tolerante: nunca bloquea el save).
        JutsuKnowledge k = new JutsuKnowledge(Set.of(), Map.of("fire", -5), Map.of(), java.util.Optional.empty(), List.of());
        assertEquals(0, k.progressOf("fire"));
    }

    // ------------------------------------------------------------------
    // Cooldowns por-instancia (DJT-077)
    // ------------------------------------------------------------------

    @Test
    void unJutsuSinCooldownNoEstaEnCooldown() {
        assertFalse(JutsuKnowledge.DEFAULT.isOnCooldown("chidori", 1000L));
        assertEquals(0L, JutsuKnowledge.DEFAULT.remainingCooldown("chidori", 1000L));
    }

    @Test
    void withCooldownPoneYExpiraAlTickAbsoluto() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withCooldown("chidori", 1000L, 400);
        assertTrue(k.isOnCooldown("chidori", 1000L));  // justo al castear
        assertTrue(k.isOnCooldown("chidori", 1399L));  // 1 tick antes de expirar
        assertFalse(k.isOnCooldown("chidori", 1400L)); // disponible en el tick de vencimiento
        assertEquals(400L, k.remainingCooldown("chidori", 1000L));
        assertEquals(1L, k.remainingCooldown("chidori", 1399L));
        assertEquals(0L, k.remainingCooldown("chidori", 1400L));
    }

    @Test
    void cooldownCeroNoRegistraNada() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withCooldown("earth_wall", 1000L, 0);
        assertFalse(k.isOnCooldown("earth_wall", 1000L));
        assertTrue(k.cooldowns().isEmpty());
    }

    @Test
    void withCooldownPodaLosVencidosSinBarridoPorTick() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT
                .withCooldown("fire_bullet", 1000L, 100)  // vence en 1100
                .withCooldown("chidori", 2000L, 400);     // now=2000: fire_bullet ya vencio -> podado
        assertFalse(k.cooldowns().containsKey("fire_bullet"));
        assertTrue(k.cooldowns().containsKey("chidori"));
        assertEquals(1, k.cooldowns().size());
    }

    @Test
    void roundTripNbtConservaLosCooldowns() {
        JutsuKnowledge original = JutsuKnowledge.DEFAULT
                .withKnown("chidori")
                .withCooldown("chidori", 500L, 400); // vence en 900
        Tag nbt = JutsuKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        JutsuKnowledge decoded = JutsuKnowledge.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        assertTrue(decoded.isOnCooldown("chidori", 899L));
        assertFalse(decoded.isOnCooldown("chidori", 900L));
    }

    @Test
    void roundTripNbtConservaTodo() {
        JutsuKnowledge original = JutsuKnowledge.DEFAULT
                .withKnown("fire_bullet")
                .withKnown("chidori")
                .withProgress("fire", 1)
                .withProgress("lightning", 5);
        Tag nbt = JutsuKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        JutsuKnowledge decoded = JutsuKnowledge.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow();
        assertEquals(original, decoded);
    }

    @Test
    void elCodecToleraElCompoundVacio() {
        Tag empty = JutsuKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, JutsuKnowledge.DEFAULT).getOrThrow();
        assertEquals(JutsuKnowledge.DEFAULT, JutsuKnowledge.CODEC.parse(NbtOps.INSTANCE, empty).getOrThrow());
    }

    // ------------------------------------------------------------------
    // Jutsu seleccionado (F11/DJT-231)
    // ------------------------------------------------------------------

    @Test
    void unNinjaNuevoNoTieneNadaSeleccionado() {
        assertTrue(JutsuKnowledge.DEFAULT.selectedJutsu().isEmpty());
    }

    @Test
    void seSeleccionaUnJutsuAprendido() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withKnown("chidori").withSelected("chidori");

        assertEquals(Optional.of("chidori"), k.selectedJutsu());
    }

    @Test
    void noSePuedeSeleccionarUnJutsuQueNoSeConoce() {
        // la invariante: el selector no puede acabar mostrando algo que el servidor jamas dejaria lanzar
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withSelected("chidori");

        assertTrue(k.selectedJutsu().isEmpty());
    }

    @Test
    void olvidarLaSeleccionFunciona() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withKnown("chidori").withSelected("chidori");

        assertTrue(k.withoutSelected().selectedJutsu().isEmpty());
    }

    @Test
    void laSeleccionSobreviveALosDemasCambios() {
        // si un wither la perdiera, al jugador se le desarmaria la tecla de cast en mitad de un combate
        JutsuKnowledge base = JutsuKnowledge.DEFAULT.withKnown("chidori").withSelected("chidori");

        assertEquals(Optional.of("chidori"), base.withKnown("rasengan").selectedJutsu());
        assertEquals(Optional.of("chidori"), base.withProgress("fire", 3).selectedJutsu());
        assertEquals(Optional.of("chidori"), base.withCooldown("chidori", 100L, 40).selectedJutsu());
    }

    @Test
    void laSeleccionSobreviveAlGuardado() {
        JutsuKnowledge k = JutsuKnowledge.DEFAULT.withKnown("chidori").withSelected("chidori");

        JutsuKnowledge recargado = JutsuKnowledge.CODEC
                .parse(net.minecraft.nbt.NbtOps.INSTANCE,
                        JutsuKnowledge.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, k).getOrThrow())
                .getOrThrow();

        assertEquals(Optional.of("chidori"), recargado.selectedJutsu());
    }

    @Test
    void unaSeleccionHuerfanaEnElSaveSeDescarta() {
        // NBT de una partida en la que el jutsu seleccionado ya no esta entre los conocidos
        JutsuKnowledge k = new JutsuKnowledge(Set.of("rasengan"), Map.of(), Map.of(), Optional.of("chidori"), List.of());

        assertTrue(k.selectedJutsu().isEmpty());
    }
}
