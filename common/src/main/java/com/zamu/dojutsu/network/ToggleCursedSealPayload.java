package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "alterna mi sello maldito" (F6/DJT-140). Marcador sin datos — el servidor valida
 * que porte un sello. El cliente <b>no</b> decide si puede ({@code [ADD/07]}); reemplaza el item "activator" del
 * datapack.
 */
public record ToggleCursedSealPayload() implements CustomPacketPayload {

    public static final Type<ToggleCursedSealPayload> TYPE = new Type<>(DojutsuMod.id("toggle_cursed_seal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCursedSealPayload> CODEC =
            StreamCodec.unit(new ToggleCursedSealPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
