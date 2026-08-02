package com.zamu.dojutsu.domain.progression;

/**
 * Estadistica con nivel y experiencia acumulada (afinidad o habilidad) {@code [S13]}.
 *
 * <p><b>Value object inmutable</b> (DJT-063): {@link #withXpAdded(int)} <b>devuelve una instancia nueva</b> en
 * vez de mutar. Antes era mutable, lo que hacia corruptible cualquier instancia compartida — en particular las
 * de {@code ProgressionState.DEFAULT}/{@code NatureProfile.DEFAULT}, que son estaticas: un
 * {@code DEFAULT.skill(X).addXp(n)} habria envenenado el default de todo el proceso. Al ser inmutable, ese fallo
 * es <b>imposible de escribir</b>, no solo desaconsejado; ademas encaja con el resto de {@code data.state}, que
 * ya era inmutable y lo trataba como valor.
 *
 * <p>Al sumar XP sube tantos niveles como permita la {@link XpCurve}, con <b>arrastre</b> del sobrante, hasta
 * {@link XpCurve#MAX_LEVEL}. Corrige dos comportamientos del datapack: elimina la guardia {@code d.dolvl} de
 * "1 level-up por tick" (sube todo lo que corresponda en una llamada) y no descarta el sobrante al subir (lo
 * arrastra) — salvo en el cap.
 *
 * <p>Dominio puro (sin imports de Minecraft). {@code equals}/{@code hashCode} por valor (nivel + xp), generados
 * por el record: se usa como valor en los mapas de {@code ProgressionState} y {@code NatureProfile}.
 *
 * @param level nivel actual, en {@code [0, XpCurve.MAX_LEVEL]}
 * @param xp    XP acumulada hacia el siguiente nivel (no negativa)
 */
public record LeveledStat(int level, int xp) {

    /**
     * Valida el estado. La capa {@code data} sanea el NBT corrupto antes de llegar aqui
     * ({@code LeveledStats.safe}), asi que un valor invalido en este punto es un bug de programacion.
     *
     * @throws IllegalArgumentException si {@code level} esta fuera de {@code [0, MAX_LEVEL]} o {@code xp < 0}
     */
    public LeveledStat {
        if (level < 0 || level > XpCurve.MAX_LEVEL) {
            throw new IllegalArgumentException("level fuera de [0, " + XpCurve.MAX_LEVEL + "]: " + level);
        }
        if (xp < 0) {
            throw new IllegalArgumentException("xp no puede ser negativa: " + xp);
        }
    }

    /** Crea la estadistica a nivel 0 con 0 XP. */
    public LeveledStat() {
        this(0, 0);
    }

    /** Indica si la estadistica alcanzo el nivel maximo. */
    public boolean isMaxLevel() {
        return level >= XpCurve.MAX_LEVEL;
    }

    /**
     * Devuelve una <b>copia</b> con {@code amount} de XP sumada, subiendo tantos niveles como permita y
     * arrastrando el sobrante. En el cap no acumula XP ni progresa (devuelve {@code this}).
     *
     * <p>Se llama {@code withXpAdded} y no {@code addXp} a proposito: en este proyecto el prefijo {@code with}
     * significa "copia con", asi que ignorar el valor devuelto se lee como el error que es. {@code addXp}
     * sugeriria una mutacion que ya no ocurre.
     *
     * @param amount XP a sumar (no negativa)
     * @throws IllegalArgumentException si {@code amount < 0}
     */
    public LeveledStat withXpAdded(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount no puede ser negativo: " + amount);
        }
        if (isMaxLevel()) {
            return this; // en el cap no hay progreso posible
        }
        int newLevel = level;
        int newXp = xp + amount;
        while (newLevel < XpCurve.MAX_LEVEL && newXp >= XpCurve.xpForLevel(newLevel)) {
            newXp -= XpCurve.xpForLevel(newLevel);
            newLevel++;
        }
        if (newLevel >= XpCurve.MAX_LEVEL) {
            newXp = 0; // en el cap se descarta el sobrante
        }
        return new LeveledStat(newLevel, newXp);
    }
}
