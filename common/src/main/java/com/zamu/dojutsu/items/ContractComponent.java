package com.zamu.dojutsu.items;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component del <b>contrato de invocacion</b> ({@code [S15]}, F8/DJT-175): con que especie esta firmado, si
 * lo esta. Reemplaza los 28 estados de NBT del datapack ({@code Contract:0b} en blanco + {@code 1..27}).
 *
 * <p><b>Un contrato en blanco es el mismo item con el componente vacio</b>, igual que el ADN sin identificar:
 * firmarlo rellena un campo, no cambia de objeto. Asi el item captura los dos gestos —golpear para firmar, clic
 * derecho para invocar— sin duplicarse.
 *
 * @param species id del contrato firmado ({@code "wolf"}), o vacio si esta en blanco
 */
public record ContractComponent(Optional<String> species) {

    /** Contrato en blanco, sin firmar. */
    public static final ContractComponent BLANK = new ContractComponent(Optional.empty());

    public static final Codec<ContractComponent> CODEC =
            Codec.STRING.optionalFieldOf("species").xmap(ContractComponent::new, ContractComponent::species).codec();

    public static final StreamCodec<ByteBuf, ContractComponent> STREAM_CODEC =
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8)
                    .map(ContractComponent::new, ContractComponent::species);

    public ContractComponent {
        Objects.requireNonNull(species, "species");
        species = species.map(id -> id.toLowerCase(Locale.ROOT));
    }

    /** Componente de un contrato ya firmado. */
    public static ContractComponent of(String speciesId) {
        return new ContractComponent(Optional.of(speciesId));
    }

    /** Indica si aun no esta firmado. */
    public boolean isBlank() {
        return species.isEmpty();
    }
}
