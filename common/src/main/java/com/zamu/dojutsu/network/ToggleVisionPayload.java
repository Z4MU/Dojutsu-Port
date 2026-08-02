package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "alterna mi chakra vision" (DJT-125). Marcador sin datos — el servidor
 * invierte el {@code chakraVisionOff} del {@code VisionState}. El cliente <b>no</b> decide si tiene vision (eso
 * depende del ojo activo), solo pide alternarla, como el {@code /trigger chakra_vision} del datapack
 * ({@code [ADD/07]}).
 */
public record ToggleVisionPayload() implements CustomPacketPayload {

    public static final Type<ToggleVisionPayload> TYPE = new Type<>(DojutsuMod.id("toggle_vision"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleVisionPayload> CODEC =
            StreamCodec.unit(new ToggleVisionPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
