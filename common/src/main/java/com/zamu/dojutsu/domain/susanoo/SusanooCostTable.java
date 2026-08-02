package com.zamu.dojutsu.domain.susanoo;

import java.util.List;

/**
 * Tabla de costes y bonificaciones del Susanoo {@code [S24]}, niveles 1..6 (DJT-120). Dominio puro y
 * <b>verificada contra el datapack</b> ({@code sharingan/mangekyo/susanoo/main.mcfunction} para el coste,
 * {@code effects.mcfunction} para los efectos, {@code level/set_N} para nombre y XP).
 *
 * <h2>La curva va al reves de lo esperable</h2>
 * Sostener el Susanoo es <b>mas barato cuanto mayor es el nivel</b>: 6 chakra/20t en Ribcage frente a 1
 * chakra/80t en Perfect — el pulso se espacia y el importe baja. Es la maestria del portador, no un error de
 * lectura. La proteccion, en cambio, sube con el nivel ({@code resistance}/{@code fire_resistance} amp
 * {@code level-1}).
 *
 * <h2>El ojo eterno (EMS) casi no lo paga</h2>
 * Con {@code wearing_eternal} el datapack <b>salta todo el bloque de coste por nivel</b> y aplica una sola
 * linea: 0 chakra y +1 de fatiga cada 160 ticks, sea cual sea el nivel. Es el mismo privilegio del EMS que ya
 * exime de fatiga en las habilidades ({@code sasuke/cost}). Los <b>efectos</b> siguen siendo los del nivel.
 */
public final class SusanooCostTable {

    /** Nivel maximo alcanzable ({@code level/set_6}, "Perfect"). */
    public static final int MAX_LEVEL = 6;

    /** Chakra minimo para poder activarlo ({@code activate}: {@code d.chakra matches 5..}); es puerta, no cobro. */
    public static final int ACTIVATION_CHAKRA_GATE = 5;

    /** Cooldown entre activaciones/avances ({@code scoreboard players set @s d.susanoo_cd 14}). */
    public static final int ACTIVATION_COOLDOWN_TICKS = 14;

    /** Uso acumulado que sube de nivel ({@code susanoo_lvl_time}: {@code d.susanoo_use >= 32000}). */
    public static final int USE_PER_LEVEL = 32000;

    /** Coste del portador de un ojo eterno: sin chakra, +1 de fatiga cada 160 ticks (mismo a cualquier nivel). */
    public static final int ETERNAL_CHAKRA_PER_PULSE = 0;
    public static final int ETERNAL_STRAIN_PER_PULSE = 1;
    public static final int ETERNAL_PULSE_TICKS = 160;

    /** Amplificador de {@code slowness} de los niveles que lastran (3 y 4). */
    public static final int SLOWNESS_AMPLIFIER = 1;

    private static final List<SusanooTier> TIERS = List.of(
            // nivel, nombre,       chakra, strain, pulso, resist, fuego, lastra, xp
            new SusanooTier(1, "Ribcage",   6, 3, 20, 0, 0, false, 10),
            new SusanooTier(2, "Skeletal",  4, 2, 20, 1, 1, false, 12),
            new SusanooTier(3, "Armored",   2, 1, 40, 2, 2, true,  13),
            new SusanooTier(4, "Humanoid",  1, 1, 50, 3, 3, true,  14),
            new SusanooTier(5, "Full-Body", 1, 1, 60, 4, 4, false, 15),
            new SusanooTier(6, "Perfect",   1, 1, 80, 5, 5, false, 16));

    private SusanooCostTable() {
    }

    /** Fila de un nivel. El nivel se <b>clampa</b> a {@code [1, MAX_LEVEL]} (estado corrupto no rompe el tick). */
    public static SusanooTier tier(int level) {
        int clamped = Math.min(Math.max(level, 1), MAX_LEVEL);
        return TIERS.get(clamped - 1);
    }

    /** Todas las filas, de nivel 1 a 6. */
    public static List<SusanooTier> tiers() {
        return TIERS;
    }

    /** Chakra por pulso que paga realmente un portador: 0 si su ojo es eterno. */
    public static int chakraPerPulse(int level, boolean eternal) {
        return eternal ? ETERNAL_CHAKRA_PER_PULSE : tier(level).chakraPerPulse();
    }

    /** Fatiga por pulso que paga realmente un portador: 1 fija si su ojo es eterno. */
    public static int strainPerPulse(int level, boolean eternal) {
        return eternal ? ETERNAL_STRAIN_PER_PULSE : tier(level).strainPerPulse();
    }

    /** Cada cuantos ticks se cobra a un portador: 160 fijos si su ojo es eterno. */
    public static int pulseTicks(int level, boolean eternal) {
        return eternal ? ETERNAL_PULSE_TICKS : tier(level).pulseTicks();
    }

    /**
     * Nivel resultante de acumular uso: sube uno por cada {@link #USE_PER_LEVEL}, con tope en
     * {@link #MAX_LEVEL}. Puro.
     *
     * @return el nivel nuevo, o el mismo si aun no toca subir
     */
    public static int levelAfterUse(int level, int use) {
        if (level >= MAX_LEVEL || use < USE_PER_LEVEL) {
            return level;
        }
        return level + 1;
    }
}
