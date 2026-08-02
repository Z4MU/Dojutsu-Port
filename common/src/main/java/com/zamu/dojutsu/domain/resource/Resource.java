package com.zamu.dojutsu.domain.resource;

/**
 * Recurso numerico {@code current/max} con clamp al rango {@code [0, max]}.
 *
 * <p>Modela el patron 4 del ADD (recurso con regen timer->pulso): la base del chakra normal y del
 * chakra natural del datapack {@code [S02]}. Es dominio puro: no importa clases de Minecraft ni de
 * ningun loader. El estado real por jugador ({@code ChakraState}) y su servicio llegan en F2 y
 * reutilizan este tipo.
 *
 * <p>Invariante: tras cualquier operacion, {@code 0 <= current <= max}. En particular, sumar por
 * encima del maximo hace overshoot que se clampa de inmediato (fiel a S02-R4: el cap se aplica
 * siempre tras cada regen).
 */
public final class Resource {

    private int current;
    private int max;

    /**
     * Crea un recurso con un valor actual y un maximo. El valor actual se clampa a {@code [0, max]}.
     *
     * @param current valor inicial (se clampa)
     * @param max     maximo (no puede ser negativo)
     * @throws IllegalArgumentException si {@code max < 0}
     */
    public Resource(int current, int max) {
        if (max < 0) {
            throw new IllegalArgumentException("max no puede ser negativo: " + max);
        }
        this.max = max;
        this.current = clamp(current);
    }

    /** Crea un recurso lleno ({@code current == max}). */
    public static Resource full(int max) {
        return new Resource(max, max);
    }

    /** Crea un recurso vacio ({@code current == 0}). */
    public static Resource empty(int max) {
        return new Resource(0, max);
    }

    public int current() {
        return current;
    }

    public int max() {
        return max;
    }

    /** Fija el valor actual, clampado a {@code [0, max]}. */
    public void setCurrent(int value) {
        this.current = clamp(value);
    }

    /**
     * Cambia el maximo y reclampa el valor actual (si el nuevo max es menor, {@code current} baja).
     *
     * @throws IllegalArgumentException si {@code value < 0}
     */
    public void setMax(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("max no puede ser negativo: " + value);
        }
        this.max = value;
        this.current = clamp(current);
    }

    /**
     * Indica si hay recurso suficiente para pagar un coste.
     *
     * @throws IllegalArgumentException si {@code cost < 0}
     */
    public boolean has(int cost) {
        requireNonNegative(cost, "cost");
        return current >= cost;
    }

    /**
     * Consume el coste de forma "todo o nada": si no hay suficiente, no cambia nada.
     *
     * @return {@code true} si se consumio; {@code false} si no habia suficiente
     * @throws IllegalArgumentException si {@code cost < 0}
     */
    public boolean consume(int cost) {
        requireNonNegative(cost, "cost");
        if (current < cost) {
            return false;
        }
        current -= cost;
        return true;
    }

    /**
     * Suma una cantidad (regen, comida...). El overshoot se clampa a {@code max}.
     *
     * @throws IllegalArgumentException si {@code amount < 0}
     */
    public void add(int amount) {
        requireNonNegative(amount, "amount");
        setCurrent(current + amount);
    }

    /** Deja el recurso al maximo (equivale a {@code chakra/restore} del datapack). */
    public void restore() {
        current = max;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(value, max));
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " no puede ser negativo: " + value);
        }
    }

    @Override
    public String toString() {
        return "Resource[" + current + "/" + max + "]";
    }
}
