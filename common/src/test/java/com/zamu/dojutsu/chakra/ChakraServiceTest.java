package com.zamu.dojutsu.chakra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.ObservableField;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ChakraState;
import org.junit.jupiter.api.Test;

/** Tests de la unica puerta de escritura del chakra: clamp, consumo todo-o-nada y marcado observable (DJT-030). */
class ChakraServiceTest {

    /** Crea un jugador con un chakra concreto (por la puerta de escritura). */
    private static PlayerNinjaData withChakra(int current, int max) {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setChakra(new ChakraState(current, max, 0, 100, 0, 0));
        data.dirtyTracker().clear(); // ignora la marca de la preparacion
        return data;
    }

    @Test
    void hasComparaContraElActual() {
        PlayerNinjaData data = withChakra(30, 100);
        assertTrue(ChakraService.has(data, 30));
        assertFalse(ChakraService.has(data, 31));
    }

    @Test
    void consumeTodoONada() {
        PlayerNinjaData data = withChakra(30, 100);
        assertFalse(ChakraService.consume(data, 31)); // insuficiente: no cambia nada
        assertEquals(30, data.chakra().chakra());
        assertTrue(ChakraService.consume(data, 30));
        assertEquals(0, data.chakra().chakra());
    }

    @Test
    void restoreClampaAlMaximo() {
        PlayerNinjaData data = withChakra(90, 100);
        ChakraService.restore(data, 50);
        assertEquals(100, data.chakra().chakra()); // overshoot clampado
    }

    @Test
    void restoreFullDejaAlMaximo() {
        PlayerNinjaData data = withChakra(10, 100);
        ChakraService.restoreFull(data);
        assertEquals(100, data.chakra().chakra());
    }

    @Test
    void nuncaSaleDelRango() {
        PlayerNinjaData data = withChakra(0, 100);
        ChakraService.consume(data, 999); // rechazado
        assertEquals(0, data.chakra().chakra());
        ChakraService.restore(data, 1_000_000);
        assertEquals(100, data.chakra().chakra());
    }

    @Test
    void mutarMarcaChakraObservable() {
        PlayerNinjaData data = withChakra(50, 100);
        assertTrue(data.dirtyTracker().isEmpty());
        ChakraService.consume(data, 10);
        assertTrue(data.dirtyTracker().isDirty(ObservableField.CHAKRA));
    }

    @Test
    void consumoRechazadoNoMarcaObservable() {
        PlayerNinjaData data = withChakra(5, 100);
        ChakraService.consume(data, 10); // insuficiente
        assertTrue(data.dirtyTracker().isEmpty()); // no hubo escritura
    }

    @Test
    void setFijaValorAbsolutoConClamp() {
        PlayerNinjaData data = withChakra(100, 100);
        ChakraService.set(data, 40);
        assertEquals(40, data.chakra().chakra());
        ChakraService.set(data, 999);
        assertEquals(100, data.chakra().chakra()); // clamp al maximo
        ChakraService.set(data, -5);
        assertEquals(0, data.chakra().chakra()); // clamp a 0
    }

    @Test
    void penalizacionDeMuerteEsLaMitadDelMaximo() {
        // main/death: chakra = max/2 (mitad del MAX, no del actual).
        PlayerNinjaData data = withChakra(90, 100);
        ChakraService.set(data, data.chakra().maxChakra() / 2);
        assertEquals(50, data.chakra().chakra());
        // Aun con el actual por debajo de la mitad, queda en la mitad del max (puede subir).
        PlayerNinjaData bajo = withChakra(10, 100);
        ChakraService.set(bajo, bajo.chakra().maxChakra() / 2);
        assertEquals(50, bajo.chakra().chakra());
    }

    @Test
    void clampSigueElMaximoActual() {
        // Caso borde de la ficha: si el max fuese menor, el actual no lo supera.
        PlayerNinjaData data = withChakra(100, 100);
        data.setChakra(new ChakraState(data.chakra().chakra(), 40, 0, 100, 0, 0));
        assertEquals(40, data.chakra().chakra()); // reclampado por el invariante del state
        ChakraService.restore(data, 100);
        assertEquals(40, data.chakra().chakra());
    }

    @Test
    void setMaxConRellenoFijaYLlena() {
        // DJT-057: el join del clan hace 'd.max_chakra = N; d.chakra = d.max_chakra'.
        PlayerNinjaData data = withChakra(30, 100);
        ChakraService.setMax(data, 500, true);
        assertEquals(500, data.chakra().maxChakra());
        assertEquals(500, data.chakra().chakra());
    }

    @Test
    void setMaxSinRellenoConservaElChakraYClampaSiBaja() {
        // Sin relleno (lo usara F7: la extraccion del bijuu resta max SIN rellenar).
        PlayerNinjaData data = withChakra(80, 100);
        ChakraService.setMax(data, 200, false);
        assertEquals(200, data.chakra().maxChakra());
        assertEquals(80, data.chakra().chakra()); // se conserva

        ChakraService.setMax(data, 50, false); // caso Lee: el max baja por debajo del actual
        assertEquals(50, data.chakra().maxChakra());
        assertEquals(50, data.chakra().chakra()); // clampado por el invariante
    }
}
