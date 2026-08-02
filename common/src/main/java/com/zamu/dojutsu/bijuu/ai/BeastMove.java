package com.zamu.dojutsu.bijuu.ai;

/**
 * Tecnica de Bestia con Cola {@code [S09]} (F7/DJT-160): <b>una sola implementacion compartida</b> por la IA de
 * la bestia y por el jinchuriki transformado.
 *
 * <h2>Por que existe esta interfaz</h2>
 * Es la respuesta directa al riesgo <b>S09-R2</b>. En el datapack, la Bola Bestia con Cola vive <b>dos veces</b>:
 * en {@code tailed_beasts/ai/moves/tailed_beast_ball/} y en {@code jinchuriki/form/<bestia>/tailed_beast_ball/}.
 * Y ya han <b>divergido</b>: la version de la IA dura 100 ticks y hace daño amplificador 3, la del jugador dura
 * 1200 y explota con tres minecarts de TNT; solo la de la IA sabe que el Camino Preta puede absorberla. Portar
 * eso literalmente seria arrastrar el bug.
 *
 * <p>Aqui la tecnica se escribe una vez y el {@link BeastMoveContext} dice quien la lanza. La diferencia real
 * —que el jugador paga chakra y la bestia no— se expresa en el <b>pipeline</b>, no en dos copias del efecto:
 * {@link #chakraCost()} es el precio <b>para el jinchuriki</b>. El datapack tenia que fingirlo poniendole
 * {@code d.chakra 10000} a la bestia antes de invocar la funcion compartida ({@code ai/cast_2}); nosotros no
 * necesitamos esa mentira.
 *
 * <p>Mismo contrato que {@code EyeAbility} (F5/DJT-118), deliberadamente: quien haya escrito una habilidad
 * ocular sabe escribir una tecnica de bestia.
 */
public interface BeastMove {

    /** Id data-driven de la tecnica ({@code "tailed_beast_ball"}); coincide con el del catalogo. */
    String id();

    /**
     * Chakra que cuesta lanzarla <b>a un jinchuriki transformado</b>. La bestia no paga: su coste es el
     * temporizador de la IA.
     */
    int chakraCost();

    /** Ticks de reutilizacion tras lanzarla. */
    int cooldownTicks();

    /**
     * Comprueba si la tecnica puede ejecutarse ahora (p. ej. hace falta un objetivo a la vista). Devolver
     * {@code false} <b>no</b> cobra nada ni consume el turno de la IA. Por defecto siempre puede.
     */
    default boolean canApply(BeastMoveContext context) {
        return true;
    }

    /** Ejecuta la tecnica. Solo se llama tras pasar {@link #canApply} y haber cobrado, si procedia. */
    void execute(BeastMoveContext context);
}
