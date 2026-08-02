package com.zamu.dojutsu.domain.throwing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.ThrowingState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

/**
 * Tests de las arrojadizas y del Vuelo del Dios del Trueno (F8/DJT-180). Verificado contra
 * {@code throwing/throw/*}, {@code throwing/move} y {@code jutsu/flying_thunder_god/*}.
 */
class FtgTableTest {

    @Test
    void elShurikenVuelaMasRapidoQueElKunai() {
        // contraintuitivo pero es lo que dice move.mcfunction: 0.5 los kunai, 0.8 el resto
        assertEquals(ThrowableType.KUNAI_SPEED, ThrowableType.KUNAI.speed());
        assertEquals(ThrowableType.FAST_SPEED, ThrowableType.SHURIKEN.speed());
        assertTrue(ThrowableType.SHURIKEN.speed() > ThrowableType.KUNAI.speed());
    }

    @Test
    void elKunaiSeLanzaElDobleDeSeguido() {
        assertEquals(10, ThrowableType.KUNAI.throwCooldown());
        for (ThrowableType type : ThrowableType.values()) {
            if (type != ThrowableType.KUNAI) {
                assertEquals(20, type.throwCooldown(), type.id() + " deberia esperar 20t");
            }
        }
    }

    @Test
    void elDanoEnManoYElDanoLanzadoSonCosasDistintas() {
        // los atributos difieren...
        assertEquals(5, ThrowableType.KUNAI.meleeDamage());
        assertEquals(3, ThrowableType.SHURIKEN.meleeDamage());
        assertEquals(4, ThrowableType.FUMA_SHURIKEN.meleeDamage());
        // ...pero lanzadas las cinco hacen lo mismo
        assertEquals(0, ThrowingTable.THROWN_DAMAGE_AMPLIFIER);
        assertEquals(6, ThrowingTable.THROWN_DAMAGE);
    }

    @Test
    void soloElKunaiDelDiosDelTruenoEsMarcaYNoSeRecupera() {
        assertTrue(ThrowableType.FTG_KUNAI.isMark());
        assertFalse(ThrowableType.FTG_KUNAI.recoverable());
        assertEquals(0, ThrowableType.FTG_KUNAI.meleeDamage()); // no lleva atributo de daño
        for (ThrowableType type : ThrowableType.values()) {
            if (type != ThrowableType.FTG_KUNAI) {
                assertFalse(type.isMark(), type.id() + " no deberia ser marca");
            }
        }
    }

    @Test
    void loQueDetonaNoSeRecupera() {
        assertTrue(ThrowableType.EXPLOSIVE_KUNAI.explodes());
        assertFalse(ThrowableType.EXPLOSIVE_KUNAI.recoverable());
        assertTrue(ThrowableType.KUNAI.recoverable());
        assertTrue(ThrowableType.SHURIKEN.recoverable());
        assertTrue(ThrowableType.FUMA_SHURIKEN.recoverable());
    }

    @Test
    void dominarElSaltoLoAbarataCincoVeces() {
        assertEquals(FtgTable.CHAKRA_NOVICE, FtgTable.chakraCost(0));
        assertEquals(FtgTable.CHAKRA_NOVICE, FtgTable.chakraCost(FtgTable.NOVICE_MAX_LEVEL));
        assertEquals(FtgTable.CHAKRA_ADEPT, FtgTable.chakraCost(FtgTable.NOVICE_MAX_LEVEL + 1));
        assertEquals(FtgTable.CHAKRA_ADEPT, FtgTable.chakraCost(FtgTable.ADEPT_MAX_LEVEL));
        assertEquals(FtgTable.CHAKRA_MASTER, FtgTable.chakraCost(FtgTable.ADEPT_MAX_LEVEL + 1));
        assertEquals(FtgTable.CHAKRA_MASTER, FtgTable.chakraCost(FtgTable.MAX_LEVEL));
        assertEquals(5, FtgTable.CHAKRA_NOVICE / FtgTable.CHAKRA_MASTER);
    }

    @Test
    void elClanDeMinatoAprendeAlDoble() {
        assertEquals(FtgTable.XP_PER_USE_NAMIKAZE, FtgTable.xpPerUse(FtgTable.NAMIKAZE_CLAN));
        assertEquals(FtgTable.XP_PER_USE, FtgTable.xpPerUse("uchiha"));
        assertEquals(FtgTable.XP_PER_USE, FtgTable.xpPerUse(null));
        assertEquals(2 * FtgTable.XP_PER_USE, FtgTable.XP_PER_USE_NAMIKAZE);
    }

    @Test
    void elNivelDejaDeSubirEnElTope() {
        assertTrue(FtgTable.levelsUp(FtgTable.MAX_LEVEL - 1, FtgTable.XP_PER_LEVEL));
        assertFalse(FtgTable.levelsUp(FtgTable.MAX_LEVEL, 999));
        assertFalse(FtgTable.levelsUp(0, FtgTable.XP_PER_LEVEL - 1));
    }

    @Test
    void hayUnaSolaMarcaYSaltarLaConsume() {
        GlobalPos first = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10));
        GlobalPos second = GlobalPos.of(Level.NETHER, new BlockPos(-5, 40, 7));

        ThrowingState state = ThrowingState.DEFAULT.withMark(first);
        assertTrue(state.hasMark());
        assertEquals(java.util.Optional.of(first), state.ftgMark());

        // marcar otra vez REEMPLAZA (reset mata el kunai anterior), incluso en otra dimension
        ThrowingState replaced = state.withMark(second);
        assertEquals(java.util.Optional.of(second), replaced.ftgMark());

        // y saltar la consume
        assertFalse(replaced.withoutMark().hasMark());
    }

    @Test
    void elNivelDeLaTecnicaSeClampaAlTope() {
        assertEquals(FtgTable.MAX_LEVEL, new ThrowingState(java.util.Optional.empty(), 999, 0).ftgLevel());
        assertEquals(0, new ThrowingState(java.util.Optional.empty(), -5, -5).ftgLevel());
    }

    @Test
    void subirDeNivelDescuentaLaXpDelNivel() {
        ThrowingState state = ThrowingState.DEFAULT.withFtgXp(FtgTable.XP_PER_LEVEL + 3).withFtgLevelUp();
        assertEquals(1, state.ftgLevel());
        assertEquals(3, state.ftgXp()); // el arrastre se conserva
    }
}
