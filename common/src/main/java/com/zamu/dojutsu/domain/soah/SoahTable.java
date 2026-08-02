package com.zamu.dojutsu.domain.soah;

/**
 * Tabla del <b>Sello de la Fuerza de un Centenar</b> {@code [S12]} (F6/DJT-150): la banca de chakra de Tsunade.
 * Dominio puro y <b>verificada contra el datapack</b> ({@code soah_seal/toggle}, {@code recharge}, {@code run},
 * {@code heal*}, {@code activate}, {@code deactivate}, {@code unlock}).
 *
 * <h2>Es una hucha, no una carga pasiva</h2>
 * La ficha de {@code [S12]} habla de "carga lenta" y de "pulsos 200/400". <b>No existe ninguna de las dos
 * cosas</b> en el datapack: el sello se llena de un solo golpe, a mano, agachandote al activarlo
 * ({@code recharge}), y la curacion ocurre <b>cada tick</b>, no por pulsos. El unico origen de chakra
 * almacenado en todo el datapack es esa transferencia de {@value #RECHARGE_AMOUNT}.
 *
 * <h2>Guardas chakra hoy para no morirte manana</h2>
 * Con el sello activo, en cuanto tu chakra baja de {@value #HEAL_TRIGGER_CHAKRA} el sello empieza a
 * devolvertelo <b>de su reserva</b> y no para hasta dejarte <b>al maximo</b>. Lo devuelve en tramos
 * ({@link #healTier}): de {@value #HEAL_TIER_LARGE} en {@value #HEAL_TIER_LARGE} mientras le quede mucho, luego
 * de {@value #HEAL_TIER_MEDIUM}, y al final gota a gota. Cuando se vacia, el sello se apaga solo.
 *
 * <h2>Apagarlo cuesta caro</h2>
 * Encenderlo solo veta {@value #ACTIVATE_COOLDOWN_TICKS} ticks; <b>apagarlo</b> veta
 * {@value #DEACTIVATE_COOLDOWN_TICKS} (medio minuto). No es un interruptor que puedas ir dando.
 */
public final class SoahTable {

    /** Nivel de Ninjutsu Medico que otorga el sello, en escala del mod ({@code soah_req}: datapack 1500). */
    public static final int UNLOCK_MEDICAL = 375;

    /** {@code ninja_xp += 100} al recibir el sello ({@code unlock}). */
    public static final int UNLOCK_NINJA_XP = 100;

    /** Afinidad Yin minima para usarlo ({@code toggle}: {@code aff_yin >= 150}; la afinidad no se reescala). */
    public static final int REQUIRED_YIN_AFFINITY = 150;

    /** Chakra <b>maximo</b> minimo para usarlo ({@code toggle}: {@code max_chakra >= 20000}). */
    public static final int REQUIRED_MAX_CHAKRA = 20_000;

    /**
     * Chakra que se transfiere del ninja al sello en una recarga ({@code recharge}).
     *
     * <p><b>Divergencia documentada:</b> el datapack veta la recarga con {@code chakra >= 10000} pero gasta
     * 20000, lo que dejaria el chakra en negativo entre esas dos cifras. Aqui el requisito es el importe que se
     * paga de verdad —{@value #RECHARGE_AMOUNT}— porque el {@code Resource} del mod es todo-o-nada por diseno
     * (F1). El 10000 del datapack se lee como descuido.
     */
    public static final int RECHARGE_AMOUNT = 20_000;

    /** Chakra por debajo del cual el sello empieza a devolver reserva ({@code enough_chakra}). */
    public static final int HEAL_TRIGGER_CHAKRA = 5_000;

    /** Tramo grande de devolucion ({@code heal_100}). */
    public static final int HEAL_TIER_LARGE = 100;

    /** Tramo medio ({@code heal_10}). */
    public static final int HEAL_TIER_MEDIUM = 10;

    /** Tramo minimo, gota a gota ({@code heal_1}). */
    public static final int HEAL_TIER_SMALL = 1;

    /** Veto tras <b>encender</b> el sello ({@code activate}: {@code d.soah_cd = 40}). */
    public static final int ACTIVATE_COOLDOWN_TICKS = 40;

    /** Veto tras <b>apagarlo</b> ({@code deactivate}: {@code d.soah_cd = 600}). Quince veces mas largo. */
    public static final int DEACTIVATE_COOLDOWN_TICKS = 600;

    private SoahTable() {
    }

    /**
     * Cuanto chakra devuelve el sello este tick segun lo que le queda en reserva: {@value #HEAL_TIER_LARGE},
     * {@value #HEAL_TIER_MEDIUM}, {@value #HEAL_TIER_SMALL} o {@code 0} si esta vacio. Decision pura, fiel al
     * orden de {@code heal} (mira primero el tramo grande y solo aplica <b>uno</b> por tick).
     */
    public static int healTier(int storedChakra) {
        if (storedChakra >= HEAL_TIER_LARGE) {
            return HEAL_TIER_LARGE;
        }
        if (storedChakra >= HEAL_TIER_MEDIUM) {
            return HEAL_TIER_MEDIUM;
        }
        if (storedChakra >= HEAL_TIER_SMALL) {
            return HEAL_TIER_SMALL;
        }
        return 0;
    }

    /** Indica si el nivel de medicina alcanza para recibir el sello. */
    public static boolean canUnlock(int medicalLevel) {
        return medicalLevel >= UNLOCK_MEDICAL;
    }

    /** Indica si el chakra actual ha bajado lo suficiente para que el sello entre a curar. */
    public static boolean needsHealing(int chakra) {
        return chakra < HEAL_TRIGGER_CHAKRA;
    }
}
