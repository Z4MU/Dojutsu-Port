package com.zamu.dojutsu.domain.strain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests del medidor de fatiga con umbrales 25/50/75/90/95/98/100 (DJT-011). */
class StrainMeterTest {

    /** Registro de cruces (umbral + direccion) para verificar las notificaciones. */
    private record Cross(StrainThreshold threshold, boolean rising) {}

    private static List<Cross> recordCrosses(StrainMeter meter) {
        List<Cross> crosses = new ArrayList<>();
        meter.addListener((t, rising) -> crosses.add(new Cross(t, rising)));
        return crosses;
    }

    @Test
    void inicialEnCeroSinUmbral() {
        StrainMeter m = new StrainMeter();
        assertEquals(0, m.value());
        assertTrue(m.currentThreshold().isEmpty());
    }

    @Test
    void addCruzaUnUmbralYNotificaUnaVez() {
        StrainMeter m = new StrainMeter();
        List<Cross> crosses = recordCrosses(m);
        m.add(30);
        assertEquals(30, m.value());
        assertEquals(List.of(new Cross(StrainThreshold.P25, true)), crosses);
    }

    @Test
    void addSaltaVariosUmbralesEnOrdenAscendente() {
        StrainMeter m = new StrainMeter();
        List<Cross> crosses = recordCrosses(m);
        m.add(96); // cruza 25,50,75,90,95
        assertEquals(List.of(
                new Cross(StrainThreshold.P25, true),
                new Cross(StrainThreshold.P50, true),
                new Cross(StrainThreshold.P75, true),
                new Cross(StrainThreshold.P90, true),
                new Cross(StrainThreshold.P95, true)), crosses);
    }

    @Test
    void quedarseEnLaMismaBandaNoVuelveANotificar() {
        StrainMeter m = new StrainMeter();
        List<Cross> crosses = recordCrosses(m);
        m.add(30); // cruza 25
        m.add(10); // 30 -> 40, sigue en la banda de 25, sin nuevo cruce
        assertEquals(40, m.value());
        assertEquals(1, crosses.size());
    }

    @Test
    void llegarA100DisparaElUmbralMaximoUnaVez() {
        StrainMeter m = new StrainMeter(99);
        List<Cross> crosses = recordCrosses(m);
        m.add(50); // clampa a 100
        assertEquals(100, m.value());
        assertEquals(List.of(new Cross(StrainThreshold.P100, true)), crosses);
        m.add(10); // ya en 100, sin nuevo cruce
        assertEquals(1, crosses.size());
    }

    @Test
    void reduceRearmaEnOrdenDescendente() {
        StrainMeter m = new StrainMeter(100);
        List<Cross> crosses = recordCrosses(m);
        m.reduce(100); // baja a 0, deja atras 100,98,95,90,75,50,25
        assertEquals(0, m.value());
        assertEquals(List.of(
                new Cross(StrainThreshold.P100, false),
                new Cross(StrainThreshold.P98, false),
                new Cross(StrainThreshold.P95, false),
                new Cross(StrainThreshold.P90, false),
                new Cross(StrainThreshold.P75, false),
                new Cross(StrainThreshold.P50, false),
                new Cross(StrainThreshold.P25, false)), crosses);
    }

    @Test
    void oscilarAlrededorDeUnUmbralRearmaYVuelveADisparar() {
        StrainMeter m = new StrainMeter(94);
        List<Cross> crosses = recordCrosses(m);
        m.add(2);    // 94 -> 96 : cruza 95 subiendo
        m.reduce(2); // 96 -> 94 : rearma 95 bajando
        m.add(2);    // 94 -> 96 : vuelve a cruzar 95 subiendo
        assertEquals(List.of(
                new Cross(StrainThreshold.P95, true),
                new Cross(StrainThreshold.P95, false),
                new Cross(StrainThreshold.P95, true)), crosses);
    }

    @Test
    void currentThresholdReflejaLaBandaActual() {
        StrainMeter m = new StrainMeter();
        assertTrue(m.currentThreshold().isEmpty());
        m.add(24);
        assertTrue(m.currentThreshold().isEmpty());
        m.add(1); // 25
        assertEquals(StrainThreshold.P25, m.currentThreshold().orElseThrow());
        m.add(75); // 100
        assertEquals(StrainThreshold.P100, m.currentThreshold().orElseThrow());
    }

    @Test
    void resetPoneACeroSinNotificarYRearma() {
        StrainMeter m = new StrainMeter(100);
        List<Cross> crosses = recordCrosses(m);
        m.reset();
        assertEquals(0, m.value());
        assertTrue(m.currentThreshold().isEmpty());
        assertTrue(crosses.isEmpty(), "reset es un reset de estado, no un cruce");
        m.add(30); // tras el reset, los umbrales estan rearmados
        assertEquals(List.of(new Cross(StrainThreshold.P25, true)), crosses);
    }

    @Test
    void variosListenersRecibenLaNotificacion() {
        StrainMeter m = new StrainMeter();
        List<Cross> a = new ArrayList<>();
        List<Cross> b = new ArrayList<>();
        m.addListener((t, rising) -> a.add(new Cross(t, rising)));
        m.addListener((t, rising) -> b.add(new Cross(t, rising)));
        m.add(30);
        assertEquals(1, a.size());
        assertEquals(1, b.size());
    }

    @Test
    void valoresNegativosSonRechazados() {
        StrainMeter m = new StrainMeter();
        assertThrows(IllegalArgumentException.class, () -> m.add(-1));
        assertThrows(IllegalArgumentException.class, () -> m.reduce(-1));
    }
}
