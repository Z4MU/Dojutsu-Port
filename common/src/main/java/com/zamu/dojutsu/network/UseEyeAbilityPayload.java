package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "lanza mi habilidad ocular seleccionada" (DJT-118). Marcador sin datos — el
 * servidor lee la {@code selectedAbility} del {@code EyeState} real y ejecuta el cast (valida ojo/chakra,
 * consume chakra+strain). El cliente <b>no</b> decide qué habilidad ni si puede lanzarla ({@code [ADD/07]}).
 */
public record UseEyeAbilityPayload() implements CustomPacketPayload {

    public static final Type<UseEyeAbilityPayload> TYPE = new Type<>(DojutsuMod.id("use_eye_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UseEyeAbilityPayload> CODEC =
            StreamCodec.unit(new UseEyeAbilityPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
