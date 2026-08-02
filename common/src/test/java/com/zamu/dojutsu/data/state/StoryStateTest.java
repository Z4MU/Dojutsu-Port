package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.domain.story.StoryTable;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests del sub-estado narrativo (F9/DJT-200). Lo que se blinda aqui son los dos malentendidos faciles de este
 * sistema: que la etapa la sube <b>el encuentro</b> y no la eleccion, y que las dos mitades son
 * <b>acumulables</b>, no una eleccion excluyente.
 */
class StoryStateTest {

    @Test
    void laEtapaLaSubeElEncuentroNoLaEleccion() {
        StoryState met = StoryState.DEFAULT.withEncounter("hagoromo");
        assertEquals(StoryTable.STAGE_MET, met.stage());
        assertEquals(Optional.of("hagoromo"), met.sage());

        // elegir camino NO toca la etapa (yin_path no escribe d.hagoromo)
        assertEquals(StoryTable.STAGE_MET, met.withYin().stage());
        assertEquals(StoryTable.STAGE_MET, met.withYang().stage());
    }

    @Test
    void aLaSegundaAparicionSeAcaba() {
        StoryState second = StoryState.DEFAULT.withEncounter("hamura").withEncounter("hamura");
        assertEquals(StoryTable.STAGE_DONE, second.stage());
        assertTrue(second.isDone());
        // y no puede pasarse de ahi (hagoromo_2 veta el try para siempre)
        assertEquals(StoryTable.STAGE_DONE, second.withEncounter("hamura").stage());
    }

    @Test
    void unEncuentroDejaElContadorComoLoDejaElDatapack() {
        StoryState state = new StoryState(Optional.empty(), 0, 2, false, false);
        assertEquals(StoryTable.attemptsAfterEncounter(), state.withEncounter("hagoromo").failedAttempts());
    }

    @Test
    void lasDosMitadesSonAcumulables() {
        StoryState both = StoryState.DEFAULT.withEncounter("hagoromo").withYin().withYang();
        assertTrue(both.yin());
        assertTrue(both.yang());
        assertTrue(both.hasBothHalves());
    }

    @Test
    void rechazarDevuelveAlPrincipioSinQuitarLoConcedido() {
        StoryState refused = StoryState.DEFAULT.withEncounter("hagoromo").withYin().withRefusal();
        assertEquals(0, refused.stage());
        assertFalse(refused.isDone());
        assertTrue(refused.yin(), "rechazar no revoca una mitad ya recibida");
    }

    @Test
    void elEstadoCorruptoNoRompeLaCarga() {
        StoryState clamped = new StoryState(Optional.empty(), 99, -5, false, false);
        assertEquals(StoryTable.STAGE_DONE, clamped.stage());
        assertEquals(0, clamped.failedAttempts());
    }
}
