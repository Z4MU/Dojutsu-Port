package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Servidor -&gt; Cliente con la vista del menu de habilidades oculares (DJT-117): el {@code EyeRegistry}
 * es data-driven y vive en el servidor (en un servidor dedicado el cliente no lo tiene), asi que la Screen
 * recibe la lista de habilidades por red — mismo patron que {@link OnboardingOptionsPayload}.
 *
 * <p>Es <b>solo presentacion</b>: la seleccion vuelve por {@link SelectEyeAbilityPayload} y el servidor la
 * re-valida contra el ojo activo real. Un {@code activeEye} vacio significa "sin ojo activo" (la Screen muestra
 * un aviso y no pinta botones).
 *
 * @param activeEye        id del ojo activo, o cadena vacia si no lleva ninguno
 * @param abilities        ids de las habilidades del ojo activo, en orden de presentacion
 * @param selectedAbility  id de la habilidad seleccionada, o cadena vacia si ninguna
 */
public record EyeMenuPayload(String activeEye, List<String> abilities, String selectedAbility,
        String eternalStatus)
        implements CustomPacketPayload {

    public static final Type<EyeMenuPayload> TYPE = new Type<>(DojutsuMod.id("eye_menu"));

    private static final StreamCodec<ByteBuf, List<String>> ABILITIES_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, EyeMenuPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, EyeMenuPayload::activeEye,
            ABILITIES_CODEC, EyeMenuPayload::abilities,
            ByteBufCodecs.STRING_UTF8, EyeMenuPayload::selectedAbility,
            ByteBufCodecs.STRING_UTF8, EyeMenuPayload::eternalStatus,
            EyeMenuPayload::new);

    public EyeMenuPayload {
        abilities = List.copyOf(abilities);
    }

    /** {@code true} si hay un ojo activo (la Screen debe pintar sus habilidades). */
    public boolean hasActiveEye() {
        return !activeEye.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
