package com.zamu.dojutsu.domain.resource;

/**
 * Factores que aceleran la regeneracion de chakra en el datapack {@code [S02]}.
 *
 * <p>Son las entradas "acumulables" que {@link RegenPolicy} traduce a una celda de la tabla de regen
 * (umbral + incremento). Se agrupan en dos dimensiones ortogonales:
 * <ul>
 *   <li><b>Clase de clan/DNA</b> (el {@code d.diff_cr} del datapack): {@link #CLAN_OTSUTSUKI},
 *       {@link #CLAN_UZUMAKI}, {@link #HASHIRAMA_DNA}.</li>
 *   <li><b>Modo sabio</b>: {@link #SAGE_MODE_1} (normal) y {@link #SAGE_MODE_2} (Seis Caminos).</li>
 * </ul>
 *
 * <p>La derivacion de estos factores desde el estado real del jugador (clan, ADN, modo activo) es
 * responsabilidad del modulo {@code chakra} (F2); aqui solo son los valores de entrada del dominio.
 */
public enum RegenFactor {

    /** Clan Otsutsuki: regen acelerada. */
    CLAN_OTSUTSUKI,

    /** Clan Uzumaki: regen acelerada (mas que Otsutsuki). */
    CLAN_UZUMAKI,

    /** Portador de ADN de Hashirama ({@code has_hsdna}): regen acelerada (la mayor). */
    HASHIRAMA_DNA,

    /** Modo Sabio normal ({@code in_sage_mode_1}). */
    SAGE_MODE_1,

    /** Modo Sabio de los Seis Caminos ({@code in_sage_mode_2}). */
    SAGE_MODE_2
}
