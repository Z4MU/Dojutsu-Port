package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: seleccionar la habilidad activa del ojo (DJT-117). Reemplaza el
 * "hotbar-as-selector" del datapack (sneak sobre un slot). El cliente envia la <b>intencion</b> (id de
 * habilidad <b>por nombre</b>); el servidor valida que el ojo activo la ofrece y la guarda en
 * {@code EyeState.selectedAbility}, y responde con un {@link EyeMenuPayload} refrescado.
 *
 * @param abilityId id de la habilidad a seleccionar (por nombre, nunca por indice del cliente)
 */
public record SelectEyeAbilityPayload(String abilityId) implements CustomPacketPayload {

    public static final Type<SelectEyeAbilityPayload> TYPE = new Type<>(DojutsuMod.id("select_eye_ability"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectEyeAbilityPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SelectEyeAbilityPayload::abilityId,
                    SelectEyeAbilityPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
