package com.zamu.dojutsu.domain.items;

import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import java.util.Locale;
import java.util.Optional;

/**
 * Los <b>26 tipos de ADN</b> implantables {@code [S14]} (F8/DJT-170). Verificados en
 * {@code items/dna/implant_dna} (el reparto 1..26) y {@code dna/get_id} (la identificacion).
 *
 * <h2>Las tres familias, y por que hacen falta tres</h2>
 * El datapack multiplexa los 26 sobre un solo {@code d.dna_tmp}, pero conceden cosas <b>distintas</b>:
 * <ul>
 *   <li><b>7 naturalezas</b> (1-7): fuego, viento, rayo, tierra, agua, Yin y Yang.</li>
 *   <li><b>18 Kekkei Genkai</b> (8-17 y 19-26).</li>
 *   <li><b>Las celulas de Hashirama</b> (18), que <b>no son un KG</b>: no escriben ningun {@code d.kg_*} sino el
 *       flag {@code d.has_hsdna}. Eso ya estaba visto y resuelto en F3 —viven en {@code NatureProfile}— y aqui
 *       el enum lo respeta en vez de forzarlas dentro de {@code KekkeiGenkai}.</li>
 * </ul>
 * Con esa separacion los numeros cuadran exactos: 7 + 18 + 1 = 26.
 *
 * @param datapackId valor de {@code d.dna_tmp} / {@code DNAId} (1..26); solo trazabilidad
 * @param nature     naturaleza que concede, si es de esa familia
 * @param kekkeiGenkai KG que concede, si es de esa familia
 */
public enum DnaType {

    FIRE(1, NatureType.FIRE, null),
    WIND(2, NatureType.WIND, null),
    LIGHTNING(3, NatureType.LIGHTNING, null),
    EARTH(4, NatureType.EARTH, null),
    WATER(5, NatureType.WATER, null),
    YIN(6, NatureType.YIN, null),
    YANG(7, NatureType.YANG, null),
    WOOD(8, null, KekkeiGenkai.WOOD),
    BLAZE(9, null, KekkeiGenkai.BLAZE),
    ICE(10, null, KekkeiGenkai.ICE),
    LAVA(11, null, KekkeiGenkai.LAVA),
    STORM(12, null, KekkeiGenkai.STORM),
    BOIL(13, null, KekkeiGenkai.BOIL),
    DUST(14, null, KekkeiGenkai.DUST),
    MAGNET(15, null, KekkeiGenkai.MAGNET),
    EXPLOSION(16, null, KekkeiGenkai.EXPLOSION),
    SCORCH(17, null, KekkeiGenkai.SCORCH),
    /** Celulas de Hashirama: ni naturaleza ni KG — un flag propio ({@code d.has_hsdna}). */
    HASHIRAMA_CELLS(18, null, null),
    SHIKOTSUMYAKU(19, null, KekkeiGenkai.SHIKOTSUMYAKU),
    JUGO_CLAN(20, null, KekkeiGenkai.JUGO_CLAN),
    STEEL(21, null, KekkeiGenkai.STEEL),
    DARK(22, null, KekkeiGenkai.DARK),
    SWIFT(23, null, KekkeiGenkai.SWIFT),
    CRYSTAL(24, null, KekkeiGenkai.CRYSTAL),
    TYPHOON(25, null, KekkeiGenkai.TYPHOON),
    MUD(26, null, KekkeiGenkai.MUD);

    private final int datapackId;
    private final NatureType nature;
    private final KekkeiGenkai kekkeiGenkai;

    DnaType(int datapackId, NatureType nature, KekkeiGenkai kekkeiGenkai) {
        this.datapackId = datapackId;
        this.nature = nature;
        this.kekkeiGenkai = kekkeiGenkai;
    }

    /** Valor de {@code DNAId} en el datapack (1..26). */
    public int datapackId() {
        return datapackId;
    }

    /** Id data-driven en minusculas ({@code "wood"}, {@code "hashirama_cells"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Naturaleza que concede, si la concede. */
    public Optional<NatureType> nature() {
        return Optional.ofNullable(nature);
    }

    /** Kekkei Genkai que concede, si lo concede. */
    public Optional<KekkeiGenkai> kekkeiGenkai() {
        return Optional.ofNullable(kekkeiGenkai);
    }

    /** Indica si son las celulas de Hashirama (el unico ADN que no es ni naturaleza ni KG). */
    public boolean isHashiramaCells() {
        return this == HASHIRAMA_CELLS;
    }

    /** Resuelve un ADN por su id data-driven; vacio si no existe (estado tolerante). */
    public static Optional<DnaType> byId(String id) {
        for (DnaType type : values()) {
            if (type.id().equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Resuelve un ADN por su valor 1..26 del datapack. */
    public static Optional<DnaType> byDatapackId(int id) {
        for (DnaType type : values()) {
            if (type.datapackId == id) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
