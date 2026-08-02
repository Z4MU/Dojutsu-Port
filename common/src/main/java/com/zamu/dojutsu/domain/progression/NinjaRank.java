package com.zamu.dojutsu.domain.progression;

/**
 * Rango ninja (0..11) derivado de la XP ninja global {@code d.ninja_xp} {@code [S20]}.
 *
 * <p>Reemplaza los 12 predicates {@code ninja/rank/req_*} por una tabla de umbrales. El {@link #ordinal()}
 * coincide con {@code d.ninja_rank} (0 = Academy). Es puramente informativo (un titulo). Dominio puro.
 *
 * <pre>
 *   0 Academy Student  0-999          6 Kage                  15000-17999
 *   1 Genin            1000-1999      7 Legendary             18000-23999
 *   2 Chunin           2000-4999      8 God of Shinobi        24000-29999
 *   3 Tokubetsu Jonin  5000-8999      9 Sage of the Six Paths 30000-34999
 *   4 Jonin            9000-11999    10 Demigod               35000-39999
 *   5 Sannin           12000-14999   11 God                   40000+
 * </pre>
 */
public enum NinjaRank {

    ACADEMY_STUDENT(0, "Academy Student"),
    GENIN(1_000, "Genin"),
    CHUNIN(2_000, "Chunin"),
    TOKUBETSU_JONIN(5_000, "Tokubetsu Jonin"),
    JONIN(9_000, "Jonin"),
    SANNIN(12_000, "Sannin"),
    KAGE(15_000, "Kage"),
    LEGENDARY(18_000, "Legendary"),
    GOD_OF_SHINOBI(24_000, "God of Shinobi"),
    SAGE_OF_THE_SIX_PATHS(30_000, "Sage of the Six Paths"),
    DEMIGOD(35_000, "Demigod"),
    GOD(40_000, "God");

    private final int minXp;
    private final String title;

    NinjaRank(int minXp, String title) {
        this.minXp = minXp;
        this.title = title;
    }

    /** XP ninja minima para alcanzar este rango. */
    public int minXp() {
        return minXp;
    }

    /** Titulo mostrado del rango (romanizacion sin macrones para consistencia de codificacion). */
    public String title() {
        return title;
    }

    /** Valor de {@code d.ninja_rank} (0..11). */
    public int level() {
        return ordinal();
    }

    /**
     * Rango correspondiente a una XP ninja global, segun los umbrales de {@code [S20]}.
     *
     * @param ninjaXp XP ninja acumulada (&ge; 0)
     * @throws IllegalArgumentException si {@code ninjaXp < 0}
     */
    public static NinjaRank fromXp(int ninjaXp) {
        if (ninjaXp < 0) {
            throw new IllegalArgumentException("ninjaXp no puede ser negativa: " + ninjaXp);
        }
        NinjaRank result = ACADEMY_STUDENT;
        for (NinjaRank rank : values()) {
            if (ninjaXp >= rank.minXp) {
                result = rank;
            } else {
                break; // umbrales en orden ascendente
            }
        }
        return result;
    }
}
