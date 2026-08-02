package com.zamu.dojutsu.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.GameType;
import org.junit.jupiter.api.Test;

/** Tests del guard de la penalizacion de muerte (DJT-024). */
class DeathHandlerTest {

    @Test
    void penalizaEnSurvivalYAdventure() {
        assertTrue(DeathHandler.appliesPenalty(GameType.SURVIVAL));
        assertTrue(DeathHandler.appliesPenalty(GameType.ADVENTURE));
    }

    @Test
    void noPenalizaEnCreativoNiEspectador() {
        assertFalse(DeathHandler.appliesPenalty(GameType.CREATIVE));
        assertFalse(DeathHandler.appliesPenalty(GameType.SPECTATOR));
    }
}
