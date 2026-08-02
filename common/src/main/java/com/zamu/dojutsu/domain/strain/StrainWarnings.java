package com.zamu.dojutsu.domain.strain;

/**
 * Bandas de <b>aviso</b> de fatiga compartidas por la fatiga ocular {@code [S05]} y la fatiga corporal de las
 * Ocho Puertas {@code [S11]}: {@code 25/50/75/90/99}, cada una avisando <b>una sola vez</b> al entrar
 * (anti-spam {@code d.esw_*} / {@code d.egsw_*}).
 *
 * <p>Extraido al dominio cuando aparecio el <b>segundo</b> consumidor real (F6/DJT-145), no antes: es
 * exactamente el riesgo <b>S11-R1</b> ("el strain de las puertas replica el ocular: unificar la mecanica"). Los
 * cortes y los colores son identicos en los dos sistemas; lo que difiere —que se avisa y que se castiga— sigue
 * viviendo en cada servicio.
 *
 * <p>Ojo: estas bandas <b>no</b> son {@link StrainThreshold}. Aquellos son los cortes de <b>castigo</b>
 * (25/50/75/90/95/98/100) que usa {@link StrainMeter}; estos son los de <b>aviso</b>, que incluyen el 99 y
 * excluyen el 95/98/100.
 */
public final class StrainWarnings {

    /** Sin banda: por debajo de 25, o al 100 (donde el apagado forzado ya avisa por su cuenta). */
    public static final int NONE = 0;

    private StrainWarnings() {
    }

    /**
     * Banda de aviso a la que pertenece un nivel de fatiga, expresada por su <b>suelo</b>: 25/50/75/90/99, o
     * {@link #NONE}. Decision pura, fiel a los cortes {@code strain/stage_1..4} + {@code strain/99} del datapack.
     */
    public static int floorFor(int strain) {
        if (strain == 99) {
            return 99;
        }
        if (strain >= 90 && strain <= 98) {
            return 90;
        }
        if (strain >= 75 && strain <= 89) {
            return 75;
        }
        if (strain >= 50 && strain <= 74) {
            return 50;
        }
        if (strain >= 25 && strain <= 49) {
            return 25;
        }
        return NONE;
    }
}
