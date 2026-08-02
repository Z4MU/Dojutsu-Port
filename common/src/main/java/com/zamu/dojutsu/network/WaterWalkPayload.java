package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Servidor -&gt; Cliente: "estas caminando sobre el agua" (F9/DJT-205).
 *
 * <p><b>Existe para que el cliente no pelee con el servidor.</b> El cliente aplica la levitacion por su cuenta
 * —los efectos se sincronizan— y con ella tiende a <b>subir</b>; el servidor lo bajaba de un tiron cada tick, y
 * esa pelea es el jitter de {@code [RB-1]}. Sabiendo el estado, el cliente anula su propia velocidad vertical y
 * los dos coinciden: ni correcciones ni tirones.
 *
 * <p>Se manda <b>solo al cambiar de estado</b>, no por tick: dos paquetes por travesia.
 *
 * @param active si el jugador local se esta sosteniendo sobre el agua
 */
public record WaterWalkPayload(boolean active) implements CustomPacketPayload {

    public static final Type<WaterWalkPayload> TYPE = new Type<>(DojutsuMod.id("water_walk"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WaterWalkPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, WaterWalkPayload::active,
            WaterWalkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
