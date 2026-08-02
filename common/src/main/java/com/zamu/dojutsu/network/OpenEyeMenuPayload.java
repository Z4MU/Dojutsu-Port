package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "abre mi menu de habilidades oculares" (DJT-117). Marcador sin datos — el
 * servidor construye la vista del menu desde el {@code EyeState} real del jugador y responde con
 * {@link EyeMenuPayload}. El cliente <b>no</b> decide que ojo/habilidades hay (autoridad del servidor,
 * {@code [ADD/07]}).
 */
public record OpenEyeMenuPayload() implements CustomPacketPayload {

    public static final Type<OpenEyeMenuPayload> TYPE = new Type<>(DojutsuMod.id("open_eye_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenEyeMenuPayload> CODEC =
            StreamCodec.unit(new OpenEyeMenuPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
