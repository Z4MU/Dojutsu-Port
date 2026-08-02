package com.zamu.dojutsu.jutsu;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.zamu.dojutsu.items.LearnerComponent;
import java.util.List;
import java.util.Objects;
import net.minecraft.util.ExtraCodecs;

/**
 * Secuencia de aprendizaje de un elemento (F4/DJT-075): la lista <b>ordenada</b> de jutsus que
 * su Skill Learner entrega, uno por uso. Replica los arboles {@code jutsu/learner/learn_<elem>}
 * del datapack (aprendizaje secuencial via {@code d.lrn_<elem>}) como dato auditable.
 *
 * <p><b>Codec estricto en el elemento</b> ({@code element} debe ser una naturaleza o un KG
 * conocidos y la secuencia no puede estar vacia); los <b>ids de jutsu NO se validan contra el
 * {@code JutsuRegistry}</b> al cargar: el catalogo llega por lotes (DJT-085/086) y el orden de
 * ejecucion de los reload listeners no esta garantizado — la validacion es en el momento de
 * aprender (jutsu aun sin definicion → "todavia no se puede aprender", sin avanzar la secuencia).
 *
 * @param element  id del elemento cuyo learner ensena esta secuencia ({@code "fire"}, {@code "wood"}...)
 * @param sequence ids de jutsu en el orden exacto de entrega del datapack
 */
public record LearnSequence(String element, List<String> sequence) {

    public static final Codec<LearnSequence> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_EMPTY_STRING.validate(LearnSequence::validElement)
                    .fieldOf("element").forGetter(LearnSequence::element),
            ExtraCodecs.nonEmptyList(ExtraCodecs.NON_EMPTY_STRING.listOf())
                    .fieldOf("sequence").forGetter(LearnSequence::sequence)
    ).apply(instance, LearnSequence::new));

    public LearnSequence {
        Objects.requireNonNull(element, "element");
        sequence = List.copyOf(Objects.requireNonNull(sequence, "sequence"));
    }

    /** Cuantos jutsus entrega en total el learner de este elemento. */
    public int size() {
        return sequence.size();
    }

    /** El jutsu en la posicion {@code progress} (el siguiente a aprender), o null si ya no quedan. */
    public String jutsuAt(int progress) {
        return progress >= 0 && progress < sequence.size() ? sequence.get(progress) : null;
    }

    /** Validacion estricta: el elemento debe existir como naturaleza o KG. */
    private static DataResult<String> validElement(String element) {
        return new LearnerComponent(element).isValid()
                ? DataResult.success(element)
                : DataResult.error(() -> "elemento desconocido: " + element);
    }
}
