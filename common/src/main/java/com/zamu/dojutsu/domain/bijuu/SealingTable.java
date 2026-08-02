package com.zamu.dojutsu.domain.bijuu;

import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Que te da sellar una Bestia con Cola {@code [S09]} (F7/DJT-165). Dominio puro y <b>verificado contra el
 * datapack</b> ({@code jinchuriki/sealing/*} y {@code removal/*}).
 *
 * <h2>La ficha decia "+8000"; el datapack dice "1000 por cola"</h2>
 * No es una cifra fija: {@link #maxChakraBonus} es {@code 1000 × colas}, de <b>1000</b> con Shukaku a
 * <b>10 000</b> con el Diez Colas. El aviso del ADD sigue siendo correcto en lo que importa —eso rompe la escala
 * normal de chakra— pero la forma es otra: <b>escala con la bestia</b>. La respuesta a {@code [RA-2]} tampoco es
 * "marcarlo como estado especial" en abstracto, sino <b>guardar cuanto se presto</b> para devolverlo exacto al
 * extraerla; {@code removal/*} hace justo eso, restando la misma cifra que sumo.
 *
 * <h2>La XP tiene dos escalones que rompen el patron</h2>
 * De la 1 a la 8 es {@code 100 + 10 × colas} (110…180). Pero el <b>Nueve Colas da 230</b>, no 190, y el
 * <b>Diez Colas da 1000</b>. Se guardan como tabla, no como formula, precisamente porque la formula miente en
 * los dos casos que mas importan.
 *
 * <h2>Cada bestia te presta sus elementos</h2>
 * Sellar concede naturalezas y, a veces, un Kekkei Genkai — <b>prestados</b>, no tuyos: el datapack los marca
 * {@code d.tmp_*}/{@code d.tkg_*} para poder retirarlos al extraer la bestia sin llevarse por delante lo que ya
 * era tuyo. El {@code TemporaryGrantService} (F3/DJT-053) se escribio para esto y hoy encuentra por fin su
 * consumidor real.
 */
public final class SealingTable {

    /** Chakra maximo prestado por cola ({@code sealing/*}: {@code max_chakra += 1000 × colas}). */
    public static final int MAX_CHAKRA_PER_TAIL = 1_000;

    /** Ticks de veto entre intentos de sellado ({@code d.tb_seal_cd = 40}). */
    public static final int SEAL_COOLDOWN_TICKS = 40;

    /** Nivel de jinchuriki al sellar ({@code d.jinchuriki_lvl = 1}). */
    public static final int INITIAL_LEVEL = 1;

    /** Ninja_xp por bestia; tabla y no formula porque el 9 y el 10 rompen el patron. */
    private static final Map<TailedBeast, Integer> NINJA_XP = Map.ofEntries(
            Map.entry(TailedBeast.ONE_TAIL, 110),
            Map.entry(TailedBeast.TWO_TAILS, 120),
            Map.entry(TailedBeast.THREE_TAILS, 130),
            Map.entry(TailedBeast.FOUR_TAILS, 140),
            Map.entry(TailedBeast.FIVE_TAILS, 150),
            Map.entry(TailedBeast.SIX_TAILS, 160),
            Map.entry(TailedBeast.SEVEN_TAILS, 170),
            Map.entry(TailedBeast.EIGHT_TAILS, 180),
            Map.entry(TailedBeast.NINE_TAILS, 230),   // rompe el patron (seria 190)
            Map.entry(TailedBeast.TEN_TAILS, 1_000)); // y el Diez Colas lo pulveriza

    /** Naturalezas que presta cada bestia ({@code sealing/*}); las que no aparecen no prestan ninguna. */
    private static final Map<TailedBeast, List<NatureType>> NATURES = Map.of(
            TailedBeast.ONE_TAIL, List.of(NatureType.WIND, NatureType.EARTH),
            TailedBeast.TWO_TAILS, List.of(NatureType.FIRE),
            TailedBeast.THREE_TAILS, List.of(NatureType.WATER),
            TailedBeast.FOUR_TAILS, List.of(NatureType.FIRE, NatureType.EARTH),
            TailedBeast.FIVE_TAILS, List.of(NatureType.FIRE, NatureType.WATER),
            TailedBeast.SIX_TAILS, List.of(NatureType.WATER),
            TailedBeast.TEN_TAILS, List.of(NatureType.FIRE, NatureType.WIND, NatureType.LIGHTNING,
                    NatureType.EARTH, NatureType.WATER, NatureType.YIN, NatureType.YANG));

    /** Kekkei Genkai que presta cada bestia; solo cuatro de las diez lo hacen. */
    private static final Map<TailedBeast, KekkeiGenkai> KEKKEI_GENKAI = Map.of(
            TailedBeast.ONE_TAIL, KekkeiGenkai.MAGNET,
            TailedBeast.FOUR_TAILS, KekkeiGenkai.LAVA,
            TailedBeast.FIVE_TAILS, KekkeiGenkai.BOIL,
            TailedBeast.TEN_TAILS, KekkeiGenkai.WOOD);

    private SealingTable() {
    }

    /** Chakra maximo que presta esa bestia: {@value #MAX_CHAKRA_PER_TAIL} por cola. */
    public static int maxChakraBonus(TailedBeast beast) {
        return MAX_CHAKRA_PER_TAIL * beast.tails();
    }

    /** Ninja_xp que da sellarla. */
    public static int ninjaXp(TailedBeast beast) {
        return NINJA_XP.getOrDefault(beast, 0);
    }

    /** Naturalezas que presta (vacio si no presta ninguna: el 7, el 8 y el 9 no dan elemento). */
    public static List<NatureType> naturesOf(TailedBeast beast) {
        return NATURES.getOrDefault(beast, List.of());
    }

    /** Kekkei Genkai que presta, o vacio. Solo Shukaku (Magnetismo), el 4 (Lava), el 5 (Ebullicion) y el 10 (Madera). */
    public static Optional<KekkeiGenkai> kekkeiGenkaiOf(TailedBeast beast) {
        return Optional.ofNullable(KEKKEI_GENKAI.get(beast));
    }
}
