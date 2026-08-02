package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor de las Ocho Puertas (F6/DJT-145). A diferencia de los demas modos, aqui la tecla
 * tiene <b>dos intenciones</b> —abrir la siguiente puerta o cerrarlas todas— igual que el clic y el clic-agachado
 * del item del datapack ({@code eight_gates/main}). El cliente manda cual de las dos; el servidor valida
 * taijutsu, vetos y puerta maxima ({@code [ADD/07]}).
 *
 * @param close {@code true} = cerrar todas las puertas; {@code false} = abrir la siguiente
 */
public record EightGatesPayload(boolean close) implements CustomPacketPayload {

    public static final Type<EightGatesPayload> TYPE = new Type<>(DojutsuMod.id("eight_gates"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EightGatesPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, EightGatesPayload::close,
            EightGatesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
