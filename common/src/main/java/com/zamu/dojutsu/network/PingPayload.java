package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload de prueba Cliente -> Servidor (C2S). Lleva un {@code nonce} que el servidor
 * devuelve en el {@link PongPayload}, para validar el ida-vuelta del puerto de red.
 *
 * @param nonce valor arbitrario que identifica el intercambio
 */
public record PingPayload(int nonce) implements CustomPacketPayload {

    public static final Type<PingPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DojutsuMod.MOD_ID, "ping"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PingPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PingPayload::nonce, PingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
