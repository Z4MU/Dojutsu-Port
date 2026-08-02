package com.zamu.dojutsu.domain.bijuu;

import java.util.Locale;
import java.util.Optional;

/**
 * Las diez <b>Bestias con Cola</b> {@code [S09]} (F7/DJT-160). Verificadas en
 * {@code functions/tailed_beasts/spawn/*} (identidad y modelo), {@code ai/cast_1} (tecnica insignia) y
 * {@code tailed_beasts/death} (color y nombre del chakra que sueltan).
 *
 * <p><b>Data-driven por enum, no diez clases</b> (RA-3): las diez bestias comparten estadisticas
 * ({@link BeastStats}) y solo difieren en <b>identidad</b> y en <b>que tecnica insignia</b> usan. La lista de
 * Diez Colas es especial y se marca aqui, no con ramas dispersas: pega mas rapido ({@link BeastStats}),
 * tiene su propia Bola Bestia con Cola y un cuarto slot de tecnica que ninguna otra usa.
 *
 * @param tails       numero de colas (1..10); es el {@code d.tb_N} del datapack
 * @param signature   id de la {@code BeastMove} insignia ({@code ai/cast_1})
 * @param chakraColor color del chakra que suelta al morir, en hex del datapack ({@code tailed_beasts/death})
 */
public enum TailedBeast {

    /** Shukaku, el Mapache de una Cola. */
    ONE_TAIL(1, "sand_buckshot", 0xE2C496),
    /** Matatabi, la Gata de dos Colas. */
    TWO_TAILS(2, "cat_flame_roaring_fire", 0x4F72FF),
    /** Isobu, la Tortuga de tres Colas. */
    THREE_TAILS(3, "great_water_mass_bullet", 0x800049),
    /** Son Goku, el Mono de cuatro Colas. */
    FOUR_TAILS(4, "monkey_flame_arson", 0xA71B1B),
    /** Kokuo, el Caballo de cinco Colas. */
    FIVE_TAILS(5, "unrivalled_strength", 0xE8E8F0),
    /** Saiken, la Babosa de seis Colas. */
    SIX_TAILS(6, "soap_bubble_barrage", 0xD3D9E7),
    /** Chomei, el Escarabajo de siete Colas. */
    SEVEN_TAILS(7, "scale_powder", 0x584FD0),
    /** Gyuki, el Buey-Pulpo de ocho Colas. */
    EIGHT_TAILS(8, "tentacle_barrage", 0xB0717A),
    /** Kurama, el Zorro de nueve Colas. */
    NINE_TAILS(9, "chakra_arms_barrage", 0xFF8000),
    /** El Diez Colas: no es una bestia mas — ver {@link #isTenTails()}. */
    TEN_TAILS(10, "tenpenchii", 0x8E776C);

    private final int tails;
    private final String signature;
    private final int chakraColor;

    TailedBeast(int tails, String signature, int chakraColor) {
        this.tails = tails;
        this.signature = signature;
        this.chakraColor = chakraColor;
    }

    /** Numero de colas (1..10). */
    public int tails() {
        return tails;
    }

    /** Id data-driven en minusculas ({@code "one_tail"}, {@code "nine_tails"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Id de su tecnica insignia ({@code ai/cast_1}). */
    public String signatureMove() {
        return signature;
    }

    /** Color del chakra que suelta al morir (hex RGB). */
    public int chakraColor() {
        return chakraColor;
    }

    /**
     * Indica si es el Diez Colas. Es el <b>nodo especial</b> de {@code [S09]} (riesgo S09-R7): ataca mas
     * seguido, tiene su propia Bola Bestia con Cola y un cuarto slot de tecnica. Su contenido completo —Arbol
     * Divino, Orbes Buscadores de la Verdad, Seis Caminos— converge en <b>F9</b>, no aqui.
     */
    public boolean isTenTails() {
        return this == TEN_TAILS;
    }

    /** Resuelve una bestia por su id data-driven; vacio si no existe (estado tolerante). */
    public static Optional<TailedBeast> byId(String id) {
        for (TailedBeast beast : values()) {
            if (beast.id().equals(id)) {
                return Optional.of(beast);
            }
        }
        return Optional.empty();
    }

    /** Resuelve una bestia por su numero de colas (1..10); vacio fuera de rango. */
    public static Optional<TailedBeast> byTails(int tails) {
        for (TailedBeast beast : values()) {
            if (beast.tails == tails) {
                return Optional.of(beast);
            }
        }
        return Optional.empty();
    }
}
