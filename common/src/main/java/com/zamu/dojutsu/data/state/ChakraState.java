package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Sub-estado de chakra del ninja {@code [S02]}: los dos recursos numericos por jugador que reemplazan los
 * scoreboards {@code d.chakra}/{@code d.max_chakra} (chakra normal) y {@code d.sagechakra}/
 * {@code d.max_sagechakra} (chakra natural / senjutsu), mas sus acumuladores de regen
 * ({@code d.chakra_timer}, {@code d.jugo_timer}).
 *
 * <p>Es un <b>value object inmutable</b> que sigue el patron de sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}: expone {@link #DEFAULT} y {@link #CODEC} y se serializa como
 * un compound propio. La <b>escritura del chakra</b> se hace por su unica puerta,
 * {@link com.zamu.dojutsu.chakra.ChakraService} (nunca mutando campos aqui): este record solo transporta el
 * estado y garantiza su invariante.
 *
 * <p><b>Invariante</b> (saneo en el constructor, defensa ante NBT corrupto): los maximos nunca son negativos;
 * {@code chakra}/{@code naturalChakra} quedan en {@code [0, max]}; los timers nunca son negativos. El clamp
 * "de gameplay" tras cada operacion lo aplica ademas {@link com.zamu.dojutsu.chakra.ChakraService} via el
 * dominio {@link com.zamu.dojutsu.domain.resource.Resource} (fiel a S02-R4: el cap se aplica siempre).
 *
 * <p><b>Defaults</b> ({@code [S02]}): {@code maxChakra}=100 y {@code maxNaturalChakra}=100; el chakra inicial
 * es 0 (el datapack inicializa {@code d.chakra} a 0 cuando no existe y lo regenera con el tiempo). Los
 * servicios de mutacion del chakra natural (Jugo/senjutsu) llegan con la regeneracion (DJT-031) y el Modo
 * Sabio (F6); aqui el campo solo se persiste.
 *
 * @param chakra           chakra normal actual (0..maxChakra)
 * @param maxChakra        chakra normal maximo (default 100; se escala en fases posteriores)
 * @param naturalChakra    chakra natural / senjutsu actual (0..maxNaturalChakra)
 * @param maxNaturalChakra chakra natural maximo (default 100)
 * @param chakraTimer      acumulador de ticks para el pulso de regen normal (0..umbral)
 * @param jugoTimer        acumulador de ticks para la regen pasiva de chakra natural (Jugo)
 */
public record ChakraState(int chakra, int maxChakra, int naturalChakra, int maxNaturalChakra,
                          int chakraTimer, int jugoTimer) {

    /** Estado por defecto de un jugador nuevo ({@code [S02]}): recursos vacios, maximos 100, timers a 0. */
    public static final ChakraState DEFAULT = new ChakraState(0, 100, 0, 100, 0, 0);

    /** Codec del sub-estado (compound tolerante: cada campo ausente cae a su default de {@link #DEFAULT}). */
    public static final Codec<ChakraState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("chakra", DEFAULT.chakra()).forGetter(ChakraState::chakra),
            Codec.INT.optionalFieldOf("maxChakra", DEFAULT.maxChakra()).forGetter(ChakraState::maxChakra),
            Codec.INT.optionalFieldOf("naturalChakra", DEFAULT.naturalChakra()).forGetter(ChakraState::naturalChakra),
            Codec.INT.optionalFieldOf("maxNaturalChakra", DEFAULT.maxNaturalChakra()).forGetter(ChakraState::maxNaturalChakra),
            Codec.INT.optionalFieldOf("chakraTimer", DEFAULT.chakraTimer()).forGetter(ChakraState::chakraTimer),
            Codec.INT.optionalFieldOf("jugoTimer", DEFAULT.jugoTimer()).forGetter(ChakraState::jugoTimer)
    ).apply(instance, ChakraState::new));

    /** Sanea el invariante (maximos &gt;= 0, actuales en {@code [0, max]}, timers &gt;= 0). */
    public ChakraState {
        maxChakra = Math.max(0, maxChakra);
        maxNaturalChakra = Math.max(0, maxNaturalChakra);
        chakra = clamp(chakra, maxChakra);
        naturalChakra = clamp(naturalChakra, maxNaturalChakra);
        chakraTimer = Math.max(0, chakraTimer);
        jugoTimer = Math.max(0, jugoTimer);
    }

    /** Devuelve una copia con otro chakra normal (se clampa a {@code [0, maxChakra]}). Inmutable. */
    public ChakraState withChakra(int newChakra) {
        return new ChakraState(newChakra, maxChakra, naturalChakra, maxNaturalChakra, chakraTimer, jugoTimer);
    }

    /**
     * Devuelve una copia con otro chakra maximo ({@code d.max_chakra}: lo fija el clan al unirse, DJT-057).
     * El chakra actual se re-clampa al nuevo maximo (invariante del constructor). Inmutable.
     */
    public ChakraState withMaxChakra(int newMaxChakra) {
        return new ChakraState(chakra, newMaxChakra, naturalChakra, maxNaturalChakra, chakraTimer, jugoTimer);
    }

    /** Devuelve una copia con otro acumulador de regen normal ({@code d.chakra_timer}). Inmutable. */
    public ChakraState withChakraTimer(int newTimer) {
        return new ChakraState(chakra, maxChakra, naturalChakra, maxNaturalChakra, newTimer, jugoTimer);
    }

    /** Devuelve una copia con otro chakra natural (se clampa a {@code [0, maxNaturalChakra]}). Inmutable. */
    public ChakraState withNaturalChakra(int newNaturalChakra) {
        return new ChakraState(chakra, maxChakra, newNaturalChakra, maxNaturalChakra, chakraTimer, jugoTimer);
    }

    /** Devuelve una copia con otro timer de regen de chakra natural ({@code d.jugo_timer}). Inmutable. */
    public ChakraState withJugoTimer(int newJugoTimer) {
        return new ChakraState(chakra, maxChakra, naturalChakra, maxNaturalChakra, chakraTimer, newJugoTimer);
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(value, max));
    }
}
