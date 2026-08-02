package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "despierta mi forma eterna (EMS)" (fleco F5). Marcador sin datos — el servidor
 * valida contra el ojo activo real (eternalizable + clan + uso + medicina, no eterno ya) y aplica los bonos. El
 * cliente <b>no</b> decide si procede ({@code [ADD/07]}); reemplaza las herramientas quirurgicas del datapack.
 */
public record AwakenEternalPayload() implements CustomPacketPayload {

    public static final Type<AwakenEternalPayload> TYPE = new Type<>(DojutsuMod.id("awaken_eternal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AwakenEternalPayload> CODEC =
            StreamCodec.unit(new AwakenEternalPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
