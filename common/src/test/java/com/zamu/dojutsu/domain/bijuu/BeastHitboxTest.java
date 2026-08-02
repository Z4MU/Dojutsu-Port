package com.zamu.dojutsu.domain.bijuu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la caja de colision de las bestias (F10, arreglo de la verificacion en partida).
 *
 * <h2>El fallo que fijan</h2>
 * El renderer dibujaba la bestia a {@code 1.4 + 0.2 * (colas - 1)} pero la caja se quedaba en el
 * {@code sized(2.0, 2.6)} fijo del {@code EntityType}. Un Nueve Colas se veia a escala <b>3,0</b> —casi seis
 * bloques de ancho— con dos bloques de caja pegados a los pies: se le pegaba al cuerpo y <b>no recibia daño</b>.
 *
 * <p>El error no era el numero sino tenerlo <b>en dos sitios</b>. Estos tests fijan que la escala es una sola
 * funcion y que crece de verdad, para que separar las dos vuelva a doler aqui antes que en partida.
 */
class BeastHitboxTest {

    @Test
    void unaBestiaDeUnaColaUsaLaEscalaBase() {
        assertEquals(1.4F, BeastStats.renderScale(TailedBeast.ONE_TAIL), 1e-4);
    }

    @Test
    void laEscalaCreceUnPasoPorCola() {
        assertEquals(1.6F, BeastStats.renderScale(TailedBeast.TWO_TAILS), 1e-4);
        assertEquals(3.0F, BeastStats.renderScale(TailedBeast.NINE_TAILS), 1e-4);
        assertEquals(3.2F, BeastStats.renderScale(TailedBeast.TEN_TAILS), 1e-4);
    }

    @Test
    void laCajaCreceConLaBestia() {
        // Es la comprobacion que faltaba: si alguien vuelve a fijar la caja, esto se pone rojo.
        assertTrue(BeastStats.width(TailedBeast.NINE_TAILS) > BeastStats.width(TailedBeast.ONE_TAIL));
        assertTrue(BeastStats.height(TailedBeast.NINE_TAILS) > BeastStats.height(TailedBeast.ONE_TAIL));
    }

    @Test
    void elNueveColasEsMuchoMasAnchoQueLaCajaFijaQueTenia() {
        // La caja vieja media 2.0 de ancho. La real del Nueve Colas: 1.95 x 3.0 = 5.85.
        float ancho = BeastStats.width(TailedBeast.NINE_TAILS);
        assertEquals(5.85F, ancho, 1e-3);
        assertTrue(ancho > 2.0F * 2.9F, "casi el triple de la caja fija que hacia que pareciera inmune");
    }

    @Test
    void todasLasBestiasTienenCajaPositivaYCreciente() {
        float anteriorAncho = 0F;
        for (TailedBeast beast : TailedBeast.values()) {
            float ancho = BeastStats.width(beast);
            float alto = BeastStats.height(beast);
            assertTrue(ancho > 0F && alto > 0F, beast.id());
            assertTrue(ancho > anteriorAncho, "cada bestia es mas ancha que la anterior: " + beast.id());
            anteriorAncho = ancho;
        }
    }
}
