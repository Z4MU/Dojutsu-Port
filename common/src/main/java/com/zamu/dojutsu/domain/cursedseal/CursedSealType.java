package com.zamu.dojutsu.domain.cursedseal;

import java.util.Locale;
import java.util.Optional;

/**
 * Los seis <b>sellos malditos</b> {@code [S10]} (F6/DJT-140): cada uno con su propia aura de potenciacion.
 * Verificados en {@code functions/cursed_seal/} y en el reparto aleatorio de {@code cursed_seal/success}
 * ({@code d.cseal_rng} 1..6).
 *
 * <p>El <b>aura</b> (efectos por tick) se expresa como amplificadores por nivel: dominio puro, sin
 * {@code MobEffect} — el glue los traduce. Cada sello tiene un perfil distinto: el <b>Cielo</b> es equilibrado,
 * <b>Tierra</b> corre mas en suelo, <b>Jirobo</b> es puro musculo, <b>Kidomaru</b> es agilidad, <b>Sukon/Ukon</b>
 * empieza flojo y <b>Tayuya</b> es intermedio.
 *
 * <p>Las <b>habilidades propias</b> del nivel 2 (vuelo del Cielo, aim-assist de Kidomaru, clon de Sukon/Ukon,
 * genjutsu sonoro de Tayuya) quedan <b>fuera de este ticket</b>: son efectos novedosos, uno por sello, que se
 * clonan con la plantilla en lotes posteriores como se hizo con las habilidades oculares.
 *
 * @param strength   amplificador de fuerza en nivel 1 y 2
 * @param speed      amplificador de velocidad en nivel 1 y 2
 * @param jumpBoost  amplificador de salto en nivel 1 y 2 ({@code -1} = el sello no lo da)
 * @param resistance amplificador de resistencia en nivel 1 y 2 ({@code -1} = no lo da)
 */
public enum CursedSealType {

    /** Cielo: equilibrado; en nivel 2 desbloquea <b>vuelo</b>. Bonus permanente de chakra y afinidades. */
    HEAVEN(1, new int[]{1, 2}, new int[]{1, 3}, new int[]{4, 6}, new int[]{1, 1}),
    /** Tierra: fuerza y salto; corre mucho mas <b>en suelo</b> (speed 3/6, matiz del datapack). */
    EARTH(2, new int[]{1, 2}, new int[]{3, 6}, new int[]{4, 6}, new int[]{1, 1}),
    /** Jirobo: puro musculo — la mayor fuerza (3 en nivel 2) y aguante, poca velocidad. */
    JIROBO(3, new int[]{1, 3}, new int[]{1, 1}, new int[]{-1, -1}, new int[]{1, 2}),
    /** Kidomaru: agilidad extrema (speed 6, salto 8 en nivel 2); en nivel 2, <b>aim-assist</b> con el arco. */
    KIDOMARU(4, new int[]{0, 0}, new int[]{3, 6}, new int[]{4, 8}, new int[]{0, 0}),
    /** Sukon/Ukon: el mas flojo de base; en nivel 2 puede crear un <b>clon</b> que ataca. */
    SUKON_UKON(5, new int[]{0, 1}, new int[]{3, 3}, new int[]{-1, -1}, new int[]{0, 1}),
    /** Tayuya: intermedio; en nivel 2 usa un <b>genjutsu sonoro</b>. */
    TAYUYA(6, new int[]{1, 2}, new int[]{1, 1}, new int[]{-1, -1}, new int[]{1, 2});

    /** El sello no otorga ese efecto. */
    public static final int NONE = -1;

    private final int datapackId;
    private final int[] strength;
    private final int[] speed;
    private final int[] jumpBoost;
    private final int[] resistance;

    CursedSealType(int datapackId, int[] strength, int[] speed, int[] jumpBoost, int[] resistance) {
        this.datapackId = datapackId;
        this.strength = strength;
        this.speed = speed;
        this.jumpBoost = jumpBoost;
        this.resistance = resistance;
    }

    /** Valor de {@code d.cseal_rng}/{@code has_seal_N} en el datapack (1..6); solo para trazabilidad. */
    public int datapackId() {
        return datapackId;
    }

    /** Id data-driven en minusculas ({@code "heaven"}, {@code "sukon_ukon"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Amplificador de fuerza en ese nivel (1 o 2). */
    public int strength(int level) {
        return strength[levelIndex(level)];
    }

    /** Amplificador de velocidad en ese nivel. */
    public int speed(int level) {
        return speed[levelIndex(level)];
    }

    /** Amplificador de salto, o {@link #NONE} si el sello no lo da. */
    public int jumpBoost(int level) {
        return jumpBoost[levelIndex(level)];
    }

    /** Amplificador de resistencia, o {@link #NONE} si el sello no lo da. */
    public int resistance(int level) {
        return resistance[levelIndex(level)];
    }

    /** Resuelve un sello por su id data-driven; vacio si no existe (estado tolerante). */
    public static Optional<CursedSealType> byId(String id) {
        for (CursedSealType type : values()) {
            if (type.id().equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Resuelve un sello por el valor 1..6 del reparto aleatorio ({@code cursed_seal_rng}). */
    public static Optional<CursedSealType> byDatapackId(int id) {
        for (CursedSealType type : values()) {
            if (type.datapackId() == id) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Indice 0/1 del nivel 1/2 (clampado). */
    private static int levelIndex(int level) {
        return level >= 2 ? 1 : 0;
    }
}
