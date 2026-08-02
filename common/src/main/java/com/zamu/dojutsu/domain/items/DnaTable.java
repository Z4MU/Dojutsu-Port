package com.zamu.dojutsu.domain.items;

import java.util.random.RandomGenerator;

/**
 * Reglas del <b>ADN</b> {@code [S14]} (F8/DJT-170): identificarlo, implantarlo y las dos veces que el azar no
 * decide. Dominio puro y verificado contra {@code items/dna/identify}, {@code get_id}, {@code implant} e
 * {@code implant_dna}.
 *
 * <h2>Implantar sale mal dos de cada tres veces</h2>
 * {@code implant_dna} tira {@code dna_rng} (1..3) y <b>solo el 1 acierta</b>: {@value #SUCCESS_CHANCE_PERCENT}%.
 * Fallar no mata pero deja mal cuerpo —nausea, debilidad y veneno—, asi que un ADN raro se puede perder tres
 * veces seguidas. Es el coste de jugar a ser Orochimaru.
 *
 * <h2>Dos excepciones donde el azar no manda</h2>
 * El datapack fuerza el exito en dos casos, y los dos cuentan la misma historia: <b>tu cuerpo ya acepto ese
 * linaje antes</b>.
 * <ul>
 *   <li>Implantar <b>Madera</b> teniendo ya <b>celulas de Hashirama</b>.</li>
 *   <li>Implantar <b>celulas de Hashirama</b> siendo del clan <b>Senju</b> — el clan de Hashirama.</li>
 * </ul>
 *
 * <h2>Lo que ya tienes no se te quita ni se te cobra</h2>
 * Si el ADN concede algo que ya posees, {@code idna_alr} anula <b>las dos</b> etiquetas (ni exito ni fallo) y
 * marca {@code d.dont_take}: el item <b>no se consume</b>. No es un fallo enmascarado, es un no-intento.
 */
public final class DnaTable {

    /** Ninjutsu Medico para <b>identificar</b> ADN, en escala del mod ({@code identify}: datapack 250 ÷ 4). */
    public static final int IDENTIFY_MEDICAL = 63;

    /** Ninjutsu Medico para <b>implantar</b> ({@code implant}: datapack 300 ÷ 4). */
    public static final int IMPLANT_MEDICAL = 75;

    /** Probabilidad de que el implante prenda ({@code dna_rng} 1..3, exito solo con el 1). */
    public static final int SUCCESS_CHANCE_PERCENT = 33;

    /** Caras del dado de implante ({@code dna_rng}: {@code rolls 1..3}). */
    public static final int IMPLANT_ROLL_FACES = 3;

    /** Tirada ganadora del implante. */
    public static final int IMPLANT_WINNING_ROLL = 1;

    /** Ticks de veto entre usos del ADN ({@code d.dna_delay = 10}). */
    public static final int USE_DELAY_TICKS = 10;

    /** Id del clan cuyo linaje acepta las celulas de Hashirama sin tirar dados. */
    public static final String SENJU_CLAN = "senju";

    /** Que ha pasado al intentar implantar. */
    public enum ImplantOutcome {
        /** Prendio: concede su naturaleza, KG o el flag de Hashirama. */
        SUCCESS,
        /** Fallo la tirada: nausea, debilidad y veneno, y el ADN se pierde. */
        FAILURE,
        /** Ya lo tenias: ni exito ni fallo, y <b>el ADN no se consume</b> ({@code idna_alr}). */
        ALREADY_OWNED,
        /** Ninjutsu Medico insuficiente. */
        LOW_MEDICAL
    }

    private DnaTable() {
    }

    /** Tira el dado del implante ({@code dna_rng}): {@code true} si prende. Azar inyectado (testeable). */
    public static boolean rollImplant(RandomGenerator random) {
        return random.nextInt(IMPLANT_ROLL_FACES) + 1 == IMPLANT_WINNING_ROLL;
    }

    /**
     * Indica si este implante <b>no tira dados</b> porque el cuerpo ya acepto ese linaje.
     *
     * @param type          ADN que se implanta
     * @param hasHashirama  ya tiene celulas de Hashirama
     * @param clanId        id del clan del ninja, o {@code null}
     */
    public static boolean isGuaranteed(DnaType type, boolean hasHashirama, String clanId) {
        if (type == DnaType.WOOD && hasHashirama) {
            return true; // ya llevas dentro el linaje que produce la Madera
        }
        return type.isHashiramaCells() && SENJU_CLAN.equals(clanId); // eres del clan de Hashirama
    }

    /** Indica si el nivel de medicina alcanza para identificar ADN. */
    public static boolean canIdentify(int medicalLevel) {
        return medicalLevel >= IDENTIFY_MEDICAL;
    }

    /** Indica si el nivel de medicina alcanza para implantar. */
    public static boolean canImplant(int medicalLevel) {
        return medicalLevel >= IMPLANT_MEDICAL;
    }

    /**
     * Decide (puro) el resultado de un intento de implante, sin tocar el mundo.
     *
     * @param type          ADN que se implanta
     * @param medicalLevel  Ninjutsu Medico del ninja
     * @param alreadyOwned  ya posee lo que ese ADN concede
     * @param hasHashirama  ya tiene celulas de Hashirama
     * @param clanId        id del clan, o {@code null}
     * @param random        azar del dado
     */
    public static ImplantOutcome attemptImplant(DnaType type, int medicalLevel, boolean alreadyOwned,
            boolean hasHashirama, String clanId, RandomGenerator random) {
        if (!canImplant(medicalLevel)) {
            return ImplantOutcome.LOW_MEDICAL;
        }
        if (alreadyOwned) {
            return ImplantOutcome.ALREADY_OWNED; // se comprueba ANTES del dado: no gasta el intento ni el item
        }
        if (isGuaranteed(type, hasHashirama, clanId) || rollImplant(random)) {
            return ImplantOutcome.SUCCESS;
        }
        return ImplantOutcome.FAILURE;
    }

    /** Elige al azar que resulta ser un ADN sin identificar ({@code dna_id_rng} 1..26). */
    public static DnaType rollIdentification(RandomGenerator random) {
        DnaType[] all = DnaType.values();
        return all[random.nextInt(all.length)];
    }
}
