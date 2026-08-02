package com.zamu.dojutsu.domain.throwing;

/**
 * Numeros de las armas arrojadizas {@code [S16]} (F8/DJT-180). Dominio puro, verificado contra
 * {@code throwing/move}, {@code hit_end} y {@code drop}.
 */
public final class ThrowingTable {

    /**
     * Amplificador del {@code instant_damage} al impactar en vuelo ({@code move}). <b>Es el mismo para las cinco
     * armas</b>: 6 puntos de daño, tres corazones. Los atributos 5/3/4 de {@code ThrowableType} son para
     * blandirlas en la mano, no para lanzarlas.
     */
    public static final int THROWN_DAMAGE_AMPLIFIER = 0;

    /** Daño real que hace el amplificador 0 (para el comando de QA y los tests). */
    public static final int THROWN_DAMAGE = 6;

    /** Radio de impacto en vuelo ({@code move}: {@code distance=0..3}). */
    public static final double HIT_RADIUS = 3.0;

    /** Ticks que un arma clavada espera antes de caer al suelo ({@code d.throwtimer = 120}). */
    public static final int STUCK_LIFETIME_TICKS = 120;

    /** Ticks antes de que un mismo objetivo pueda volver a ser golpeado ({@code schedule hit_end 15t}). */
    public static final int HIT_COOLDOWN_TICKS = 15;

    /** Potencia de la explosion del kunai con sello ({@code move}: un tnt_minecart). */
    public static final float EXPLOSIVE_POWER = 4.0F;

    private ThrowingTable() {
    }
}
