package com.zamu.dojutsu.dojutsu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zamu.dojutsu.data.state.EyeState;
import org.junit.jupiter.api.Test;

/**
 * Tests de la decision pura del encendido/apagado del dojutsu (F11/DJT-230): cuando la fatiga veta encender.
 * El resto del servicio (rate-limit, avisos, sonido) necesita un {@code ServerPlayer} y se verifica en partida.
 */
class EyeToggleServiceTest {

    private static EyeState conFatiga(String eyeId, int strain) {
        return EyeState.DEFAULT.withActiveEye(eyeId).withEyeStrain(strain);
    }

    @Test
    void sinFatigaSePuedeEncender() {
        assertFalse(EyeToggleService.blockedByStrain(conFatiga("sharingan", 0)));
    }

    @Test
    void conFatigaAltaPeroNoMaximaSePuedeEncender() {
        // 99% duele (ceguera y weakness) pero no impide encender: el veto es solo el tope
        assertFalse(EyeToggleService.blockedByStrain(conFatiga("sharingan", 99)));
    }

    @Test
    void alMaximoDeFatigaNoSePuedeEncender() {
        // sin esta guarda, el apagado forzado del 100% se saltaria pulsando la tecla otra vez
        assertTrue(EyeToggleService.blockedByStrain(conFatiga("sharingan", 100)));
    }

    @Test
    void elRinneSharinganNuncaLoVetaLaFatiga() {
        // es el ojo exento del apagado forzado (unless wearing_eye_10); si el veto de encendido no fuera
        // igual de exento, las dos reglas se contradirian
        assertFalse(EyeToggleService.blockedByStrain(conFatiga(EyeStrainService.STRAINLESS_EYE, 100)));
    }

    @Test
    void sinOjoNoHayNadaQueVetar() {
        assertFalse(EyeToggleService.blockedByStrain(EyeState.DEFAULT.withEyeStrain(100)));
    }
}
