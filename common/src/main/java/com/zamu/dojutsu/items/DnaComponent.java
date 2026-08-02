package com.zamu.dojutsu.items;

import com.mojang.serialization.Codec;
import com.zamu.dojutsu.domain.items.DnaType;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component de una muestra de <b>ADN</b> ({@code [S14]}, F8/DJT-170): que lleva dentro, si es que ya se
 * sabe. Reemplaza el {@code carrot_on_a_stick{DNAId:Nb}} del datapack por un componente tipado.
 *
 * <p><b>Un ADN sin identificar es el mismo item con el componente vacio</b>, no otro item. Asi identificarlo es
 * rellenar un campo y no cambiar de objeto — que es exactamente lo que hace {@code dna/get_id} al sustituir el
 * {@code DNAId:0b} por el que toque.
 *
 * @param type id del ADN ({@code "wood"}, {@code "hashirama_cells"}), o vacio si aun no se ha identificado
 */
public record DnaComponent(Optional<String> type) {

    /** ADN recien encontrado, sin identificar. */
    public static final DnaComponent UNIDENTIFIED = new DnaComponent(Optional.empty());

    public static final Codec<DnaComponent> CODEC =
            Codec.STRING.optionalFieldOf("type").xmap(DnaComponent::new, DnaComponent::type).codec();

    public static final StreamCodec<ByteBuf, DnaComponent> STREAM_CODEC =
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).map(DnaComponent::new, DnaComponent::type);

    public DnaComponent {
        Objects.requireNonNull(type, "type");
        type = type.map(id -> id.toLowerCase(Locale.ROOT));
    }

    /** Componente de un ADN ya identificado. */
    public static DnaComponent of(DnaType type) {
        return new DnaComponent(Optional.of(type.id()));
    }

    /** El tipo resuelto, o vacio si no esta identificado (o el id es desconocido: estado tolerante). */
    public Optional<DnaType> resolved() {
        return type.flatMap(DnaType::byId);
    }

    /** Indica si aun no se sabe que ADN es. */
    public boolean isUnidentified() {
        return type.isEmpty();
    }
}
