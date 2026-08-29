package com.zamu.dojutsu.jutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.ObservableField;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.jutsu.JutsuSelectionService.SelectOutcome;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests del selector de jutsu (F11/DJT-231). Lo que se protege aqui es la puerta de servidor: seleccionar es
 * una <b>peticion</b> del cliente y solo prende si el jutsu esta aprendido.
 */
class JutsuSelectionServiceTest {

    private static PlayerNinjaData conJutsus(String... ids) {
        PlayerNinjaData data = new PlayerNinjaData();
        for (String id : ids) {
            data.setJutsu(data.jutsu().withKnown(id));
        }
        data.dirtyTracker().clear();
        return data;
    }

    @Test
    void seleccionarUnJutsuAprendidoLoGuarda() {
        PlayerNinjaData data = conJutsus("chidori");

        assertEquals(SelectOutcome.SELECTED, JutsuSelectionService.select(data, "chidori"));
        assertEquals(Optional.of("chidori"), data.jutsu().selectedJutsu());
    }

    @Test
    void seleccionarLoQueNoSeConoceSeRechazaSinEfecto() {
        PlayerNinjaData data = conJutsus("chidori");

        assertEquals(SelectOutcome.NOT_KNOWN, JutsuSelectionService.select(data, "rasengan"));
        assertTrue(data.jutsu().selectedJutsu().isEmpty(), "un payload ajeno no debe tocar el estado");
    }

    @Test
    void unJutsuAjenoNoDesplazaAlYaSeleccionado() {
        // el caso feo: si la peticion invalida borrara la seleccion previa, un cliente hostil podria
        // desarmar a otro jugador mandando basura
        PlayerNinjaData data = conJutsus("chidori");
        JutsuSelectionService.select(data, "chidori");

        assertEquals(SelectOutcome.NOT_KNOWN, JutsuSelectionService.select(data, "amaterasu"));
        assertEquals(Optional.of("chidori"), data.jutsu().selectedJutsu());
    }

    @Test
    void repetirLaMismaSeleccionNoEsUnCambio() {
        PlayerNinjaData data = conJutsus("chidori");
        JutsuSelectionService.select(data, "chidori");
        data.dirtyTracker().clear();

        assertEquals(SelectOutcome.UNCHANGED, JutsuSelectionService.select(data, "chidori"));
        // sin cambio no hay delta: la rueda puede soltar la misma seleccion varias veces
        assertTrue(data.dirtyTracker().isEmpty(), "repetir la seleccion no debe generar trafico");
    }

    @Test
    void laSeleccionMarcaSuCampoObservable() {
        PlayerNinjaData data = conJutsus("chidori");

        JutsuSelectionService.select(data, "chidori");

        assertTrue(data.dirtyTracker().isDirty(ObservableField.JUTSU_SELECTED));
    }

    @Test
    void retirarLaSeleccionLaDejaVacia() {
        PlayerNinjaData data = conJutsus("chidori");
        JutsuSelectionService.select(data, "chidori");

        assertEquals(SelectOutcome.CLEARED, JutsuSelectionService.clear(data));
        assertTrue(data.jutsu().selectedJutsu().isEmpty());
    }

    @Test
    void retirarSinSeleccionNoEsUnCambio() {
        PlayerNinjaData data = conJutsus("chidori");

        assertEquals(SelectOutcome.UNCHANGED, JutsuSelectionService.clear(data));
        assertTrue(data.dirtyTracker().isEmpty());
    }

    @Test
    void loQueLanzaLaTeclaEsLaSeleccionDelEstado() {
        PlayerNinjaData data = conJutsus("chidori");

        assertTrue(JutsuSelectionService.selected(data).isEmpty(), "sin elegir no hay nada que lanzar");
        JutsuSelectionService.select(data, "chidori");
        assertEquals(Optional.of("chidori"), JutsuSelectionService.selected(data));
    }
}
