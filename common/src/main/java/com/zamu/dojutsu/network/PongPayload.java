package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload de prueba Servidor -> Cliente (S2C). Respuesta al {@link PingPayload} con el
 * mismo {@code nonce}, para validar el ida-vuelta del puerto de red.
 *
 * @param nonce valor recibido en el Ping correspondiente
 */
public record PongPayload(int nonce) implements CustomPacketPayload {

    public static final Type<PongPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DojutsuMod.MOD_ID, "pong"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PongPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PongPayload::nonce, PongPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
