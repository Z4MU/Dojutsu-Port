package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "alterna mi modo sabio" (F6/DJT-136). Marcador sin datos — el servidor valida
 * que sea sabio, que no lleve un sello maldito activo y que tenga energia suficiente para su nivel. El cliente
 * <b>no</b> decide si puede ({@code [ADD/07]}); reemplaza los items "activator" del datapack.
 */
public record ToggleSagePayload() implements CustomPacketPayload {

    public static final Type<ToggleSagePayload> TYPE = new Type<>(DojutsuMod.id("toggle_sage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleSagePayload> CODEC =
            StreamCodec.unit(new ToggleSagePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
