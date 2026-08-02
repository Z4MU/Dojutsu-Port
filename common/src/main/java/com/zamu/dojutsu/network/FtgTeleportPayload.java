package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "saltar a mi marca del Vuelo del Dios del Trueno" (F8/DJT-180). Marcador sin
 * datos — el servidor valida que haya marca, el fuinjutsu, el chakra y el veto, y <b>el servidor</b> decide a
 * donde te mueve ({@code [ADD/07]}: un teleport nunca lo pide el cliente con coordenadas).
 *
 * <p>En el datapack el gesto es <b>agachado</b> con el kunai FTG en la mano ({@code throwing/main}); aqui es una
 * tecla, porque la marca ya no vive en el item.
 */
public record FtgTeleportPayload() implements CustomPacketPayload {

    public static final Type<FtgTeleportPayload> TYPE = new Type<>(DojutsuMod.id("ftg_teleport"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FtgTeleportPayload> CODEC =
            StreamCodec.unit(new FtgTeleportPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
