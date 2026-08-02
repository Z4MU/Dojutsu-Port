package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor de las elecciones de la creacion del ninja ({@code [S01]}, DJT-059). El cliente
 * (la Screen de DJT-060) <b>nunca</b> avanza la fase: envia la intencion y el {@code OnboardingController} la
 * valida contra la fase actual y los registros — un payload de fase incorrecta se rechaza sin efecto.
 *
 * @param kind   que se esta eligiendo (clan o afinidad)
 * @param choice id de la eleccion <b>por nombre</b> ({@code "uchiha"}, {@code "fire"}, {@code "random"});
 *               nunca un indice del cliente
 */
public record OnboardingChoicePayload(Kind kind, String choice) implements CustomPacketPayload {

    /** Que se elige en esta fase del onboarding. */
    public enum Kind {
        CLAN,
        AFFINITY
    }

    public static final Type<OnboardingChoicePayload> TYPE = new Type<>(DojutsuMod.id("onboarding_choice"));

    private static final StreamCodec<ByteBuf, Kind> KIND_CODEC = ByteBufCodecs.VAR_INT.map(
            ordinal -> {
                Kind[] values = Kind.values();
                return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : Kind.CLAN;
            },
            Kind::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, OnboardingChoicePayload> CODEC = StreamCodec.composite(
            KIND_CODEC, OnboardingChoicePayload::kind,
            ByteBufCodecs.STRING_UTF8, OnboardingChoicePayload::choice,
            OnboardingChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
