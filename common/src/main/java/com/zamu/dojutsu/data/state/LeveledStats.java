package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.domain.progression.LeveledStat;
import com.zamu.dojutsu.domain.progression.XpCurve;

/**
 * Serializacion compartida de {@link LeveledStat} para los sub-estados de {@code data.state}.
 *
 * <p>Vive en la capa {@code data} porque el dominio no conoce {@code Codec}. Lo usan {@link ProgressionState}
 * (habilidades) y {@link NatureProfile} (afinidades): ambos son el <b>mismo eje</b> nivel/XP con la misma
 * {@link XpCurve} ({@code [S13-R1]}), asi que comparten codec en vez de duplicarlo.
 *
 * <p>Clase de utilidad no instanciable.
 */
public final class LeveledStats {

    /** Codec de un {@link LeveledStat}; tolerante (campos ausentes -> 0) y saneado (ver {@link #safe}). */
    public static final Codec<LeveledStat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("level", 0).forGetter(LeveledStat::level),
            Codec.INT.optionalFieldOf("xp", 0).forGetter(LeveledStat::xp)
    ).apply(instance, LeveledStats::safe));

    private LeveledStats() {
    }

    /**
     * Construye un {@link LeveledStat} clampando a un rango legal: el constructor del dominio lanza si el
     * nivel/XP son invalidos, y un NBT corrupto no debe impedir cargar la partida.
     *
     * @param level nivel leido (se clampa a {@code [0, XpCurve.MAX_LEVEL]})
     * @param xp    XP leida (se clampa a {@code >= 0})
     */
    public static LeveledStat safe(int level, int xp) {
        int safeLevel = Math.max(0, Math.min(level, XpCurve.MAX_LEVEL));
        return new LeveledStat(safeLevel, Math.max(0, xp));
    }
}
