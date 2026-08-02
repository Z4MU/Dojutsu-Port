package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.domain.story.SagePath;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: "elijo este camino del Sabio" (F9/DJT-200). Reemplaza los <b>triggers</b> de
 * scoreboard del datapack ({@code yin_path}/{@code yang_path}/{@code no_path}) por un payload tipado.
 *
 * <p>El servidor <b>revalida</b> que haya un encuentro abierto antes de conceder nada ({@code [ADD/07]}): un
 * cliente que invente este payload sin encuentro no consigue poderes de Seis Caminos.
 *
 * @param path camino elegido
 */
public record PathChoicePayload(SagePath path) implements CustomPacketPayload {

    public static final Type<PathChoicePayload> TYPE = new Type<>(DojutsuMod.id("path_choice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PathChoicePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(id -> SagePath.values()[id], SagePath::ordinal), PathChoicePayload::path,
            PathChoicePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
