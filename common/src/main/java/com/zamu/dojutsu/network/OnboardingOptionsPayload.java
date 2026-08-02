package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Servidor -&gt; Cliente con las <b>opciones de clan</b> de la creacion del ninja (DJT-060): el
 * {@code ClanRegistry} es data-driven y vive en el servidor (en un servidor dedicado el cliente no lo tiene),
 * asi que la Screen recibe la lista por red en vez de leerla de un registro local.
 *
 * <p>Es <b>solo presentacion</b> (que botones pintar y de que color): la eleccion vuelve por
 * {@code OnboardingChoicePayload} y el servidor la re-valida contra el registro real — un cliente con una
 * lista manipulada no puede unirse a un clan inexistente.
 *
 * @param clans opciones en el orden de presentacion (ids ordenados alfabeticamente; el orden por paginas del
 *              datapack era un artefacto de la hotbar de 9 slots, no un ranking de diseno)
 */
public record OnboardingOptionsPayload(List<ClanOption> clans) implements CustomPacketPayload {

    /**
     * Una opcion de clan para la Screen.
     *
     * @param id    id del clan (el que se devuelve en la eleccion)
     * @param color color RGB del nombre (del {@code teamColor} del clan, DJT-062)
     */
    public record ClanOption(String id, int color) {

        public static final StreamCodec<ByteBuf, ClanOption> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, ClanOption::id,
                ByteBufCodecs.VAR_INT, ClanOption::color,
                ClanOption::new);
    }

    public static final Type<OnboardingOptionsPayload> TYPE = new Type<>(DojutsuMod.id("onboarding_options"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OnboardingOptionsPayload> CODEC =
            StreamCodec.composite(
                    ClanOption.CODEC.apply(ByteBufCodecs.list()), OnboardingOptionsPayload::clans,
                    OnboardingOptionsPayload::new);

    public OnboardingOptionsPayload {
        clans = List.copyOf(clans);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
