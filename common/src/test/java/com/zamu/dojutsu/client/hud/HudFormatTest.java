package com.zamu.dojutsu.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Tests del formato compacto del HUD (DJT-041): valores grandes no rompen el layout. */
class HudFormatTest {

    @Test
    void valoresPequenosSeMuestranTalCual() {
        assertEquals("0", HudFormat.compact(0));
        assertEquals("100", HudFormat.compact(100));
        assertEquals("999", HudFormat.compact(999));
    }

    @Test
    void millaresSeCompactan() {
        assertEquals("1.0k", HudFormat.compact(1_000));
        assertEquals("8.0k", HudFormat.compact(8_000)); // chakra de Jinchuriki (futuro)
        assertEquals("12.5k", HudFormat.compact(12_500));
    }

    @Test
    void millonesSeCompactan() {
        assertEquals("1.0M", HudFormat.compact(1_000_000));
        assertEquals("1.5M", HudFormat.compact(1_500_000));
    }
}
