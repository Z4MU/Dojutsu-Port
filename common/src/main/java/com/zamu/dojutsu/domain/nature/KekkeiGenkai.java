package com.zamu.dojutsu.domain.nature;

/**
 * Kekkei Genkai (lineas de sangre) del ninja {@code [S03]}: las 18 combinaciones que reemplazan los flags
 * {@code d.kg_<nombre>} del datapack.
 *
 * <p>Un KG <b>implica</b> sus naturalezas-componente (p. ej. Wood = Earth + Water, Ice = Wind + Water). Esa
 * relacion <b>no se declara aqui</b>: es data-driven y llega en DJT-052 ({@code KekkeiGenkaiRegistry} + JSON);
 * hardcodearla en el enum seria justo el error que el ticket quiere evitar. Este enum solo fija la
 * <b>identidad</b> de cada KG.
 *
 * <p><b>Por que 18 y no 19:</b> el datapack tiene 19 ficheros {@code natures/give/kg/*} (sin contar
 * {@code all}/{@code random}), pero {@code hashirama_cells} <b>no es un KG</b>: no escribe ningun
 * {@code d.kg_*}, sino el flag {@code d.has_hsdna} (celulas de Hashirama implantadas, con efectos propios de
 * medical/regen). Por eso vive como {@code hashiramaDna} en {@code NatureProfile} y no como constante de este
 * enum. Con esa correccion, los ficheros cuadran con los <b>18 KG</b> que declara {@code [S03]}.
 *
 * <p>Nota: el nombre del flag del datapack no siempre coincide con el del fichero ({@code shikotsumyaku} ->
 * {@code d.kg_shiko}, {@code jugo_clan} -> {@code d.kg_jugo}). Aqui manda el <b>nombre canonico</b> del
 * fichero/JSON; el mod no persiste los nombres de scoreboard.
 *
 * <p>Dominio puro: sin imports de Minecraft.
 */
public enum KekkeiGenkai {

    WOOD,
    ICE,
    LAVA,
    STORM,
    BOIL,
    DUST,
    MAGNET,
    EXPLOSION,
    SCORCH,
    STEEL,
    DARK,
    SWIFT,
    CRYSTAL,
    TYPHOON,
    MUD,
    BLAZE,
    /** Shikotsumyaku (huesos, clan Kaguya). Flag del datapack: {@code d.kg_shiko}. */
    SHIKOTSUMYAKU,
    /**
     * KG del clan Jugo. Flag del datapack: {@code d.kg_jugo}. Habilita la regeneracion de chakra natural
     * ({@code [S02]}): es el gancho que hoy deja inactivo {@code RegenService.hasJugoClan()} (se enchufa al
     * portar el clan, no aqui).
     */
    JUGO_CLAN
}
