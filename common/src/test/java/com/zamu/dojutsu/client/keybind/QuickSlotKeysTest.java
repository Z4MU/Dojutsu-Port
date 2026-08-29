package com.zamu.dojutsu.client.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zamu.dojutsu.data.state.JutsuKnowledge;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

/**
 * Tests del mapeo tecla -&gt; hueco rapido (F11/DJT-234).
 *
 * <p>Es aritmetica, pero es la clase de aritmetica que se equivoca en silencio: un desfase de uno hace que
 * {@code ALT+1} lance el jutsu del hueco 2, y en partida eso se diagnostica fatal.
 */
class QuickSlotKeysTest {

    @Test
    void laFilaNumericaMapeaALosOchoHuecos() {
        assertEquals(0, KeyBindings.slotOf(GLFW.GLFW_KEY_1), "la tecla 1 es el hueco 0");
        assertEquals(7, KeyBindings.slotOf(GLFW.GLFW_KEY_8), "la tecla 8 es el ultimo hueco");
    }

    @Test
    void elTecladoNumericoMapeaIgual() {
        assertEquals(0, KeyBindings.slotOf(GLFW.GLFW_KEY_KP_1));
        assertEquals(7, KeyBindings.slotOf(GLFW.GLFW_KEY_KP_8));
    }

    @Test
    void laTeclaNueveNoEsUnHueco() {
        // la hotbar tiene 9 slots y la rueda 8: ALT+9 debe seguir siendo la hotbar de siempre
        assertEquals(-1, KeyBindings.slotOf(GLFW.GLFW_KEY_9));
        assertEquals(-1, KeyBindings.slotOf(GLFW.GLFW_KEY_KP_9));
    }

    @Test
    void elCeroYOtrasTeclasNoSonHuecos() {
        assertEquals(-1, KeyBindings.slotOf(GLFW.GLFW_KEY_0));
        assertEquals(-1, KeyBindings.slotOf(GLFW.GLFW_KEY_W));
        assertEquals(-1, KeyBindings.slotOf(GLFW.GLFW_KEY_ESCAPE));
    }

    @Test
    void elRangoCubiertoCoincideConElNumeroDeHuecos() {
        // si algun dia SLOT_COUNT cambia, este test avisa de que el mapeo de teclas se quedo atras
        int mapeadas = 0;
        for (int key = GLFW.GLFW_KEY_0; key <= GLFW.GLFW_KEY_9; key++) {
            if (KeyBindings.slotOf(key) >= 0) {
                mapeadas++;
            }
        }
        assertEquals(JutsuKnowledge.SLOT_COUNT, mapeadas);
    }
}
