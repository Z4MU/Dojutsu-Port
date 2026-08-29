package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "mandame mi libro de jutsus" (F11/DJT-235). Marcador sin datos; el servidor
 * responde con un {@link JutsuBookPayload} construido desde el estado real. Gemelo de
 * {@link OpenEyeMenuPayload}.
 */
public record OpenJutsuBookPayload() implements CustomPacketPayload {

    public static final Type<OpenJutsuBookPayload> TYPE = new Type<>(DojutsuMod.id("open_jutsu_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenJutsuBookPayload> CODEC =
            StreamCodec.unit(new OpenJutsuBookPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
