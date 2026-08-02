package com.zamu.dojutsu.domain.summoning;

import java.util.Locale;
import java.util.Optional;

/**
 * Los tres <b>tiers de invocacion</b> {@code [S15]} (F8/DJT-175): que hace falta para <b>firmar</b> un contrato y
 * que para <b>usarlo</b>. Verificados en {@code summoning/attempt_form_contract} y en los predicados
 * {@code can_use_low/mid/high}, con los tags {@code #dojutsu:contractable_*}.
 *
 * <h2>Firmar y usar piden cosas distintas</h2>
 * Es facil pasarlo por alto porque los numeros se parecen, pero son dos puertas:
 * <ul>
 *   <li><b>Firmar</b> exige rango Chunin ({@value SummonTable#CONTRACT_MIN_RANK}) para <b>cualquier</b> tier, y
 *       el fuinjutsu solo entra en juego en mid y high.</li>
 *   <li><b>Usar</b> exige fuinjutsu <b>tambien en el tier bajo</b>, y el rango sube a
 *       {@value SummonTable#HIGH_TIER_RANK} para el alto.</li>
 * </ul>
 * O sea: puedes firmar un contrato que todavia no puedes usar. Es coherente —firmar es un pacto, invocar es
 * pagarlo— y es lo que hace el datapack.
 *
 * <p>Reparto verificado: <b>13</b> especies en el tier bajo (bichos de granja), <b>12</b> en el medio (monturas y
 * bestias) y <b>2</b> en el alto (aldeano y mercader errante). 13 + 12 + 2 = 27.
 *
 * <p>Requisitos de fuinjutsu en <b>escala del mod</b> (datapack ÷ 4, convencion de DJT-082): el 600 del datapack
 * es {@value #MID_FUINJUTSU} y el 800 es {@value #HIGH_FUINJUTSU}.
 *
 * @param contractFuinjutsu fuinjutsu para <b>firmar</b> un contrato de este tier
 * @param useFuinjutsu      fuinjutsu para <b>invocar</b> con el contrato
 * @param useRank           ordinal minimo de rango ninja para invocar
 */
public enum SummonTier {

    /** Tier bajo: firmar solo pide ser Chunin; usar, algo de fuinjutsu. */
    LOW(0, 75, SummonTable.CONTRACT_MIN_RANK),
    /** Tier medio: monturas y bestias. */
    MID(150, 150, SummonTable.CONTRACT_MIN_RANK),
    /** Tier alto: solo aldeano y mercader errante, y piden un rango mas. */
    HIGH(200, 200, SummonTable.HIGH_TIER_RANK);

    /** Fuinjutsu (escala del mod) del tier medio: datapack 600 ÷ 4. */
    public static final int MID_FUINJUTSU = 150;

    /** Fuinjutsu (escala del mod) del tier alto: datapack 800 ÷ 4. */
    public static final int HIGH_FUINJUTSU = 200;

    private final int contractFuinjutsu;
    private final int useFuinjutsu;
    private final int useRank;

    SummonTier(int contractFuinjutsu, int useFuinjutsu, int useRank) {
        this.contractFuinjutsu = contractFuinjutsu;
        this.useFuinjutsu = useFuinjutsu;
        this.useRank = useRank;
    }

    /** Id data-driven en minusculas ({@code "low"}, {@code "mid"}, {@code "high"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Fuinjutsu para firmar un contrato de este tier. */
    public int contractFuinjutsu() {
        return contractFuinjutsu;
    }

    /** Fuinjutsu para invocar con el contrato. */
    public int useFuinjutsu() {
        return useFuinjutsu;
    }

    /** Ordinal minimo de rango ninja para invocar (2 = Chunin, 3 = Tokubetsu Jonin). */
    public int useRank() {
        return useRank;
    }

    /** Indica si con ese fuinjutsu y ese rango se puede <b>firmar</b> un contrato de este tier. */
    public boolean canForm(int fuinjutsu, int rankOrdinal) {
        return rankOrdinal >= SummonTable.CONTRACT_MIN_RANK
                && fuinjutsu >= contractFuinjutsu
                && (this != HIGH || rankOrdinal >= SummonTable.HIGH_TIER_RANK);
    }

    /** Indica si con ese fuinjutsu y ese rango se puede <b>invocar</b> con un contrato de este tier. */
    public boolean canSummon(int fuinjutsu, int rankOrdinal) {
        return fuinjutsu >= useFuinjutsu && rankOrdinal >= useRank;
    }

    /** Resuelve un tier por su id; vacio si no existe (estado tolerante). */
    public static Optional<SummonTier> byId(String id) {
        for (SummonTier tier : values()) {
            if (tier.id().equals(id)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }
}
