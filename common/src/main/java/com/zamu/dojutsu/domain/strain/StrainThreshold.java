package com.zamu.dojutsu.domain.strain;

/**
 * Umbrales del medidor de fatiga ({@code strain}) del datapack, compartidos por eye strain
 * {@code [S05]} y eight-gates strain {@code [S11]}.
 *
 * <p>Valores en porcentaje sobre el rango {@code 0..100}: {@code 25/50/75/90/95/98/100}. El meter usa
 * estos cortes para notificar cruces; el <em>efecto</em> asociado a cada umbral (aviso, weakness a 95,
 * blindness a 98, apagado forzado a 100...) lo decide el consumidor, no el dominio (RM-3).
 */
public enum StrainThreshold {

    P25(25),
    P50(50),
    P75(75),
    P90(90),
    P95(95),
    P98(98),
    P100(100);

    private final int percent;

    StrainThreshold(int percent) {
        this.percent = percent;
    }

    /** Valor (0..100) a partir del cual se considera alcanzado este umbral. */
    public int percent() {
        return percent;
    }
}
