package com.zamu.dojutsu.domain.vision;

/**
 * Relacion de una entidad con el observador para la <b>chakra vision</b> {@code [S25]} (DJT-125): el color con
 * el que el ojo la resalta. Son las cuatro categorias del datapack ({@code tags/entity_types/chakra_highlights/
 * *.json}), traducidas a una decision de dominio.
 *
 * <p>Dominio puro: el color se expresa como el <b>nombre de un color vanilla</b> ({@link net.minecraft.ChatFormatting}
 * lo resuelve en el glue) para no importar Minecraft aqui, y como RGB para referencia. El contorno lo dibuja el
 * <b>glow nativo</b> coloreado por el equipo del observado, no un renderer propio ni particulas por tick (RB-3).
 */
public enum EntityRelationship {

    /** Aliados util(es): aldeanos, lobos, golems… El datapack los pinta de naranja. */
    SPECIAL("gold", 0xFF9933),
    /** Pasivos inofensivos: gallinas, vacas, ovejas… Verde. */
    FRIENDLY("green", 0x4DFF21),
    /** Neutrales (atacan si se les provoca): endermen, aranas, pandas… Amarillo. */
    NEUTRAL("yellow", 0xFFFF4D),
    /** Hostiles: zombis, esqueletos, creepers… Rojo. */
    HOSTILE("red", 0xFF0000);

    private final String teamColorName;
    private final int rgb;

    EntityRelationship(String teamColorName, int rgb) {
        this.teamColorName = teamColorName;
        this.rgb = rgb;
    }

    /** Nombre del color vanilla del equipo de glow (lo resuelve el glue a {@code ChatFormatting}). */
    public String teamColorName() {
        return teamColorName;
    }

    /** Color RGB de referencia (el que usaba el {@code particle dust} del datapack). */
    public int rgb() {
        return rgb;
    }

    /** Id del equipo de scoreboard que colorea el glow de esta relacion. */
    public String teamName() {
        return "dojutsu.cv." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
