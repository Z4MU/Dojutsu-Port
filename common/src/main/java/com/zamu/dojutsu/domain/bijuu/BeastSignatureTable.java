package com.zamu.dojutsu.domain.bijuu;

/**
 * Constantes de las <b>técnicas insignia</b> de las nueve primeras Bestias con Cola {@code [S09]}
 * (F10/DJT-226). Verificadas en {@code tailed_beasts/ai/moves/<tecnica>/{summon,move,run}.mcfunction},
 * {@code tailed_beasts/main.mcfunction} (lineas 60-100) y {@code jinchuriki/form/one_tail/sand_buckshot/cast}.
 *
 * <h2>Las nueve son la misma técnica</h2>
 * Es el hallazgo del ticket y conviene decirlo sin rodeos: {@code sand_buckshot}, {@code cat_flame_roaring_fire},
 * {@code great_water_mass_bullet}, {@code monkey_flame_arson}, {@code unrivalled_strength},
 * {@code soap_bubble_barrage}, {@code scale_powder}, {@code tentacle_barrage} y {@code chakra_arms_barrage}
 * tienen, <b>valor a valor</b>, la misma velocidad, el mismo alcance, el mismo radio y el mismo daño. Lo único
 * que cambia entre ellas son las <b>partículas</b> (y un sonido de chapoteo en la del Tres Colas).
 *
 * <p>Por eso aquí hay <b>una</b> tabla y <b>una</b> clase de técnica, no nueve: es exactamente el caso que
 * describe RA-1 del ADD («una clase por familia, nunca por técnica»). Portarlas como nueve implementaciones
 * habría multiplicado por nueve la superficie de mantenimiento de un único comportamiento.
 *
 * <h2>El temporizador no es un enfriamiento: es el alcance</h2>
 * El {@code d.<N>t_<xx>timer} que fija {@code summon} vive en la <b>bestia</b>, no en el proyectil, y cuando
 * llega a 0 el barrido de {@code tailed_beasts/main} le pone {@code d.hit} al proyectil — o sea que lo
 * <b>detona</b>. Es la vida máxima del disparo. Y como el proyectil no arranca hasta el cuarto tick (la cuenta
 * atrás {@code d.hit1/2/3}, igual que la Bola Bestia con Cola), el alcance real es
 * {@code (vida - carga) × velocidad} ≈ <b>8,4 bloques</b>: son técnicas de corta distancia, coherentes con
 * llamarse «perdigonada» y «andanada».
 *
 * <p><b>El Nueve Colas rompe el patrón</b>, como ya lo rompía en la XP de sellado (DJT-165): su
 * {@code d.9t_cabtimer} vale <b>20</b> y no 10, así que su andanada llega el doble de lejos que la de sus ocho
 * hermanas.
 */
public final class BeastSignatureTable {

    /** Vida del proyectil en ticks para ocho de las nueve ({@code d.<N>t_<xx>timer = 10}). */
    public static final int FLIGHT_TICKS = 10;

    /** Vida del proyectil del Nueve Colas ({@code d.9t_cabtimer = 20}): el doble que las demás. */
    public static final int FLIGHT_TICKS_NINE_TAILS = 20;

    /** Ticks quieto antes de salir disparado (la cuenta atrás {@code d.hit1/2/3}). */
    public static final int CHARGE_TICKS = 3;

    /** Bloques por tick: doce pasos de 0,1 en {@code move.mcfunction}. */
    public static final float SPEED_BLOCKS_PER_TICK = 1.2F;

    /** Radio del daño al impactar ({@code @e[distance=0..5]}). */
    public static final double BLAST_RADIUS = 5.0D;

    /** Amplificador del {@code instant_damage} al impactar (Daño II = 12 puntos). */
    public static final int DAMAGE_AMPLIFIER = 1;

    /** Chakra que cuesta a un <b>jinchuriki</b> ({@code form/one_tail/sand_buckshot/cast}: {@code chakra >= 40}). */
    public static final int CHAKRA_COST = 40;

    /** Ticks de reutilización para el jinchuriki ({@code d.jinform_timer = 100}). */
    public static final int COOLDOWN_TICKS = 100;

    private BeastSignatureTable() {
    }

    /** Vida del proyectil de esa bestia: {@value #FLIGHT_TICKS}, salvo el Nueve Colas. */
    public static int flightTicks(TailedBeast beast) {
        return beast == TailedBeast.NINE_TAILS ? FLIGHT_TICKS_NINE_TAILS : FLIGHT_TICKS;
    }

    /**
     * Alcance efectivo en bloques: la vida menos la carga, por la velocidad. No es un número del datapack sino
     * su consecuencia — se expone porque es lo que de verdad define a estas técnicas.
     */
    public static double rangeBlocks(TailedBeast beast) {
        return (flightTicks(beast) - CHARGE_TICKS) * SPEED_BLOCKS_PER_TICK;
    }

    /**
     * Indica si esa bestia tiene técnica insignia de esta familia. El Diez Colas <b>no</b>: su
     * {@code tenpenchii} es un área de rayos, no un proyectil.
     */
    public static boolean hasBarrage(TailedBeast beast) {
        return !beast.isTenTails();
    }
}
