package com.zamu.dojutsu.domain.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Una oferta de un comerciante ninja {@code [S18]} (F8/DJT-190): que vende, por cuantas esmeraldas y cuantas
 * veces. Data-driven, cargada de {@code data/dojutsu/trades/*.json}.
 *
 * <h2>Esto es el riesgo RB-4 en su forma mas pura</h2>
 * El datapack construye las ofertas <b>con NBT literal dentro de la funcion</b>:
 * {@code trades/setup_shinobi} tiene <b>14</b> lineas de {@code data modify ... Offers.Recipes append} y cada una
 * lleva el item entero incrustado —{@code CustomModelData}, lore, atributos, durabilidad— en una sola linea de
 * miles de caracteres. {@code setup_trader} tiene <b>22</b> mas. Cambiar un precio significa editar una linea
 * ilegible; añadir una oferta, copiarla y rezar.
 *
 * <p>Aqui es un JSON de cinco campos y el item se referencia por su <b>id de registro</b>: el objeto ya sabe como
 * es. Un pack puede retocar precios o añadir mercancia <b>sin recompilar</b>.
 *
 * <p><b>Un item desconocido no rompe nada</b>: el catalogo lo declara igual y el servicio lo salta con un aviso.
 * Eso permite tener aqui el catalogo <b>completo del datapack</b> —incluida la mercancia cuyo item aun no existe
 * (bombas de humo, pergaminos, ojos)— y que las ofertas <b>aparezcan solas</b> el dia que el item se implemente,
 * sin tocar codigo.
 *
 * @param item     id de registro de lo que vende ({@code "dojutsu:kunai"}, {@code "minecraft:paper"})
 * @param count    unidades por compra
 * @param price    esmeraldas que cuesta
 * @param maxUses  compras antes de agotarse
 * @param xp       experiencia que gana el comerciante por venta
 */
public record TraderOffer(String item, int count, int price, int maxUses, int xp) {

    /** Codec de la oferta; {@code count} y {@code xp} tienen defaults sensatos. */
    public static final Codec<TraderOffer> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("item").forGetter(TraderOffer::item),
            Codec.INT.optionalFieldOf("count", 1).forGetter(TraderOffer::count),
            Codec.INT.fieldOf("price").forGetter(TraderOffer::price),
            Codec.INT.fieldOf("maxUses").forGetter(TraderOffer::maxUses),
            Codec.INT.optionalFieldOf("xp", 5).forGetter(TraderOffer::xp)
    ).apply(instance, TraderOffer::new));

    /** Sanea valores absurdos de un pack ajeno (precio 0 o usos negativos romperian el trade vanilla). */
    public TraderOffer {
        count = Math.max(count, 1);
        price = Math.max(price, 1);
        maxUses = Math.max(maxUses, 1);
        xp = Math.max(xp, 0);
    }
}
