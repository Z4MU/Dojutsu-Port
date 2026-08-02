package com.zamu.dojutsu.items;

import com.mojang.serialization.Codec;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component del "Skill Learner" ({@code [ADD/03 §4]}): que <b>elemento</b> ensena este item. Reemplaza el
 * multiplexado por NBT del datapack ({@code carrot_on_a_stick{LearnId:1b, LearnerItem:1b}} + el nombre en el
 * display) por un componente <b>tipado</b> de 1.21 — justo el "posible error" que la ficha pedia evitar.
 *
 * <p>{@code element} es el id data-driven en minusculas de una naturaleza ({@code "fire"}) o de un KG
 * ({@code "wood"}); ambos espacios de nombres son disjuntos. Se guarda como texto y no como enum para que el
 * componente sobreviva a elementos futuros y a NBT ajeno; {@link #nature()}/{@link #kekkeiGenkai()} lo
 * resuelven al tipo (vacios si no existe: el uso lo trata como learner invalido, no crashea).
 *
 * @param element id del elemento que ensena ({@code "fire"}, {@code "wood"}, ...)
 */
public record LearnerComponent(String element) {

    public static final Codec<LearnerComponent> CODEC =
            Codec.STRING.xmap(LearnerComponent::new, LearnerComponent::element);

    public static final StreamCodec<ByteBuf, LearnerComponent> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(LearnerComponent::new, LearnerComponent::element);

    public LearnerComponent {
        Objects.requireNonNull(element, "element");
        element = element.toLowerCase(Locale.ROOT);
    }

    /** El elemento como naturaleza, si lo es. */
    public Optional<NatureType> nature() {
        for (NatureType nature : NatureType.values()) {
            if (nature.name().toLowerCase(Locale.ROOT).equals(element)) {
                return Optional.of(nature);
            }
        }
        return Optional.empty();
    }

    /** El elemento como Kekkei Genkai, si lo es. */
    public Optional<KekkeiGenkai> kekkeiGenkai() {
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            if (kg.name().toLowerCase(Locale.ROOT).equals(element)) {
                return Optional.of(kg);
            }
        }
        return Optional.empty();
    }

    /** Indica si el elemento existe (naturaleza o KG conocidos). */
    public boolean isValid() {
        return nature().isPresent() || kekkeiGenkai().isPresent();
    }
}
