package com.zamu.dojutsu.domain.rng;

import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * Tabla de pesos pura que reemplaza las loot tables usadas como RNG en el datapack ({@code *_rng} de KG,
 * clan, trader...). El origen del azar se <b>inyecta</b> en {@link #roll(RandomGenerator)}, lo que la hace
 * determinista y testeable con semilla fija.
 *
 * <p>Nota de arquitectura: el ADD menciona {@code RandomSource}, pero esa es una clase de Minecraft y el
 * dominio no puede depender de ella (cero imports de MC). Se usa el {@link RandomGenerator} del JDK; el
 * consumidor (F2+) adapta el {@code RandomSource} del mundo/entidad a esta interfaz.
 *
 * <p>Inmutable y generica. Dominio puro.
 *
 * @param <T> tipo de los valores de la tabla
 */
public final class RollTable<T> {

    private final List<WeightedEntry<T>> entries;
    private final int totalWeight;

    /** Crea una tabla a partir de sus entradas (se copia de forma defensiva e inmutable). */
    public RollTable(List<WeightedEntry<T>> entries) {
        Objects.requireNonNull(entries, "entries");
        this.entries = List.copyOf(entries); // inmutable y rechaza nulos
        int total = 0;
        for (WeightedEntry<T> entry : this.entries) {
            total += entry.weight();
        }
        this.totalWeight = total;
    }

    /** Crea una tabla a partir de entradas sueltas. */
    @SafeVarargs
    public static <T> RollTable<T> of(WeightedEntry<T>... entries) {
        return new RollTable<>(List.of(entries));
    }

    /**
     * Tira la tabla con el generador dado y devuelve el valor elegido, con probabilidad proporcional a su
     * peso. Las entradas de peso 0 nunca se eligen.
     *
     * @throws IllegalStateException si la tabla no tiene peso total positivo (vacia o todos los pesos 0)
     */
    public T roll(RandomGenerator random) {
        Objects.requireNonNull(random, "random");
        if (totalWeight <= 0) {
            throw new IllegalStateException("RollTable sin peso total positivo: no se puede tirar");
        }
        int r = random.nextInt(totalWeight); // [0, totalWeight)
        int cumulative = 0;
        for (WeightedEntry<T> entry : entries) {
            cumulative += entry.weight();
            if (r < cumulative) {
                return entry.value();
            }
        }
        throw new IllegalStateException("inalcanzable: r=" + r + " totalWeight=" + totalWeight);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int totalWeight() {
        return totalWeight;
    }

    /** Las entradas de la tabla (lista inmutable). */
    public List<WeightedEntry<T>> entries() {
        return entries;
    }
}
