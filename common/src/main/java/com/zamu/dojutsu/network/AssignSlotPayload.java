package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: colocar (o vaciar) un hueco de la rueda desde el libro (F11/DJT-235).
 *
 * <p>El hueco viaja como <b>posicion</b> —es lo que el jugador pulso, igual que en {@link CastSlotPayload}—
 * y el jutsu <b>por nombre</b>, porque ahi si es una eleccion sobre una lista que dibujo el cliente. El
 * servidor revalida que lo conozca.
 *
 * @param slot    hueco destino, 0-7
 * @param jutsuId jutsu a colocar; cadena vacia = <b>vaciar</b> el hueco
 */
public record AssignSlotPayload(int slot, String jutsuId) implements CustomPacketPayload {

    public static final Type<AssignSlotPayload> TYPE = new Type<>(DojutsuMod.id("assign_slot"));

    /** Tope de longitud del id: un jutsuId real son decenas de caracteres, no kilobytes. */
    static final int MAX_ID_LENGTH = 128;

    public static final StreamCodec<RegistryFriendlyByteBuf, AssignSlotPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AssignSlotPayload::slot,
            ByteBufCodecs.stringUtf8(MAX_ID_LENGTH), AssignSlotPayload::jutsuId,
            AssignSlotPayload::new);

    /** Indica si este payload pide vaciar el hueco en vez de colocar algo. */
    public boolean isClear() {
        return jutsuId.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
