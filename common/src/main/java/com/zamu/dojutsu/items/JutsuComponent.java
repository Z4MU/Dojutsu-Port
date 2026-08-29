package com.zamu.dojutsu.items;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component del item de jutsu (F4/DJT-075): <b>que tecnica</b> porta este item. Reemplaza el
 * multiplexado por NBT del datapack ({@code carrot_on_a_stick{JutsuItem:1b, JutsuId:55b}}) por un
 * componente tipado de 1.21, y el indice numerico por el <b>id data-driven</b> (regla 12).
 *
 * <p>Se guarda como texto (no se valida contra el {@code JutsuRegistry} aqui): un id que ya no exista
 * simplemente no sirve de nada — desde F11/DJT-232 el item no castea, y al usarlo la seleccion se valida
 * contra los jutsus <b>aprendidos</b>, con autoridad de servidor ({@code JutsuSelectionService}).
 *
 * @param jutsuId id data-driven de la tecnica ({@code "great_fireball"})
 */
public record JutsuComponent(String jutsuId) {

    public static final Codec<JutsuComponent> CODEC =
            Codec.STRING.xmap(JutsuComponent::new, JutsuComponent::jutsuId);

    public static final StreamCodec<ByteBuf, JutsuComponent> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(JutsuComponent::new, JutsuComponent::jutsuId);

    public JutsuComponent {
        Objects.requireNonNull(jutsuId, "jutsuId");
        jutsuId = jutsuId.toLowerCase(Locale.ROOT);
    }
}
