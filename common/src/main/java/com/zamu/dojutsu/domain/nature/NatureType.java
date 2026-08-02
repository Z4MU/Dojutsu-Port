package com.zamu.dojutsu.domain.nature;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Naturalezas de chakra del ninja {@code [S03]}: los 5 elementos basicos mas las 3 avanzadas. Reemplaza los
 * flags {@code d.has_<nat>} del datapack.
 *
 * <p><b>Dos ejes, no uno</b> (evidencia directa del datapack, {@code [S03-R5]}):
 * <ul>
 *   <li>La <b>afinidad</b> de una naturaleza es un {@code LeveledStat} cuyo nivel es {@code d.aff_<nat>} y cuya
 *       XP es {@code d.<nat>_xp}. Sube por puntos ({@code menu/leveling/affinity/try_increase_*}) y auto-nivela
 *       con la misma {@code XpCurve} que las habilidades ({@code menu/leveling/main} + los predicates
 *       {@code lvl_req/<nat>_0_10}...{@code <nat>_500}, que leen {@code d.aff_<nat>}). Es la <b>escala
 *       compartida</b> de {@code [S13-R1]}: mismo eje que {@code Skill}, no uno paralelo.</li>
 *   <li>{@code d.<nat>_afflvl} / {@code d.<nat>_aff_xp} son un sistema <b>distinto</b> (entrenamiento de
 *       afinidad, {@code jutsu/training/*}, cap 30) cuyo premio al completarse es <b>otorgar la naturaleza</b>.
 *       No es el nivel de afinidad y no se modela aqui (llegara con el entrenamiento de jutsus, F4).</li>
 * </ul>
 *
 * <p><b>{@link #YIN_YANG} es especial:</b> es una naturaleza real (tiene su flag {@code d.has_yinyang} y otorga
 * 100 de ninja_xp), pero <b>no almacena afinidad propia</b>: {@code d.aff_yinyang} es <b>derivada</b>
 * ({@code menu/leveling/affinity/update_yinyang}: {@code (aff_yin + aff_yang) / 2}) y no tiene objetivo de XP.
 * Por eso {@link #storesAffinity()} la excluye y {@code NatureProfile} la calcula (misma postura que el rango
 * ninja: derivado, nunca cacheado).
 *
 * <p>Dominio puro: sin imports de Minecraft.
 */
public enum NatureType {

    FIRE(Category.BASIC, 25),
    WIND(Category.BASIC, 20),
    LIGHTNING(Category.BASIC, 25),
    EARTH(Category.BASIC, 20),
    WATER(Category.BASIC, 20),
    /** Yin: naturaleza avanzada (genjutsu). */
    YIN(Category.ADVANCED, 30),
    /** Yang: naturaleza avanzada. */
    YANG(Category.ADVANCED, 30),
    /** Yin-Yang: la mas rara; su afinidad es <b>derivada</b> de Yin y Yang. */
    YIN_YANG(Category.COMBINED, 100);

    /** Las 5 basicas ({@code predicates/has_all_basic_natures}): dan el boost {@code all_basics}. */
    private static final Set<NatureType> BASICS =
            Collections.unmodifiableSet(EnumSet.of(FIRE, WIND, LIGHTNING, EARTH, WATER));

    /** Las 7 con afinidad almacenada; coinciden con {@code predicates/has_all_natures} (ver {@link #withAffinity()}). */
    private static final Set<NatureType> WITH_AFFINITY =
            Collections.unmodifiableSet(EnumSet.of(FIRE, WIND, LIGHTNING, EARTH, WATER, YIN, YANG));

    private final Category category;
    private final int ninjaXp;

    NatureType(Category category, int ninjaXp) {
        this.category = category;
        this.ninjaXp = ninjaXp;
    }

    public Category category() {
        return category;
    }

    /**
     * ninja_xp que otorga obtener esta naturaleza (y que se resta al perderla: los {@code take/*} del datapack
     * son exactamente simetricos). Valores <b>auditados uno a uno</b> en {@code natures/give/<nat>} (RA-4).
     *
     * <p><b>Ojo:</b> {@code [S03]} resume "basicas elementales = 25", pero <b>no es uniforme</b>: fire y
     * lightning dan 25; wind, earth y water dan <b>20</b>. Manda el datapack, no el resumen.
     */
    public int ninjaXp() {
        return ninjaXp;
    }

    /**
     * Indica si la naturaleza <b>almacena</b> su propia afinidad ({@code d.aff_<nat>} + {@code d.<nat>_xp}).
     * Falso solo para {@link #YIN_YANG}, cuya afinidad se deriva de Yin/Yang.
     */
    public boolean storesAffinity() {
        return category != Category.COMBINED;
    }

    /**
     * Las 5 naturalezas basicas ({@code predicates/has_all_basic_natures}: fire, wind, ligh, earth, water).
     * Tenerlas todas otorga el boost {@code d.all_basics} (+100 ninja_xp, {@code natures/boost}).
     */
    public static Set<NatureType> basics() {
        return BASICS;
    }

    /**
     * Las 7 naturalezas con afinidad almacenada. <b>Coinciden exactamente</b> con las que exige
     * {@code predicates/has_all_natures} (las 5 basicas + Yin + Yang: el predicate <b>no</b> pide Yin-Yang), asi
     * que este mismo conjunto sirve para el boost {@code d.all_natures} (+150 ninja_xp). La coincidencia es
     * consistente: Yin-Yang queda fuera de ambos por ser derivada.
     */
    public static Set<NatureType> withAffinity() {
        return WITH_AFFINITY;
    }

    /** Familia de la naturaleza; determina si almacena afinidad y si cuenta para los boosts. */
    public enum Category {
        /** Elemental basica (fuego, viento, rayo, tierra, agua). */
        BASIC,
        /** Avanzada con afinidad propia (yin, yang). */
        ADVANCED,
        /** Combinada: su afinidad se deriva de otras (yin-yang). */
        COMBINED
    }
}
