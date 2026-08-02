package com.zamu.dojutsu.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

/**
 * Definicion data-driven de un desbloqueo por hito ({@code unlocks} del datapack, {@code [S20]}): al alcanzar
 * {@code requiredLevel} en la habilidad/afinidad {@code skillId}, se otorga {@code unlockId}.
 *
 * <p>Se carga desde {@code data/dojutsu/unlocks/*.json} (ver {@code UnlockReloadListener}). El efecto real
 * (entregar el item de jutsu asociado) llega en F4; en F2 el desbloqueo solo registra el id en el estado.
 *
 * <h2>Escala (F10/DJT-221)</h2>
 * {@code requiredLevel} va en <b>escala del mod</b>: las habilidades y afinidades del datapack se dividen
 * <b>entre 4</b>. No es una convencion arbitraria — el techo de habilidad del mod es 500 ({@code XpCurve}) y los
 * requisitos mas altos del datapack valen exactamente <b>2000</b> (maestria de las Ocho Puertas, tercer sello de
 * fuinjutsu, Blaze, Creation Rebirth SOAH): 2000 ÷ 4 = 500, el tope. Las dos escalas son la misma vista con
 * distinto zoom.
 *
 * <p>Lo que <b>no</b> se reescala: {@code requiredMaxChakra} y el {@code ninja_xp}, que valen lo mismo en los dos
 * lados.
 *
 * @param unlockId          id del contenido desbloqueado
 * @param skillId           habilidad/afinidad cuyo nivel dispara el hito (minusculas, p. ej. "kenjutsu")
 * @param requiredLevel     nivel a partir del cual se otorga, en escala del mod (datapack ÷ 4)
 * @param requiredMaxChakra chakra maximo tambien exigido (0 = sin requisito). Cuatro hitos del datapack piden
 *                          <b>dos cosas a la vez</b>: Creation Rebirth (medical 500 <b>y</b> 10 000 de chakra
 *                          maximo), su version SOAH (2000 y 30 000), el kunai del Dios del Trueno (fuinjutsu
 *                          1000 y 10 000) y el acceso a Senjutsu (solo chakra). Sin este campo se otorgarian
 *                          antes de tiempo
 * @param ninjaXp           ninja_xp que suma el hito al otorgarse ({@code scoreboard players add @s d.ninja_xp N}
 *                          en {@code main/internal/unlocks}). <b>No es uniforme</b>: va de 1 (Substitution) a 100
 *                          (Izanagi, Ocho Puertas, SOAH)
 * @param grantsJutsus      tecnicas que <b>entrega</b> el hito ({@code function dojutsu:give/jutsu/<id>}). Es
 *                          una lista porque el datapack tiene un caso de dos: la octava puerta da a la vez el
 *                          Elefante Nocturno y el Night Guy. Vacia = el hito no da tecnica (las Ocho Puertas
 *                          dan una <b>capacidad</b>, el contrato y el kunai dan <b>items</b>)
 */
public record UnlockDefinition(String unlockId, String skillId, int requiredLevel, int requiredMaxChakra,
        int ninjaXp, List<String> grantsJutsus) {

    public static final Codec<UnlockDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("unlockId").forGetter(UnlockDefinition::unlockId),
            Codec.STRING.fieldOf("skillId").forGetter(UnlockDefinition::skillId),
            Codec.INT.fieldOf("requiredLevel").forGetter(UnlockDefinition::requiredLevel),
            Codec.INT.optionalFieldOf("requiredMaxChakra", 0).forGetter(UnlockDefinition::requiredMaxChakra),
            Codec.INT.optionalFieldOf("ninjaXp", 0).forGetter(UnlockDefinition::ninjaXp),
            Codec.STRING.listOf().optionalFieldOf("grantsJutsus", List.of())
                    .forGetter(UnlockDefinition::grantsJutsus)
    ).apply(instance, UnlockDefinition::new));

    public UnlockDefinition {
        grantsJutsus = List.copyOf(grantsJutsus);
    }

    /** Constructor de conveniencia para los hitos sin co-requisito, XP ni entrega (tests). */
    public UnlockDefinition(String unlockId, String skillId, int requiredLevel) {
        this(unlockId, skillId, requiredLevel, 0, 0, List.of());
    }

    /** Constructor de conveniencia sin entrega de tecnica (tests de balance). */
    public UnlockDefinition(String unlockId, String skillId, int requiredLevel, int requiredMaxChakra,
            int ninjaXp) {
        this(unlockId, skillId, requiredLevel, requiredMaxChakra, ninjaXp, List.of());
    }

    /** Lista de definiciones (formato de cada archivo JSON: un array de hitos). */
    public static final Codec<java.util.List<UnlockDefinition>> LIST_CODEC = CODEC.listOf();
}
