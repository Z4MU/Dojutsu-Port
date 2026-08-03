package com.zamu.dojutsu.dojutsu.ability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * La tabla de sonido por habilidad ocular (F10/DJT-227).
 *
 * <p>Lo que se protege aqui no es que suene —eso necesita mundo— sino <b>quien no suena</b>: la tentacion
 * permanente es convertir esto en una regla por familia, y estos tests fallarian en cuanto alguien lo hiciera.
 */
class EyeAbilitySfxTest {

    @Test
    void lasDelMangekyoSuenan() {
        for (String id : new String[] {"tsukuyomi", "tengai_shinsei", "hinokagutsuchi", "kotoamatsukami",
                "beams_of_light", "chakra_blades", "phase_shift", "flame_control"}) {
            assertTrue(EyeAbilitySfx.hasSound(id), id);
        }
    }

    @Test
    void amaterasuEsMudoAunqueSeaDelMangekyo() {
        // La carpeta de Itachi no tiene un solo playsound. Si esto falla, alguien convirtio la tabla en una
        // regla por familia y le dio voz a una tecnica que el datapack dejo callada.
        assertFalse(EyeAbilitySfx.hasSound("amaterasu"));
    }

    @Test
    void lasDelByakuganSonMudas() {
        // dojutsu.byakugan.activate sale solo en awaken y evolve, nunca al usar una tecnica.
        assertFalse(EyeAbilitySfx.hasSound("vacuum_palm"));
        assertFalse(EyeAbilitySfx.hasSound("sixty_four_palms"));
        assertFalse(EyeAbilitySfx.hasSound("hundred_twenty_eight_palms"));
    }

    @Test
    void lasDelTenseiganSonMudas() {
        assertFalse(EyeAbilitySfx.hasSound("truth_seeking_orbs"));
        assertFalse(EyeAbilitySfx.hasSound("kinrin_tensei_baku"));
    }

    @Test
    void kamuiNoEstaEnLaTablaPorqueUsaUnSonidoVanilla() {
        // Obito toca entity.enderman.teleport, no el del Mangekyo. Vive en KamuiAbility.
        assertFalse(EyeAbilitySfx.hasSound("kamui"));
        assertFalse(EyeAbilitySfx.hasSound("kamui_others"));
    }

    @Test
    void losModosNoEstanEnLaTablaPorqueSuenanAlAlternar() {
        // El Susanoo y el Chakra Mode no suenan al castear sino al encenderse y apagarse: su sonido vive en
        // su servicio, que es quien sabe si acaba de encenderse o de apagarse.
        assertFalse(EyeAbilitySfx.hasSound("susanoo"));
        assertFalse(EyeAbilitySfx.hasSound("tenseigan_chakra_mode"));
    }

    @Test
    void lasQueTienenSonidoPropioEstanDeclaradas() {
        assertTrue(EyeAbilitySfx.hasSound("bansho_tenin"));
        assertTrue(EyeAbilitySfx.hasSound("amenotejikara"));
        assertTrue(EyeAbilitySfx.hasSound("genjutsu_stun"));
    }

    @Test
    void lasDelKetsuryuganYLasDelRinnegonSuenan() {
        for (String id : new String[] {"blood_eruption", "iron_blood_fist", "rising_blood_spear",
                "exploding_human", "shinra_tensei", "asura_path", "preta_path", "chibaku_tensei"}) {
            assertTrue(EyeAbilitySfx.hasSound(id), id);
        }
    }
}
