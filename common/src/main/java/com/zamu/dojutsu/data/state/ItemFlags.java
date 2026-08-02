package com.zamu.dojutsu.data.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * Sub-estado de <b>efectos de item que viven en el ninja</b> {@code [S14]} (F8/DJT-170). Sub-estado enchufable de
 * {@link com.zamu.dojutsu.data.PlayerNinjaData}.
 *
 * <h2>Es pequeño a proposito: casi todo ya tenia dueño</h2>
 * La ficha pedia aqui {@code oiled}, {@code maskId}, {@code pills} y {@code dna}. Al abrir el datapack resulta
 * que <b>tres de los cuatro ya estaban modelados</b> en fases anteriores, y duplicarlos habria creado dos fuentes
 * de verdad:
 * <ul>
 *   <li><b>{@code oiled}</b> (aceite de sapo) vive en {@link SageState} desde F6/DJT-135: es un modificador de la
 *       carga de energia natural, no un flag de inventario.</li>
 *   <li><b>{@code dna}</b> no es estado propio: implantar ADN concede naturalezas y KG, que viven en
 *       {@code NatureProfile} desde F3 — incluidas las celulas de Hashirama ({@code hashiramaDna}).</li>
 *   <li><b>{@code pills}</b> son cargas <b>del item</b> ({@code PillCount}), no del jugador; la resaca es un
 *       efecto temporal, no un campo persistido.</li>
 * </ul>
 * Queda un solo campo real: la <b>mascara puesta</b>.
 *
 * <h2>La mascara es estado, no un casco</h2>
 * El datapack la mete en el slot de la cabeza ({@code item replace entity @s armor.head}) y por eso te veta
 * llevar casco. Aqui es <b>estado</b>, la misma decision que ya se tomo con los <b>ojos</b> en F5 — y por la
 * misma razon: un slot de armadura no deberia ser el sitio donde vive la identidad del ninja.
 * <b>Consecuencia asumida:</b> aqui puedes llevar mascara <i>y</i> casco.
 *
 * <p>Value object <b>inmutable</b> (regla 10b): se reemplaza con withers.
 *
 * @param mask id de la mascara puesta ({@code "anbu_1"}, {@code "tobi_1"}); vacio = sin mascara
 */
public record ItemFlags(Optional<String> mask) {

    /** Ninja sin nada puesto. */
    public static final ItemFlags DEFAULT = new ItemFlags(Optional.empty());

    /** Codec del sub-estado; campos ausentes caen a {@link #DEFAULT}. */
    public static final Codec<ItemFlags> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("mask").forGetter(ItemFlags::mask)
    ).apply(instance, ItemFlags::new));

    /** Indica si lleva mascara puesta. */
    public boolean hasMask() {
        return mask.isPresent();
    }

    /** Copia con esa mascara puesta. */
    public ItemFlags withMask(String maskId) {
        return new ItemFlags(Optional.of(maskId));
    }

    /** Copia sin mascara. */
    public ItemFlags withoutMask() {
        return new ItemFlags(Optional.empty());
    }
}
