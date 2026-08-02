package com.zamu.dojutsu.data.state;

/**
 * Habilidades gastables del ninja {@code [S13]}: las 7 categorias que se suben con skill points (y, la
 * mayoria, tambien por XP de uso). Cada una se modela como un {@code LeveledStat} en {@code ProgressionState}.
 *
 * <p>El flag {@link #autoLevels()} distingue las que <b>auto-nivelan por XP</b> (banda de {@code leveling/main})
 * de las que <b>solo suben por puntos</b>: {@code senjutsu} y {@code cc} aparecen como gastables pero sin banda
 * de auto-nivelacion propia (postura por defecto {@code [ADD/00 §3]}; confirmar contra el datapack).
 *
 * <p>Las <b>afinidades</b> (fuego, viento, rayo, agua, tierra, yin, yang) comparten la misma escala pero viven
 * en {@code NatureProfile} (F3), no aqui; se nivelan con la misma curva ({@code XpCurve}).
 */
public enum Skill {

    TAIJUTSU(true),
    GENJUTSU(true),
    MEDICAL(true),
    FUINJUTSU(true),
    KENJUTSU(true),
    /** Senjutsu (modo sabio): gastable por puntos, sin auto-nivelacion por XP. */
    SENJUTSU(false),
    /** Control de chakra (chakra control): gastable por puntos, sin auto-nivelacion por XP. */
    CC(false);

    private final boolean autoLevels;

    Skill(boolean autoLevels) {
        this.autoLevels = autoLevels;
    }

    /** Indica si la habilidad sube automaticamente al acumular XP de uso (true) o solo por puntos (false). */
    public boolean autoLevels() {
        return autoLevels;
    }
}
