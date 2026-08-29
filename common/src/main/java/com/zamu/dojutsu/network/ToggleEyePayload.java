package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "alterna mi dojutsu" (F11/DJT-230). Marcador sin datos: el cliente manda la
 * <b>intencion</b> de alternar, nunca el estado destino, para que dos pulsaciones seguidas no puedan dejar el
 * ojo en un estado que el cliente eligio ({@code [ADD/07]}, regla 12).
 *
 * <p>El servidor valida que el jugador tenga un ojo y que la fatiga ocular no lo impida, y aplica un
 * rate-limit: sin el, mantener la tecla haria parpadear el ojo en todos los clientes que vean al jugador.
 *
 * <p>El datapack no tiene equivalente — alli "apagar" era quitarse el item del slot de casco.
 */
public record ToggleEyePayload() implements CustomPacketPayload {

    public static final Type<ToggleEyePayload> TYPE = new Type<>(DojutsuMod.id("toggle_eye"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleEyePayload> CODEC =
            StreamCodec.unit(new ToggleEyePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
