package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor de las acciones del menu ninja ({@code [S13]}, {@code [ADD/05 §Input]}). El
 * cliente <b>nunca</b> decide gameplay: envia la intencion y el servidor la valida y aplica (DJT-042).
 *
 * <p>Establece el contrato de accion del menu (RM-1): un {@link Action} + los parametros. En F2 la unica accion
 * es {@link Action#SPEND_SKILL} (gastar puntos en una habilidad); las futuras se anaden al enum sin cambiar el
 * canal. Se <b>define</b> aqui (DJT-039) y se <b>registra con handler</b> en DJT-042.
 *
 * @param action  accion solicitada
 * @param skillId habilidad objetivo (nombre en minusculas, p. ej. "taijutsu")
 * @param amount  cantidad (p. ej. puntos a gastar)
 */
public record MenuActionPayload(Action action, String skillId, int amount) implements CustomPacketPayload {

    /** Acciones del menu validadas en servidor. F2: solo gastar puntos. */
    public enum Action {
        SPEND_SKILL
    }

    public static final Type<MenuActionPayload> TYPE = new Type<>(DojutsuMod.id("menu_action"));

    private static final StreamCodec<ByteBuf, Action> ACTION_CODEC = ByteBufCodecs.VAR_INT.map(
            ordinal -> {
                Action[] values = Action.values();
                return (ordinal >= 0 && ordinal < values.length) ? values[ordinal] : Action.SPEND_SKILL;
            },
            Action::ordinal);

    public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> CODEC = StreamCodec.composite(
            ACTION_CODEC, MenuActionPayload::action,
            ByteBufCodecs.STRING_UTF8, MenuActionPayload::skillId,
            ByteBufCodecs.VAR_INT, MenuActionPayload::amount,
            MenuActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
