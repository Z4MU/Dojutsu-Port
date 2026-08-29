package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Servidor -&gt; Cliente con la vista del libro de jutsus (F11/DJT-235): las fichas de las tecnicas
 * que el jugador <b>conoce</b> y como tiene repartida la rueda. Mismo patron peticion/respuesta que
 * {@link EyeMenuPayload}: la Screen pide al abrirse y el servidor contesta con su verdad.
 *
 * <p>Se mandan <b>solo las aprendidas</b>, no el catalogo entero: el libro es el inventario de tecnicas del
 * jugador, no una enciclopedia, y asi lo que viaja crece con lo que el jugador tiene y no con lo que el
 * datapack define.
 *
 * @param entries fichas de las tecnicas conocidas, en orden estable
 * @param slots   reparto actual de la rueda ({@code ""} = hueco libre)
 */
public record JutsuBookPayload(List<JutsuBookEntry> entries, List<String> slots)
        implements CustomPacketPayload {

    public static final Type<JutsuBookPayload> TYPE = new Type<>(DojutsuMod.id("jutsu_book"));

    private static final StreamCodec<ByteBuf, List<JutsuBookEntry>> ENTRIES_CODEC =
            JutsuBookEntry.STREAM_CODEC.apply(ByteBufCodecs.list());

    private static final StreamCodec<ByteBuf, List<String>> SLOTS_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, JutsuBookPayload> CODEC = StreamCodec.composite(
            ENTRIES_CODEC, JutsuBookPayload::entries,
            SLOTS_CODEC, JutsuBookPayload::slots,
            JutsuBookPayload::new);

    public JutsuBookPayload {
        entries = List.copyOf(entries);
        slots = List.copyOf(slots);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
