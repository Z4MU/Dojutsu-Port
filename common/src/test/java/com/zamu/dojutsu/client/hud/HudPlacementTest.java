package com.zamu.dojutsu.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la colocacion del HUD (F11/DJT-233). Todo lo de aqui es aritmetica pura: donde cae un elemento
 * dado un anclaje, un desplazamiento y un tamano de pantalla.
 *
 * <p>El caso que de verdad importa es {@link #laColocacionSobreviveAUnCambioDeResolucion()}: guardar en
 * pixeles en vez de en fraccion de pantalla es el fallo clasico de los HUD movibles.
 */
class HudPlacementTest {

    private static final int W = 1920;
    private static final int H = 1080;
    private static final int EW = 128; // ancho del elemento
    private static final int EH = 8;   // alto del elemento

    @Test
    void arribaIzquierdaMideDesdeElOrigen() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, 0.0F, 0.0F, true);

        assertEquals(0, p.resolveX(W, EW));
        assertEquals(0, p.resolveY(H, EH));
    }

    @Test
    void abajoDerechaDejaElElementoDentroDeLaPantalla() {
        // sin restar el tamano del elemento, un anclaje a la derecha lo dibujaria fuera del borde
        HudPlacement p = new HudPlacement(HudAnchor.BOTTOM_RIGHT, 0.0F, 0.0F, true);

        assertEquals(W - EW, p.resolveX(W, EW));
        assertEquals(H - EH, p.resolveY(H, EH));
    }

    @Test
    void elCentroSuperiorCentraElElementoNoSuBorde() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_CENTER, 0.0F, 0.0F, true);

        assertEquals(W / 2 - EW / 2, p.resolveX(W, EW));
    }

    @Test
    void laColocacionSobreviveAUnCambioDeResolucion() {
        // el jugador coloca el elemento a un cuarto del ancho en 1920x1080...
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, 0.25F, 0.10F, true);

        // ...y en 1280x720 sigue estando a un cuarto del ancho, no en el mismo pixel
        assertEquals(480, p.resolveX(1920, EW));
        assertEquals(320, p.resolveX(1280, EW));
        assertEquals(108, p.resolveY(1080, EH));
        assertEquals(72, p.resolveY(720, EH));
    }

    @Test
    void nadaPuedeQuedarFueraDeLaPantalla() {
        // red de seguridad para un config editado a mano o una resolucion mucho menor
        HudPlacement fuera = new HudPlacement(HudAnchor.TOP_LEFT, 1.0F, 1.0F, true);

        int x = fuera.resolveX(W, EW);
        int y = fuera.resolveY(H, EH);

        assertTrue(x >= 0 && x <= W - EW, "x fuera de pantalla: " + x);
        assertTrue(y >= 0 && y <= H - EH, "y fuera de pantalla: " + y);
    }

    @Test
    void arrastrarYResolverSonOperacionesInversas() {
        // es lo que hace el editor: el jugador suelta el raton en un pixel y eso debe volver a dibujarse ahi
        HudPlacement base = new HudPlacement(HudAnchor.BOTTOM_RIGHT, 0.0F, 0.0F, true);

        HudPlacement movido = base.movedTo(700, 400, W, H, EW, EH);

        assertEquals(700, movido.resolveX(W, EW));
        assertEquals(400, movido.resolveY(H, EH));
    }

    @Test
    void unDesplazamientoAbsurdoSeRecorta() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, 50.0F, -50.0F, true);

        assertEquals(1.0F, p.offsetX());
        assertEquals(-1.0F, p.offsetY());
    }

    @Test
    void unDesplazamientoNoNumericoNoRompeNada() {
        HudPlacement p = new HudPlacement(HudAnchor.TOP_LEFT, Float.NaN, Float.NaN, true);

        assertEquals(0.0F, p.offsetX());
        assertEquals(0.0F, p.offsetY());
    }

    @Test
    void unAnclajeDesconocidoCaeAlDeReserva() {
        assertEquals(HudAnchor.TOP_LEFT, HudAnchor.byName("no_existe", HudAnchor.TOP_LEFT));
        assertEquals(HudAnchor.TOP_LEFT, HudAnchor.byName(null, HudAnchor.TOP_LEFT));
        assertEquals(HudAnchor.BOTTOM_RIGHT, HudAnchor.byName(" bottom_right ", HudAnchor.TOP_LEFT));
    }
}
