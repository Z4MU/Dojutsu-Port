package com.zamu.dojutsu.domain.throwing;

/**
 * Reglas del <b>Vuelo del Dios del Trueno</b> {@code [S16]} (F8/DJT-180): la tecnica de Minato. Dominio puro y
 * verificado contra {@code jutsu/flying_thunder_god/{teleport,reset,xp}} y {@code throwing/main}.
 *
 * <h2>Una sola marca, y de un solo uso</h2>
 * La ficha listaba "multiples marcas (seleccion)" como caso borde. <b>No existe</b>: {@code reset} mata el kunai
 * anterior en cuanto lanzas otro, asi que hay <b>exactamente una marca por jugador</b>. Y teletransportarse la
 * <b>consume</b> ({@code kill @s} tras el {@code tp}). Eso cambia por completo como se juega: la marca no es una
 * red de puntos de viaje, es <b>una salida de emergencia</b> que preparas y gastas.
 *
 * <h2>Dominarlo lo abarata cinco veces</h2>
 * El coste baja con {@code d.ftg_lvl}: {@value #CHAKRA_NOVICE} de chakra hasta el nivel 10,
 * {@value #CHAKRA_ADEPT} hasta el 20 y {@value #CHAKRA_MASTER} de ahi en adelante. Se sube con
 * {@value #XP_PER_LEVEL} usos por nivel (cap {@value #MAX_LEVEL}), y el <b>clan Namikaze</b> —el de Minato— gana
 * el <b>doble</b> por uso.
 *
 * <h2>Y no te deja donde clavaste el kunai</h2>
 * Apareces {@value #TELEPORT_Y_OFFSET} bloques <b>por encima</b> ({@code tp @a ~ ~3 ~}), no en el punto exacto:
 * si clavaste el kunai en una pared o dentro de un hueco, sales arriba y no incrustado.
 *
 * <p>Requisito: fuinjutsu {@value #REQUIRED_FUINJUTSU} en escala del mod (datapack 1000 ÷ 4).
 */
public final class FtgTable {

    /** Fuinjutsu para teletransportarse, en escala del mod ({@code teleport}: datapack 1000 ÷ 4). */
    public static final int REQUIRED_FUINJUTSU = 250;

    /** Chakra por salto hasta el nivel 10 de la tecnica. */
    public static final int CHAKRA_NOVICE = 50;

    /** Chakra por salto entre los niveles 11 y 20. */
    public static final int CHAKRA_ADEPT = 25;

    /** Chakra por salto desde el nivel 21: cinco veces mas barato que empezando. */
    public static final int CHAKRA_MASTER = 10;

    /** Ultimo nivel con el coste de novato. */
    public static final int NOVICE_MAX_LEVEL = 10;

    /** Ultimo nivel con el coste intermedio. */
    public static final int ADEPT_MAX_LEVEL = 20;

    /** Nivel maximo de la tecnica ({@code xp}: {@code unless ftg_lvl matches 30..}). */
    public static final int MAX_LEVEL = 30;

    /** Usos necesarios para subir un nivel ({@code xp}: {@code ftg_xp matches 10..}). */
    public static final int XP_PER_LEVEL = 10;

    /** XP de tecnica por salto. */
    public static final int XP_PER_USE = 1;

    /** XP por salto siendo del clan Namikaze — el clan de Minato. */
    public static final int XP_PER_USE_NAMIKAZE = 2;

    /** Id del clan que aprende el doble de rapido. */
    public static final String NAMIKAZE_CLAN = "namikaze";

    /** Bloques por encima de la marca donde apareces ({@code tp @a ~ ~3 ~}). */
    public static final int TELEPORT_Y_OFFSET = 3;

    /** Ticks de veto tras un salto ({@code d.ftg_cd = 20}). */
    public static final int COOLDOWN_TICKS = 20;

    /** Ticks de veto tras un intento fallido ({@code d.ftg_cd = 40}): fallar cuesta el doble. */
    public static final int FAILED_COOLDOWN_TICKS = 40;

    private FtgTable() {
    }

    /** Chakra que cuesta un salto con ese nivel de tecnica. */
    public static int chakraCost(int level) {
        if (level > ADEPT_MAX_LEVEL) {
            return CHAKRA_MASTER;
        }
        if (level > NOVICE_MAX_LEVEL) {
            return CHAKRA_ADEPT;
        }
        return CHAKRA_NOVICE;
    }

    /** XP de tecnica que gana un salto, segun el clan. */
    public static int xpPerUse(String clanId) {
        return NAMIKAZE_CLAN.equals(clanId) ? XP_PER_USE_NAMIKAZE : XP_PER_USE;
    }

    /** Indica si el fuinjutsu alcanza para teletransportarse. */
    public static boolean canTeleport(int fuinjutsu) {
        return fuinjutsu >= REQUIRED_FUINJUTSU;
    }

    /** Indica si con esa XP acumulada toca subir de nivel (y no se ha alcanzado el cap). */
    public static boolean levelsUp(int level, int xp) {
        return level < MAX_LEVEL && xp >= XP_PER_LEVEL;
    }
}
