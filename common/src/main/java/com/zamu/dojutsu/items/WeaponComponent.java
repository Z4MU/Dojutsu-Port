package com.zamu.dojutsu.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component de un arma ninja (F4/DJT-082, {@code [S07]}): que arma es, el daño base y el nivel de
 * kenjutsu que exigen sus tecnicas. Reemplaza el multiplexado por NBT del datapack
 * ({@code iron_sword{ChakraBlade:1b,...}}) por un componente tipado.
 *
 * <p>El <b>bonus de daño real</b> del arma NO vive aqui: lo escribe {@code WeaponAttributeService} en el
 * {@code ItemAttributeModifiers} vanilla del stack (daño base + proficiencia de kenjutsu), que MC aplica y
 * <b>revierte solo</b> al equipar/desequipar — reversible por construccion (corrige S07-R2, el "atributo
 * pegado" del {@code dmg_calc}/{@code remove_attributes} manual del datapack).
 *
 * @param weaponType  id del arma ({@code "chakra_blade"}, {@code "kubikiribocho"}...)
 * @param kenjutsuReq nivel de kenjutsu que exigen sus tecnicas de espada (DJT-083); 0 = ninguno
 * @param baseDamage  daño de ataque propio del arma (Operation ADD, como los {@code give/weapons/*})
 */
public record WeaponComponent(String weaponType, int kenjutsuReq, double baseDamage) {

    public static final Codec<WeaponComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("weaponType").forGetter(WeaponComponent::weaponType),
            Codec.INT.optionalFieldOf("kenjutsuReq", 0).forGetter(WeaponComponent::kenjutsuReq),
            Codec.DOUBLE.optionalFieldOf("baseDamage", 0.0D).forGetter(WeaponComponent::baseDamage)
    ).apply(instance, WeaponComponent::new));

    public static final StreamCodec<ByteBuf, WeaponComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WeaponComponent::weaponType,
            ByteBufCodecs.VAR_INT, WeaponComponent::kenjutsuReq,
            ByteBufCodecs.DOUBLE, WeaponComponent::baseDamage,
            WeaponComponent::new);

    public WeaponComponent {
        Objects.requireNonNull(weaponType, "weaponType");
        weaponType = weaponType.toLowerCase(Locale.ROOT);
    }
}
