package com.zamu.dojutsu.domain.strain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Medidor de fatiga ({@code strain}) {@code 0..100} con umbrales escalonados {@code [S05]/[S11]}.
 *
 * <p>Reproduce el patron de "strain" del datapack (patron 3 del ADD): la fatiga sube al usar poderes y
 * baja al regenerar/dormir; al cruzar cada umbral ({@link StrainThreshold}) se dispara una notificacion
 * <b>una sola vez</b> (equivale a las tags anti-spam {@code d.esw_*}/{@code d.egsw_*} del datapack) y se
 * <b>rearma</b> al caer por debajo. El meter es agnostico del castigo concreto: solo informa del cruce
 * via {@link StrainListener}; el efecto (aviso, weakness a 95, blindness a 98, apagado a 100...) lo pone
 * el consumidor (RM-3).
 *
 * <p>Dominio puro: sin imports de Minecraft; uso previsto server-side y sincrono. {@code [S05]}/{@code [S11]}
 * no definen banda de histeresis, por lo que el rearme ocurre en el mismo umbral (subir a &ge;T notifica;
 * bajar a &lt;T rearma).
 */
public final class StrainMeter {

    /** Valor minimo del medidor. */
    public static final int MIN = 0;

    /** Valor maximo del medidor (100% = fatiga total). */
    public static final int MAX = 100;

    private int value;
    private final List<StrainListener> listeners = new ArrayList<>();

    /** Crea un medidor a 0 (sin fatiga). */
    public StrainMeter() {
        this(MIN);
    }

    /** Crea un medidor con un valor inicial, clampado a {@code [0, 100]}. */
    public StrainMeter(int initial) {
        this.value = clamp(initial);
    }

    /** Registra un observador de cruces de umbral. */
    public void addListener(StrainListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Valor actual de fatiga (0..100). */
    public int value() {
        return value;
    }

    /**
     * Umbral mas alto alcanzado por el valor actual, o vacio si esta por debajo de {@link StrainThreshold#P25}.
     */
    public Optional<StrainThreshold> currentThreshold() {
        StrainThreshold reached = null;
        for (StrainThreshold t : StrainThreshold.values()) {
            if (value >= t.percent()) {
                reached = t;
            } else {
                break; // los umbrales estan en orden ascendente
            }
        }
        return Optional.ofNullable(reached);
    }

    /**
     * Aumenta la fatiga (usar un poder). Clampa a 100 y notifica, en orden ascendente, cada umbral
     * recien cruzado hacia arriba.
     *
     * @throws IllegalArgumentException si {@code amount < 0}
     */
    public void add(int amount) {
        requireNonNegative(amount);
        int old = value;
        value = clamp(value + amount);
        if (value > old) {
            for (StrainThreshold t : StrainThreshold.values()) {
                if (old < t.percent() && t.percent() <= value) {
                    notifyCrossed(t, true);
                }
            }
        }
    }

    /**
     * Reduce la fatiga (regen/dormir parcial). Clampa a 0 y notifica, en orden descendente, cada umbral
     * dejado atras hacia abajo (rearme).
     *
     * @throws IllegalArgumentException si {@code amount < 0}
     */
    public void reduce(int amount) {
        requireNonNegative(amount);
        int old = value;
        value = clamp(value - amount);
        if (value < old) {
            StrainThreshold[] all = StrainThreshold.values();
            for (int i = all.length - 1; i >= 0; i--) {
                StrainThreshold t = all[i];
                if (value < t.percent() && t.percent() <= old) {
                    notifyCrossed(t, false);
                }
            }
        }
    }

    /**
     * Reinicia la fatiga a 0 <b>sin notificar</b> (equivale a dormir la noche entera, {@code [S02]}):
     * es un reset de estado, no un cruce de gameplay. Deja todos los umbrales rearmados.
     */
    public void reset() {
        value = MIN;
    }

    private void notifyCrossed(StrainThreshold threshold, boolean rising) {
        for (StrainListener listener : listeners) {
            listener.onThresholdCrossed(threshold, rising);
        }
    }

    private static int clamp(int value) {
        return Math.max(MIN, Math.min(value, MAX));
    }

    private static void requireNonNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount no puede ser negativo: " + amount);
        }
    }

    @Override
    public String toString() {
        return "StrainMeter[" + value + "/" + MAX + "]";
    }
}
