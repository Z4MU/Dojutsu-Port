package com.zamu.dojutsu.domain.eye;

import java.util.Objects;

/**
 * Un requisito de una arista del grafo de ojos ({@link EyeEvolution}): la puerta que el datapack pone a un
 * despertar o evolucion (predicates {@code <eye>_use_level_N}, {@code clan/is_*}, flags como el ADN de
 * Hashirama). Modela el <b>qué</b> como dato, sin ramas hardcodeadas (RA-3): quien camina el grafo
 * (AwakeningService/EvolutionService, DJT-114/115) interpreta cada requisito contra el estado del jugador.
 *
 * <p>Dominio puro (sin {@code Codec}): el codec vive en {@link com.zamu.dojutsu.dojutsu.EyeDefinition}.
 *
 * @param type  clase de requisito
 * @param min   umbral minimo del contador de uso ({@link Type#USE_LEVEL}); ignorado por los demas tipos
 * @param max   tope del contador de uso ({@link Type#USE_LEVEL}); {@link Integer#MAX_VALUE} = sin tope. Sirve
 *              para las <b>rutas divergentes</b> (R6): el Sharingan de un Uchiha/Otsutsuki usa el tramo
 *              <b>acotado</b> {@code [216000, 432000)} para poder pasar a Mangekyo, mientras que un ojo
 *              implantado usa el tramo <b>abierto</b> {@code >=216000} y se queda en 3 tomoe
 * @param value id de clan ({@link Type#CLAN}, p. ej. {@code "otsutsuki_uchiha"}) o nombre de flag
 *              ({@link Type#FLAG}, p. ej. {@code "hashirama_dna"}); vacio para {@link Type#USE_LEVEL}
 */
public record EyeRequirement(Type type, int min, int max, String value) {

    /** Clase de requisito de una arista del grafo. */
    public enum Type {

        /** Umbral (y tope opcional) sobre el contador de uso de la <b>familia del ojo origen</b> del datapack
         * ({@code d.<eye>_use}): {@code min <= useLevel < max}. */
        USE_LEVEL,

        /** El jugador debe pertenecer al clan (o cumplir el predicate de clan compuesto) indicado en
         * {@link #value} ({@code clan/is_*}: {@code uchiha}, {@code hyuga}, {@code chinoike}, {@code otsutsuki},
         * {@code otsutsuki_uchiha}). */
        CLAN,

        /** El jugador debe tener activo el flag indicado en {@link #value} ({@code hashirama_dna}...). */
        FLAG
    }

    public EyeRequirement {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") no puede superar a max (" + max + ")");
        }
        if ((type == Type.CLAN || type == Type.FLAG) && value.isBlank()) {
            throw new IllegalArgumentException(type + " exige un 'value' no vacio");
        }
    }

    /** Requisito de uso con tope abierto (ruta implante: sin evolucion posterior). */
    public static EyeRequirement useAtLeast(int min) {
        return new EyeRequirement(Type.USE_LEVEL, min, Integer.MAX_VALUE, "");
    }

    /** Requisito de uso acotado {@code [min, max)} (ruta Uchiha/Otsutsuki: permite la siguiente evolucion). */
    public static EyeRequirement useBetween(int min, int max) {
        return new EyeRequirement(Type.USE_LEVEL, min, max, "");
    }

    /** Requisito de pertenencia a clan / predicate de clan. */
    public static EyeRequirement clan(String clanPredicate) {
        return new EyeRequirement(Type.CLAN, 0, Integer.MAX_VALUE, clanPredicate);
    }

    /** Requisito de flag ({@code hashirama_dna}...). */
    public static EyeRequirement flag(String flagName) {
        return new EyeRequirement(Type.FLAG, 0, Integer.MAX_VALUE, flagName);
    }
}
