package com.zamu.dojutsu.data.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests del estado de los Seis Caminos (F9/DJT-210). Lo que se blinda aqui es la distincion que el datapack hace
 * con <b>dos funciones distintas</b> y que es facil aplanar al portar: <b>prestado no es lo mismo que concedido</b>.
 */
class SixPathsStateTest {

    @Test
    void loConcedidoEsSuyoYNoSeLePuedeQuitar() {
        SixPathsState granted = SixPathsState.DEFAULT.granted();
        assertTrue(granted.chakra());
        assertFalse(granted.isLoaned());
        assertFalse(granted.isLoanedBy("ten_tails"));
    }

    @Test
    void loPrestadoSabeQuienLoPresta() {
        SixPathsState loaned = SixPathsState.DEFAULT.loanedBy("ten_tails");
        assertTrue(loaned.chakra());
        assertTrue(loaned.isLoaned());
        assertTrue(loaned.isLoanedBy("ten_tails"));
        assertFalse(loaned.isLoanedBy("hagoromo"), "otra fuente no puede reclamar un prestamo ajeno");
    }

    @Test
    void unPrestamoSeConsolidaAlConcederlo() {
        // recibir el chakra del Sabio mientras lo tenias del Diez Colas te lo deja en propiedad
        SixPathsState consolidated = SixPathsState.DEFAULT.loanedBy("ten_tails").granted();
        assertTrue(consolidated.chakra());
        assertFalse(consolidated.isLoaned(), "ya es suyo: extraer la bestia no debe quitarselo");
    }

    @Test
    void revocarLoDejaSinChakraPeroConservaElModoSabio() {
        SixPathsState revoked = SixPathsState.DEFAULT.loanedBy("ten_tails").withSageMode().revoked();
        assertFalse(revoked.chakra());
        assertFalse(revoked.isLoaned());
        assertTrue(revoked.sageMode(), "take_sp_chakra no toca d.is_sage");
    }

    @Test
    void sinChakraNoPuedeQuedarUnPrestamoHuerfano() {
        // NBT corrupto: prestado por alguien pero sin chakra -> el constructor lo normaliza
        SixPathsState broken = new SixPathsState(false, Optional.of("ten_tails"), false);
        assertEquals(Optional.empty(), broken.loanSource());
        assertFalse(broken.isLoaned());
    }
}
