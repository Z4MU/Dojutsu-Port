package com.zamu.dojutsu.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Item que se usa <b>soltandolo</b> ({@code [S14]}, F10/DJT-225): la bomba de humo y el sello explosivo suelto.
 *
 * <p>Los dos comparten toda la mecanica de activacion ({@code items/main}:
 * {@code as @e[type=item,predicate=...]}, y los dos predicates exigen {@code OnGround:1b}) y se diferencian solo
 * en <b>que pasa cuando el item toca el suelo</b>. Esa diferencia vive en {@link DropUseService}, que es quien
 * observa la caida; el item en si no tiene comportamiento — no responde al clic derecho, a proposito.
 *
 * <p>El tipo es el propio item, sin componente, como las mascaras y las arrojadizas.
 */
public final class DropUseItem extends Item {

    /** Que hace este item al aterrizar. */
    public enum Kind {
        /** Cortina de humo de {@code items/smoke_bomb}. */
        SMOKE_BOMB,
        /** Mina de {@code items/explosive_tag}. */
        EXPLOSIVE_TAG
    }

    private final Kind kind;

    public DropUseItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    /** Que hace este item al aterrizar. */
    public Kind kind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(kind == Kind.SMOKE_BOMB
                        ? "item.dojutsu.smoke_bomb.tooltip"
                        : "item.dojutsu.explosive_tag.tooltip")
                .withStyle(kind == Kind.SMOKE_BOMB ? ChatFormatting.BLUE : ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
