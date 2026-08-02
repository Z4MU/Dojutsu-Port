package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor de la forma de jinchuriki (F7/DJT-166). Tres intenciones sobre una sola tecla, en
 * vez de tres items "activator" del datapack: pulsar transforma, <b>agachado</b> lanza la tecnica de la bestia y
 * <b>corriendo</b> alterna el Modo Baryon. El servidor valida bestia, vinculo, chakra y vetos ({@code [ADD/07]}).
 *
 * @param action que quiso hacer el cliente
 */
public record JinchurikiFormPayload(Action action) implements CustomPacketPayload {

    /** Las tres intenciones de la tecla. */
    public enum Action {
        /** Entrar o salir de la forma. */
        TOGGLE_FORM,
        /** Lanzar la tecnica de la bestia (exige forma y vinculo suficiente). */
        CAST_MOVE,
        /** Encender o apagar el Modo Baryon. */
        TOGGLE_BARYON
    }

    public static final Type<JinchurikiFormPayload> TYPE = new Type<>(DojutsuMod.id("jinchuriki_form"));

    public static final StreamCodec<RegistryFriendlyByteBuf, JinchurikiFormPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> Action.values()[id], Action::ordinal), JinchurikiFormPayload::action,
            JinchurikiFormPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
