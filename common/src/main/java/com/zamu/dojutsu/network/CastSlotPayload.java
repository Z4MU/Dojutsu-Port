package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: lanzar el jutsu de un <b>hueco rapido</b> (F11/DJT-234). Es el atajo de
 * combate: la rueda sirve para preparar el set entre peleas, y esto para tirar de el sin abrir nada.
 *
 * <p><b>Aqui si viaja un indice</b>, y es la excepcion consciente a "por nombre, nunca por indice": no es un
 * indice de una lista que el cliente construyo y que podria estar desincronizada, sino la <b>posicion fisica</b>
 * del hueco, que es exactamente lo que el jugador pulso. El servidor lee que jutsu hay en ese hueco de su
 * propio estado; si esta vacio no pasa nada.
 *
 * @param slot hueco pulsado, 0-7 (la tecla 1 es el hueco 0). Fuera de rango se ignora en el servidor
 */
public record CastSlotPayload(int slot) implements CustomPacketPayload {

    public static final Type<CastSlotPayload> TYPE = new Type<>(DojutsuMod.id("cast_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSlotPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CastSlotPayload::slot,
            CastSlotPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
