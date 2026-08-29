package com.zamu.dojutsu.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * Tests de la lectura del archivo de colocacion del HUD (F11/DJT-233). Es un fichero que la gente edita a
 * mano, asi que lo que se comprueba aqui es sobre todo que <b>nada de lo que escriban rompa la carga</b>.
 */
class HudLayoutTest {

    private static Properties props(String... keyValues) {
        Properties p = new Properties();
        for (int i = 0; i < keyValues.length; i += 2) {
            p.setProperty(keyValues[i], keyValues[i + 1]);
        }
        return p;
    }

    @Test
    void unArchivoVacioDejaTodoComoDeFabrica() {
        Map<HudElement, HudPlacement> read = HudLayout.fromProperties(new Properties());

        for (HudElement element : HudElement.values()) {
            assertEquals(element.defaultPlacement(), read.get(element), element.key());
        }
    }

    @Test
    void seLeeLoQueElJugadorColoco() {
        Properties p = props(
                "chakra.anchor", "BOTTOM_RIGHT",
                "chakra.x", "-0.05",
                "chakra.y", "-0.1",
                "chakra.visible", "false");

        HudPlacement chakra = HudLayout.fromProperties(p).get(HudElement.CHAKRA);

        assertEquals(HudAnchor.BOTTOM_RIGHT, chakra.anchor());
        assertEquals(-0.05F, chakra.offsetX(), 0.0001F);
        assertEquals(-0.1F, chakra.offsetY(), 0.0001F);
        assertFalse(chakra.visible());
    }

    @Test
    void unValorInvalidoCaeAlDefaultSinRomperElResto() {
        // "izquierda" no es un anclaje y "mucho" no es un numero: el elemento no debe perderse por ello
        Properties p = props(
                "chakra.anchor", "izquierda",
                "chakra.x", "mucho",
                "chakra.visible", "quiza",
                "rank.anchor", "TOP_RIGHT");

        Map<HudElement, HudPlacement> read = HudLayout.fromProperties(p);

        assertEquals(HudElement.CHAKRA.defaultPlacement(), read.get(HudElement.CHAKRA));
        assertEquals(HudAnchor.TOP_RIGHT, read.get(HudElement.RANK).anchor());
    }

    @Test
    void seAceptanUnoYCeroComoBooleanos() {
        Map<HudElement, HudPlacement> read = HudLayout.fromProperties(
                props("chakra.visible", "0", "rank.visible", "1"));

        assertFalse(read.get(HudElement.CHAKRA).visible());
        assertTrue(read.get(HudElement.RANK).visible());
    }

    @Test
    void guardarYVolverALeerDevuelveLoMismo() {
        HudLayout.reset();
        HudPlacement movido = new HudPlacement(HudAnchor.BOTTOM_CENTER, 0.2F, -0.3F, false);
        HudLayout.set(HudElement.ABILITY_BAR, movido);
        try {
            Map<HudElement, HudPlacement> readBack = HudLayout.fromProperties(HudLayout.toProperties());

            assertEquals(movido, readBack.get(HudElement.ABILITY_BAR));
        } finally {
            HudLayout.reset();
        }
    }

    @Test
    void resetDevuelveUnElementoASuSitio() {
        HudLayout.reset();
        HudLayout.set(HudElement.RANK, new HudPlacement(HudAnchor.BOTTOM_LEFT, 0.5F, 0.5F, false));
        try {
            HudLayout.reset(HudElement.RANK);

            assertEquals(HudElement.RANK.defaultPlacement(), HudLayout.get(HudElement.RANK));
        } finally {
            HudLayout.reset();
        }
    }

    @Test
    void todosLosElementosNacenVisibles() {
        // quien no toque el editor no debe perder ninguna parte del HUD que ya tenia
        for (HudElement element : HudElement.values()) {
            assertTrue(element.defaultPlacement().visible(), element.key());
        }
    }

    @Test
    void lasClavesDeLosElementosSonUnicas() {
        // dos elementos con la misma clave se pisarian en el archivo y uno perderia su colocacion
        long distintas = java.util.Arrays.stream(HudElement.values()).map(HudElement::key).distinct().count();

        assertEquals(HudElement.values().length, distintas);
    }
}
