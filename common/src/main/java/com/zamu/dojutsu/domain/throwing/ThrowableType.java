package com.zamu.dojutsu.domain.throwing;

import java.util.Locale;
import java.util.Optional;

/**
 * Las cinco <b>armas arrojadizas</b> {@code [S16]} (F8/DJT-180). Verificadas en {@code throwing/throw/*}
 * (velocidad, daño y veto), {@code throwing/move} (impacto) y {@code throwing/drop} (recuperacion).
 *
 * <h2>Dos velocidades y dos daños distintos</h2>
 * Los kunai vuelan a {@value #KUNAI_SPEED} bloques/tick y todo lo demas a {@value #FAST_SPEED} — el shuriken es
 * literalmente mas rapido que el kunai. Y hay <b>dos</b> daños que no hay que confundir: el del atributo
 * ({@link #meleeDamage}, para cuando lo llevas en la mano) y el del impacto en vuelo, que es
 * {@code instant_damage} amplificador 0 e <b>igual para todos</b> ({@link ThrowingTable#THROWN_DAMAGE}). Un fuma
 * shuriken no hace mas daño lanzado que un shuriken normal; pega mas fuerte si lo blandes.
 *
 * <h2>El kunai del Dios del Trueno no es un arma</h2>
 * No lleva atributo de daño, no se recupera y <b>no caduca</b>: es una <b>marca</b>. Todo lo demas se clava,
 * espera {@value ThrowingTable#STUCK_LIFETIME_TICKS} ticks y cae al suelo como item recuperable.
 *
 * @param speed         bloques por tick de vuelo
 * @param throwCooldown ticks de veto tras lanzar ({@code schedule end})
 * @param meleeDamage   daño del atributo al llevarlo en la mano; {@code 0} si no lo lleva
 * @param explodes      detona al clavarse (kunai con sello explosivo)
 * @param mark          es una marca del Vuelo del Dios del Trueno, no un arma
 */
public enum ThrowableType {

    /** Kunai: el mas lento, pero el que se puede lanzar mas seguido. */
    KUNAI(0.5F, 10, 5, false, false),
    /** Shuriken: mas rapido que el kunai y menos daño en mano. */
    SHURIKEN(0.8F, 20, 3, false, false),
    /** Fuma shuriken: el gigante. Pega mas en mano, lo mismo lanzado. */
    FUMA_SHURIKEN(0.8F, 20, 4, false, false),
    /** Kunai con sello explosivo: se clava y detona. */
    EXPLOSIVE_KUNAI(0.5F, 20, 5, true, false),
    /** Kunai del Dios del Trueno: no es arma, es una marca a la que teletransportarse. */
    FTG_KUNAI(0.8F, 20, 0, false, true);

    /** Velocidad de los kunai ({@code move}: {@code ^ ^ ^.5}). */
    public static final float KUNAI_SPEED = 0.5F;

    /** Velocidad de todo lo demas ({@code move}: {@code ^ ^ ^.8}). */
    public static final float FAST_SPEED = 0.8F;

    private final float speed;
    private final int throwCooldown;
    private final int meleeDamage;
    private final boolean explodes;
    private final boolean mark;

    ThrowableType(float speed, int throwCooldown, int meleeDamage, boolean explodes, boolean mark) {
        this.speed = speed;
        this.throwCooldown = throwCooldown;
        this.meleeDamage = meleeDamage;
        this.explodes = explodes;
        this.mark = mark;
    }

    /** Id data-driven en minusculas ({@code "kunai"}, {@code "ftg_kunai"}). */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Bloques por tick de vuelo. */
    public float speed() {
        return speed;
    }

    /** Ticks de veto tras lanzar: el kunai se puede encadenar el doble de rapido. */
    public int throwCooldown() {
        return throwCooldown;
    }

    /** Daño del atributo al llevarlo en la mano ({@code 0} = no lo lleva). */
    public int meleeDamage() {
        return meleeDamage;
    }

    /** Indica si detona al clavarse. */
    public boolean explodes() {
        return explodes;
    }

    /** Indica si es una marca del Vuelo del Dios del Trueno en vez de un arma. */
    public boolean isMark() {
        return mark;
    }

    /**
     * Indica si al clavarse se puede recoger del suelo. Las marcas no ({@code drop} solo suelta kunai, shuriken y
     * fuma) y lo que detona tampoco (ya no existe).
     */
    public boolean recoverable() {
        return !mark && !explodes;
    }

    /** Resuelve un arrojadizo por su id; vacio si no existe (estado tolerante). */
    public static Optional<ThrowableType> byId(String id) {
        for (ThrowableType type : values()) {
            if (type.id().equals(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
