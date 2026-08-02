package com.zamu.dojutsu.domain.jutsu;

/**
 * Contexto de lanzamiento de una tecnica: expone lo que la maquina de precondiciones necesita saber
 * del <b>caster generico</b>.
 *
 * <p>No asume jugador (S17-R3): un NPC con IA implementa esta interfaz igual que un {@code Player}. El
 * dominio no conoce ni {@code Player} ni el sistema de naturalezas (F3): la afinidad se consulta por el
 * id data-driven de la naturaleza. Dominio puro.
 */
public interface CastContext {

    /** Chakra disponible del caster ahora mismo. */
    int availableChakra();

    /** Indica si la tecnica de id dado esta en cooldown para este caster. */
    boolean isOnCooldown(String jutsuId);

    /**
     * Indica si el caster <b>posee</b> la naturaleza (por su id data-driven). Es la puerta de
     * {@code check_affinity} del datapack ("Lack required Chakra Nature."), distinta del nivel: un caster
     * puede conservar nivel de afinidad de una naturaleza revocada (F4/DJT-074, regla 16).
     */
    boolean hasNature(String natureId);

    /**
     * Nivel de afinidad del caster para una naturaleza (por su id data-driven). Un caster sin perfil de
     * afinidad devuelve 0.
     */
    int affinity(String natureId);

    /**
     * Nivel de una habilidad del caster (por su id data-driven: {@code "kenjutsu"}, {@code "medical"}...).
     * Un caster sin esa habilidad (o sin progresion) devuelve 0. F4/DJT-083.
     */
    default int skillLevel(String skillId) {
        return 0;
    }

    /**
     * Indica si el caster <b>posee</b> el Kekkei Genkai (por su id data-driven: {@code "wood"}, {@code "ice"}...).
     * Es la puerta {@code check_kekkei_genkai} del datapack, exclusiva de las tecnicas KG. Un caster sin
     * ese KG (o sin perfil) devuelve {@code false}. F4/DJT-091.
     */
    default boolean hasKekkeiGenkai(String kekkeiGenkaiId) {
        return false;
    }
}
