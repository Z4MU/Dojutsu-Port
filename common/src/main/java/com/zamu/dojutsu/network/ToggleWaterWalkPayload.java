package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "alterna mi caminata sobre el agua" (F10, peticion de la verificacion en
 * partida). Marcador sin datos — el servidor invierte el {@code waterWalkOff} del {@code VisionState}.
 *
 * <p><b>No existe en el datapack</b>, donde caminar sobre el agua es automatico y la unica forma de bajarse es
 * mantenerse agachado. Cruzar un rio nadando obligaba a sostener el sneak todo el trayecto. Se añade con el
 * mismo patron que el {@code d.no_cv} de la chakra vision, que si es fiel: el cliente <b>solo pide alternar</b>,
 * el servidor decide y sincroniza.
 */
public record ToggleWaterWalkPayload() implements CustomPacketPayload {

    public static final Type<ToggleWaterWalkPayload> TYPE = new Type<>(DojutsuMod.id("toggle_water_walk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleWaterWalkPayload> CODEC =
            StreamCodec.unit(new ToggleWaterWalkPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
