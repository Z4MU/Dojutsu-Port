package com.zamu.dojutsu.domain.progression;

/**
 * Curva de experiencia por nivel de afinidad/habilidad {@code [S13]}: reemplaza los ~600 predicates
 * {@code lvl_req/*} + {@code xp/*} del datapack por una funcion {@code xpForLevel(level)}.
 *
 * <p>La XP necesaria para pasar de {@code level} a {@code level+1} depende de la banda del nivel actual
 * (curva creciente por tramos, cap {@link #MAX_LEVEL}). Tabla fiel a {@code [S13]}:
 * <pre>
 *   nivel     0-10  11-20  21-40  41-80  81-90  91-99  100-299  300-499  500(cap)
 *   XP/subir   10    15     20     30     50     60      80       100      120
 * </pre>
 *
 * <p>Dominio puro (RA-4: numeros exactos verificados por tests). Clase de utilidad no instanciable.
 */
public final class XpCurve {

    /** Nivel maximo por afinidad/habilidad (cap de diseno del datapack). */
    public static final int MAX_LEVEL = 500;

    private XpCurve() {
    }

    /**
     * XP necesaria para avanzar de {@code level} a {@code level+1}, segun la banda de {@code [S13]}.
     *
     * @param level nivel actual, en {@code [0, MAX_LEVEL]}
     * @throws IllegalArgumentException si {@code level} esta fuera de {@code [0, MAX_LEVEL]}
     */
    public static int xpForLevel(int level) {
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("level fuera de [0, " + MAX_LEVEL + "]: " + level);
        }
        if (level <= 10) {
            return 10;
        }
        if (level <= 20) {
            return 15;
        }
        if (level <= 40) {
            return 20;
        }
        if (level <= 80) {
            return 30;
        }
        if (level <= 90) {
            return 50;
        }
        if (level <= 99) {
            return 60;
        }
        if (level <= 299) {
            return 80;
        }
        if (level <= 499) {
            return 100;
        }
        return 120; // level == MAX_LEVEL (banda final)
    }
}
