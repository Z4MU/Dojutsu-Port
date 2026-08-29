package com.zamu.dojutsu.network;

import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ficha de una tecnica tal como la ve el libro de jutsus (F11/DJT-235). Es la <b>proyeccion de
 * presentacion</b> de una entrada del {@code JutsuRegistry}, que es data-driven y vive en el servidor: en un
 * servidor dedicado el cliente no tiene esos numeros, asi que viajan por red igual que la vista del menu
 * ocular ({@link EyeMenuPayload}).
 *
 * <p><b>No lleva el requisito de nivel</b>, y no es un olvido: ese requisito es la puerta de
 * <b>aprendizaje</b>, y el libro solo muestra lo que ya has aprendido. Mostrarlo seria informacion muerta.
 *
 * @param id           id data-driven de la tecnica
 * @param chakraCost   coste de chakra
 * @param cooldownTicks enfriamiento en ticks
 * @param category     id de categoria/elemento; es por lo que filtra el libro
 * @param affinity     afinidad exigida, o vacio si es universal
 * @param damage       dano declarado en los parametros del efecto, o {@code 0} si esa familia no lo declara.
 *                     Es informativo: no todas las familias hacen dano, y el numero real puede escalar con
 *                     la progresion del jugador
 */
public record JutsuBookEntry(String id, int chakraCost, int cooldownTicks, String category,
        Optional<AffinityView> affinity, float damage) {

    /** Requisito de afinidad, aplanado para la red. */
    public record AffinityView(String nature, int amount) {

        public static final StreamCodec<ByteBuf, AffinityView> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, AffinityView::nature,
                ByteBufCodecs.VAR_INT, AffinityView::amount,
                AffinityView::new);
    }

    public static final StreamCodec<ByteBuf, JutsuBookEntry> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, JutsuBookEntry::id,
            ByteBufCodecs.VAR_INT, JutsuBookEntry::chakraCost,
            ByteBufCodecs.VAR_INT, JutsuBookEntry::cooldownTicks,
            ByteBufCodecs.STRING_UTF8, JutsuBookEntry::category,
            ByteBufCodecs.optional(AffinityView.STREAM_CODEC), JutsuBookEntry::affinity,
            ByteBufCodecs.FLOAT, JutsuBookEntry::damage,
            JutsuBookEntry::new);

    /** Enfriamiento en segundos con un decimal, para pintarlo sin repetir la aritmetica en la Screen. */
    public String cooldownSeconds() {
        long tenths = (cooldownTicks * 10L + 19) / 20;
        return (tenths / 10) + "." + (tenths % 10);
    }
}
