package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.dojutsu.EyeStrainService.StrainEffects;
import org.junit.jupiter.api.Test;

/**
 * Tests de la fatiga ocular (DJT-116): regen por pulso (1, o 3 con ADN de Hashirama), clasificación de efectos
 * por umbral (95 weakness / 98 blindness / 100 apagado) con el Rinne Sharingan exento de ceguera/apagado, y las
 * puertas `addStrain`/`resetStrain`. Sin Minecraft (la aplicación de MobEffects es glue, no se testea aquí).
 */
class EyeStrainServiceTest {

    // --- regen (puro) ---

    @Test
    void sinFatigaNoRegenera() {
        EyeState s = EyeState.DEFAULT; // strain 0
        assertSame(s, EyeStrainService.regen(s, false));
    }

    @Test
    void mientrasElTimerCuentaSoloDecrementa() {
        EyeState s = EyeState.DEFAULT.withEyeStrain(50).withTimer(EyeStrainService.REGEN_TIMER_KEY, 5);
        EyeState r = EyeStrainService.regen(s, false);
        assertEquals(50, r.eyeStrain()); // la fatiga no baja hasta el pulso
        assertEquals(4, r.timer(EyeStrainService.REGEN_TIMER_KEY));
    }

    @Test
    void alPulsoBajaUnoYReiniciaElTimer() {
        EyeState s = EyeState.DEFAULT.withEyeStrain(50); // timer 0 (default)
        EyeState r = EyeStrainService.regen(s, false);
        assertEquals(49, r.eyeStrain());
        assertEquals(200, r.timer(EyeStrainService.REGEN_TIMER_KEY));
    }

    @Test
    void conAdnDeHashiramaBajaTres() {
        EyeState s = EyeState.DEFAULT.withEyeStrain(50);
        assertEquals(47, EyeStrainService.regen(s, true).eyeStrain());
    }

    @Test
    void laFatigaNoBajaDeCero() {
        EyeState s = EyeState.DEFAULT.withEyeStrain(1);
        assertEquals(0, EyeStrainService.regen(s, false).eyeStrain());
    }

    // --- effectsFor (puro) ---

    @Test
    void bajoNoventaYCincoSinEfectos() {
        StrainEffects e = EyeStrainService.effectsFor(94, false);
        assertFalse(e.weakness());
        assertFalse(e.blindness());
        assertFalse(e.forceDeactivate());
    }

    @Test
    void aNoventaYCincoSoloWeakness() {
        StrainEffects e = EyeStrainService.effectsFor(95, false);
        assertTrue(e.weakness());
        assertFalse(e.blindness());
        assertFalse(e.forceDeactivate());
    }

    @Test
    void aNoventaYOchoWeaknessYBlindness() {
        StrainEffects e = EyeStrainService.effectsFor(98, false);
        assertTrue(e.weakness());
        assertTrue(e.blindness());
        assertFalse(e.forceDeactivate());
    }

    @Test
    void aCienApagaYCiega() {
        StrainEffects e = EyeStrainService.effectsFor(100, false);
        assertTrue(e.weakness());
        assertTrue(e.blindness());
        assertTrue(e.forceDeactivate());
    }

    @Test
    void elRinneSharinganSeEximeDeCegueraYApagadoPeroNoDeWeakness() {
        StrainEffects e = EyeStrainService.effectsFor(100, true);
        assertTrue(e.weakness());        // weakness sí (fiel a eye_strain/main, sin exención)
        assertFalse(e.blindness());      // exento
        assertFalse(e.forceDeactivate()); // exento (unless wearing_eye_10)
    }

    // --- addStrain / resetStrain (sobre PlayerNinjaData) ---

    @Test
    void addStrainSubeLaFatiga() {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setEye(EyeState.DEFAULT.withEyeStrain(40));
        EyeStrainService.addStrain(data, 15);
        assertEquals(55, data.eye().eyeStrain());
    }

    @Test
    void addStrainIgnoraCantidadesNoPositivas() {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setEye(EyeState.DEFAULT.withEyeStrain(40));
        EyeStrainService.addStrain(data, 0);
        EyeStrainService.addStrain(data, -5);
        assertEquals(40, data.eye().eyeStrain());
    }

    @Test
    void resetStrainDejaLaFatigaEnCero() {
        PlayerNinjaData data = new PlayerNinjaData();
        data.setEye(EyeState.DEFAULT.withEyeStrain(80));
        EyeStrainService.resetStrain(data);
        assertEquals(0, data.eye().eyeStrain());
    }

    // --- avisos de chat por banda (fleco F5) ---

    @Test
    void laBandaDeAvisoSigueLosCortesDelDatapack() {
        // stage_1 25-49, stage_2 50-74, stage_3 75-89, stage_4 90-98, 99 exacto
        assertEquals(0, EyeStrainService.strainWarningFloor(24)); // por debajo: sin aviso
        assertEquals(25, EyeStrainService.strainWarningFloor(25));
        assertEquals(25, EyeStrainService.strainWarningFloor(49));
        assertEquals(50, EyeStrainService.strainWarningFloor(50));
        assertEquals(50, EyeStrainService.strainWarningFloor(74));
        assertEquals(75, EyeStrainService.strainWarningFloor(75));
        assertEquals(75, EyeStrainService.strainWarningFloor(89));
        assertEquals(90, EyeStrainService.strainWarningFloor(90));
        assertEquals(90, EyeStrainService.strainWarningFloor(98));
        assertEquals(99, EyeStrainService.strainWarningFloor(99));
    }

    @Test
    void alCienNoHayBandaDeAvisoElApagadoForzadoAvisaAparte() {
        // a 100 el force_deactivate muestra su propio mensaje; no debe solaparse con un aviso de banda
        assertEquals(0, EyeStrainService.strainWarningFloor(100));
    }
}
