package com.zamu.dojutsu.network.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.Skill;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tests de las validaciones del handler de acciones de menu (DJT-042): parseo y anti-spam. */
class MenuActionHandlerTest {

    @Test
    void parseSkillResuelvePorNombreSinDistinguirMayusculas() {
        assertEquals(Skill.TAIJUTSU, MenuActionHandler.parseSkill("taijutsu"));
        assertEquals(Skill.TAIJUTSU, MenuActionHandler.parseSkill("TAIJUTSU"));
        assertEquals(Skill.SENJUTSU, MenuActionHandler.parseSkill("SenJutsu"));
    }

    @Test
    void parseSkillRechazaValoresInvalidos() {
        assertNull(MenuActionHandler.parseSkill("no_existe"));
        assertNull(MenuActionHandler.parseSkill(""));
        assertNull(MenuActionHandler.parseSkill(null));
        assertNull(MenuActionHandler.parseSkill("0")); // no se acepta el indice del cliente
    }

    @Test
    void primeraAccionNoEstaLimitada() {
        UUID player = UUID.randomUUID();
        assertFalse(MenuActionHandler.isRateLimited(player, 1_000L));
    }

    @Test
    void accionDemasiadoSeguidaSeLimita() {
        UUID player = UUID.randomUUID();
        assertFalse(MenuActionHandler.isRateLimited(player, 1_000L));
        assertTrue(MenuActionHandler.isRateLimited(player, 1_000L + MenuActionHandler.MIN_INTERVAL_MS - 1));
    }

    @Test
    void trasElIntervaloSeAceptaDeNuevo() {
        UUID player = UUID.randomUUID();
        assertFalse(MenuActionHandler.isRateLimited(player, 1_000L));
        assertFalse(MenuActionHandler.isRateLimited(player, 1_000L + MenuActionHandler.MIN_INTERVAL_MS));
    }

    @Test
    void elLimiteEsPorJugador() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertFalse(MenuActionHandler.isRateLimited(a, 1_000L));
        assertFalse(MenuActionHandler.isRateLimited(b, 1_000L)); // otro jugador no se ve afectado
    }

    @Test
    void forgetLimpiaElEstadoDelJugador() {
        UUID player = UUID.randomUUID();
        assertFalse(MenuActionHandler.isRateLimited(player, 1_000L));
        MenuActionHandler.forget(player);
        assertFalse(MenuActionHandler.isRateLimited(player, 1_000L)); // sin memoria previa
    }
}
