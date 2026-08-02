package com.zamu.dojutsu.domain.cursedseal;

import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Tabla del <b>sello maldito</b> {@code [S10]} (F6/DJT-140): la inyeccion probabilistica, el coste de sostenerlo
 * y la degradacion. Dominio puro y <b>verificada contra el datapack</b> ({@code cursed_seal/inject},
 * {@code success}, {@code cost}, {@code punish}).
 *
 * <h2>Inyectarse el sello es una apuesta</h2>
 * El {@code inject} tira {@code 1..100} y solo <b>≤60 tiene exito</b> ({@value #SUCCESS_CHANCE_PERCENT}%). Fallar
 * <b>no mata</b> (daño + veneno + hambre), pero jugartela con poca vida si puede acabar contigo.
 *
 * <h2>La muerte llega despues, por quedarte sin chakra</h2>
 * El castigo real ({@code punish}) es sostener el sello <b>hasta agotar el chakra</b>: te <b>mata</b>. Y en nivel
 * 2 cada muerte asi cuenta: a las {@value #DEATHS_TO_DEGRADE} el sello <b>degrada</b> a nivel 1.
 *
 * <h2>Madurar lo abarata</h2>
 * Sostenerlo cuesta {@value #CHAKRA_COST_LEVEL_1} de chakra por pulso en nivel 1 y solo
 * {@value #CHAKRA_COST_LEVEL_2} en nivel 2, cada {@value #DRAIN_INTERVAL_TICKS} ticks.
 */
public final class CursedSealTable {

    /** Probabilidad de que la inyeccion tenga exito ({@code inject}: {@code matches 0..60} de 1..100). */
    public static final int SUCCESS_CHANCE_PERCENT = 60;

    /** Nivel maximo del sello. */
    public static final int MAX_LEVEL = 2;

    /** Chakra por pulso sosteniendo el sello en nivel 1 ({@code cost}). */
    public static final int CHAKRA_COST_LEVEL_1 = 12;

    /** Chakra por pulso en nivel 2: madurar lo <b>abarata</b> ({@code cost}). */
    public static final int CHAKRA_COST_LEVEL_2 = 8;

    /** Ticks entre cobros ({@code seal_timer}: {@code d.cseal_timer >= 40}). */
    public static final int DRAIN_INTERVAL_TICKS = 40;

    /** Muertes por agotamiento en nivel 2 que degradan el sello ({@code punish}). */
    public static final int DEATHS_TO_DEGRADE = 10;

    // --- Recompensas de una inyeccion exitosa (success) ---

    /** {@code max_chakra += 300} al recibir el sello. */
    public static final int SUCCESS_MAX_CHAKRA = 300;

    /** {@code ninja_xp += 50} al recibir el sello. */
    public static final int SUCCESS_NINJA_XP = 50;

    /** {@code senjutsu += 50} al recibir el sello. */
    public static final int SUCCESS_SENJUTSU = 50;

    /** {@code ninja_xp += 100} al madurar a nivel 2 ({@code level_up}). */
    public static final int LEVEL_UP_NINJA_XP = 100;

    private CursedSealTable() {
    }

    /**
     * Tira la inyeccion: {@code true} si el sello prende. Azar <b>inyectado</b> (testeable con semilla), como el
     * resto del proyecto.
     */
    public static boolean rollInjection(RandomGenerator random) {
        return random.nextInt(100) + 1 <= SUCCESS_CHANCE_PERCENT; // 1..100, exito <= 60
    }

    /** Elige al azar cual de los seis sellos prende ({@code cursed_seal_rng} 1..6). */
    public static CursedSealType rollSealType(RandomGenerator random) {
        CursedSealType[] all = CursedSealType.values();
        return all[random.nextInt(all.length)];
    }

    /** Chakra que cuesta cada pulso de mantenimiento en ese nivel. */
    public static int chakraCost(int level) {
        return level >= MAX_LEVEL ? CHAKRA_COST_LEVEL_2 : CHAKRA_COST_LEVEL_1;
    }

    /**
     * Decide si una muerte por agotamiento <b>degrada</b> el sello: solo en nivel 2 y al alcanzar
     * {@value #DEATHS_TO_DEGRADE} muertes acumuladas. Decision pura.
     *
     * @param level  nivel del sello al morir
     * @param deaths muertes acumuladas <b>incluyendo</b> esta
     */
    public static boolean degradesOnDeath(int level, int deaths) {
        return level >= MAX_LEVEL && deaths >= DEATHS_TO_DEGRADE;
    }

    /** El sello que prende en una inyeccion, o vacio si fallo. Combina las dos tiradas del datapack. */
    public static Optional<CursedSealType> inject(RandomGenerator random) {
        return rollInjection(random) ? Optional.of(rollSealType(random)) : Optional.empty();
    }
}
