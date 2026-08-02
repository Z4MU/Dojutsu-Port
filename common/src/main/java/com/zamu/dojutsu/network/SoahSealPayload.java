package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor del Sello de la Fuerza de un Centenar (F6/DJT-150). Lleva si el jugador iba
 * <b>agachado</b>, porque el datapack distingue los dos gestos con el mismo item: agachado y con la reserva
 * vacia, <b>recarga</b>; en cualquier otro caso, alterna. El servidor valida requisitos y vetos
 * ({@code [ADD/07]}).
 *
 * @param sneaking si el jugador estaba agachado al pulsar
 */
public record SoahSealPayload(boolean sneaking) implements CustomPacketPayload {

    public static final Type<SoahSealPayload> TYPE = new Type<>(DojutsuMod.id("soah_seal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoahSealPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SoahSealPayload::sneaking,
            SoahSealPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
