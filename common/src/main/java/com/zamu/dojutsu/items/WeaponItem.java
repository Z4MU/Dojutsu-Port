package com.zamu.dojutsu.items;

import net.minecraft.world.item.Item;

/**
 * Arma ninja (F4/DJT-082, {@code [S07]}): item con {@link WeaponComponent}. El daño se aplica por
 * {@code ItemAttributeModifiers} vanilla (via {@code WeaponAttributeService}), no por el item — reversible por
 * construccion. El item en si solo porta el tipo; las tecnicas de espada son jutsus de categoria kenjutsu
 * (DJT-083) y el comportamiento especial de la Samehada es DJT-084.
 */
public final class WeaponItem extends Item {

    public WeaponItem(Properties properties) {
        super(properties);
    }
}
