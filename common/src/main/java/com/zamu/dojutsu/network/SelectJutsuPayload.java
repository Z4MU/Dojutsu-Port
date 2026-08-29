package com.zamu.dojutsu.network;

import com.zamu.dojutsu.DojutsuMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Payload Cliente -&gt; Servidor: elegir el jutsu que lanzara la tecla de cast (F11/DJT-231). Es el gemelo de
 * {@link SelectEyeAbilityPayload} para las tecnicas, y la pieza que permite retirar el objeto como via de
 * lanzamiento: antes el jutsu "elegido" era el item que llevaras en la mano.
 *
 * <p>El id viaja <b>por nombre</b> y nunca por indice de la lista del cliente ({@code [ADD/07]}): el orden de
 * la lista es cosa de presentacion, y un indice convertiria un desajuste de sincronizacion en el lanzamiento
 * de una tecnica distinta a la que el jugador pulso. El servidor lo revalida contra los jutsus aprendidos.
 *
 * @param jutsuId id del jutsu a seleccionar. Cadena vacia = <b>retirar</b> la seleccion (la rueda permite
 *                dejar el hueco vacio); asi no hace falta un segundo payload para un caso de un bit
 */
public record SelectJutsuPayload(String jutsuId) implements CustomPacketPayload {

    public static final Type<SelectJutsuPayload> TYPE = new Type<>(DojutsuMod.id("select_jutsu"));

    /** Tope de longitud del id: un jutsuId real son decenas de caracteres, no kilobytes de un cliente hostil. */
    static final int MAX_ID_LENGTH = 128;

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectJutsuPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_ID_LENGTH), SelectJutsuPayload::jutsuId,
            SelectJutsuPayload::new);

    /** Payload que retira la seleccion. */
    public static SelectJutsuPayload clear() {
        return new SelectJutsuPayload("");
    }

    /** Indica si este payload pide retirar la seleccion en vez de elegir un jutsu. */
    public boolean isClear() {
        return jutsuId.isEmpty();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
