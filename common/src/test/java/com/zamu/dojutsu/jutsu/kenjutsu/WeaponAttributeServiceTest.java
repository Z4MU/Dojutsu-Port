package com.zamu.dojutsu.jutsu.kenjutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests de la logica pura del bonus de kenjutsu (F4/DJT-082). Las bandas del datapack
 * (400/800/1200/1600/2000 → +1..+5) reescaladas ÷4 a la escala del mod (0..500). El glue de
 * {@code ItemAttributeModifiers} se valida en runtime (necesita bootstrap de MC).
 */
class WeaponAttributeServiceTest {

    @Test
    void sinNivelNoHayBonus() {
        assertEquals(0, WeaponAttributeService.proficiencyBonus(0));
        assertEquals(0, WeaponAttributeService.proficiencyBonus(99)); // justo antes del primer tier
    }

    @Test
    void lasBandasReescaladasCoincidenConLosTiersDelDatapack() {
        assertEquals(1, WeaponAttributeService.proficiencyBonus(100)); // datapack 400
        assertEquals(1, WeaponAttributeService.proficiencyBonus(199));
        assertEquals(2, WeaponAttributeService.proficiencyBonus(200)); // datapack 800
        assertEquals(3, WeaponAttributeService.proficiencyBonus(300)); // datapack 1200
        assertEquals(4, WeaponAttributeService.proficiencyBonus(400)); // datapack 1600
        assertEquals(4, WeaponAttributeService.proficiencyBonus(499));
        assertEquals(5, WeaponAttributeService.proficiencyBonus(500)); // datapack 2000+, cap del mod
    }

    @Test
    void elBonusNoSuperaElMaximoEnElCap() {
        // El nivel esta capado a 500 (XpCurve.MAX_LEVEL); por robustez, valores mayores dan +5.
        assertEquals(5, WeaponAttributeService.proficiencyBonus(1000));
    }

    @Test
    void laPenalizacionDeVelocidadDesapareceAlCuartoTier() {
        // Datapack: la penalizacion se quita a kenjutsu 1600 -> escala del mod 400.
        assertTrue(WeaponAttributeService.hasSpeedPenalty(0));
        assertTrue(WeaponAttributeService.hasSpeedPenalty(399));
        assertFalse(WeaponAttributeService.hasSpeedPenalty(400));
        assertFalse(WeaponAttributeService.hasSpeedPenalty(500));
    }
}
