package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor de la tecla "lanzar jutsu" (F4/DJT-080, {@code [ADD/05 §Input]},
 * {@code [ADD/07]}, {@code [S06]}). Reemplaza el polling de {@code carrot_on_a_stick{JutsuId}} del datapack por
 * input real validado en servidor.
 *
 * <p><b>Marcador sin datos</b> — el cliente no dice <b>que</b> lanzar, solo <b>que quiere lanzar</b>. El
 * servidor lee {@code JutsuKnowledge.selectedJutsu} del estado real y castea por el {@code JutsuCastPipeline}.
 * Un payload no puede inyectar un jutsu arbitrario.
 *
 * <p><b>Cambio en F11/DJT-231:</b> antes llevaba la {@code InteractionHand} y el servidor leia el
 * {@code JutsuComponent} del item de esa mano, o sea que la tecnica lanzada era "lo que llevas en la mano".
 * Con el selector, el jutsu vive en el estado del jugador y el payload se queda sin nada que transportar. El
 * item de jutsu sobrevive como pergamino de aprendizaje, no como via de lanzamiento.
 */
public record CastJutsuPayload() implements CustomPacketPayload {

    public static final Type<CastJutsuPayload> TYPE = new Type<>(DojutsuMod.id("cast_jutsu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastJutsuPayload> CODEC =
            StreamCodec.unit(new CastJutsuPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
