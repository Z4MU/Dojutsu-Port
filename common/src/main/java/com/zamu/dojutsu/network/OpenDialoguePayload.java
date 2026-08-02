package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Servidor -&gt; Cliente: "abre el dialogo de este Sabio" (F9/DJT-200). Reemplaza el <b>armor stand
 * narrador</b> del datapack ({@code main/internal/narr}) y su barrido por tick: el dialogo es una pantalla, no una
 * entidad que sigue al jugador.
 *
 * <p>Solo lo manda el <b>primer</b> encuentro: el segundo ({@code dialogue/second}) no pregunta nada —se despide y
 * concede la mitad que falte—, asi que se resuelve entero en el servidor y llega por chat.
 *
 * @param sage id del sabio que habla ({@code "hagoromo"}, {@code "hamura"})
 */
public record OpenDialoguePayload(String sage) implements CustomPacketPayload {

    public static final Type<OpenDialoguePayload> TYPE = new Type<>(DojutsuMod.id("open_dialogue"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::sage,
            OpenDialoguePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
