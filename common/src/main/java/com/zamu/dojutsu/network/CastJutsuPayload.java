package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

/**
 * Payload Cliente -&gt; Servidor de la tecla "lanzar jutsu" (F4/DJT-080, {@code [ADD/05 §Input]},
 * {@code [ADD/07]}, {@code [S06]}). Reemplaza el polling de {@code carrot_on_a_stick{JutsuId}} del datapack por
 * input real validado en servidor.
 *
 * <p><b>El cliente no decide qué jutsu es</b> (RM-1): el payload solo indica <b>la mano</b> con la que se pulsó;
 * el servidor lee el {@link com.zamu.dojutsu.items.JutsuComponent} del ítem <b>real</b> de esa mano, comprueba
 * que el jugador conozca el jutsu y castea por el {@code JutsuCastPipeline}. Un payload no puede inyectar un
 * jutsu arbitrario.
 *
 * @param hand mano con la que se solicitó el cast (el servidor lee el ítem de esa mano)
 */
public record CastJutsuPayload(InteractionHand hand) implements CustomPacketPayload {

    public static final Type<CastJutsuPayload> TYPE = new Type<>(DojutsuMod.id("cast_jutsu"));

    /** Codec de la mano por su ordinal (0 = principal, 1 = secundaria); fuera de rango → principal. */
    private static final StreamCodec<ByteBuf, InteractionHand> HAND_CODEC = ByteBufCodecs.VAR_INT.map(
            ordinal -> {
                InteractionHand[] values = InteractionHand.values();
                return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : InteractionHand.MAIN_HAND;
            },
            InteractionHand::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, CastJutsuPayload> CODEC = StreamCodec.composite(
            HAND_CODEC, CastJutsuPayload::hand,
            CastJutsuPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
