package com.zamu.dojutsu.domain.strain;

/**
 * Observador de cruces de umbral de un {@link StrainMeter}.
 *
 * <p>Es el punto de extension que mantiene el meter agnostico del castigo concreto (RM-3): cada
 * consumidor (eye strain S05, eight-gates S11, susanoo S24...) aplica aqui sus propios efectos.
 * Las notificaciones son sincronas y server-side.
 */
@FunctionalInterface
public interface StrainListener {

    /**
     * Se invoca <b>una sola vez</b> cada vez que el valor cruza un umbral.
     *
     * @param threshold el umbral cruzado
     * @param rising    {@code true} si se alcanzo subiendo (activar efecto); {@code false} si se dejo
     *                  atras bajando (rearmar / retirar efecto)
     */
    void onThresholdCrossed(StrainThreshold threshold, boolean rising);
}
