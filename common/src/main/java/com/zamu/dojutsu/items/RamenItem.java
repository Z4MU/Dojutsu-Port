package com.zamu.dojutsu.items;

import com.zamu.dojutsu.chakra.FoodChakraHandler;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * <b>Ramen</b> ({@code [S14]}/{@code [S02]}, F10/DJT-225): comida que repone chakra de mas.
 *
 * <p>No tiene comportamiento propio: es comida vanilla y quien la nota es {@link FoodChakraHandler}, que
 * reconoce el item al terminar de comerlo. Existe como clase —en vez de dos {@code Item} pelados— precisamente
 * para eso: el reconocimiento es {@code instanceof}, no una lista de ids que haya que acordarse de ampliar el dia
 * que llegue un tercer ramen.
 *
 * <p>Los dos ramen del datapack ({@code cooked_beef{Ramen:1b}}, con {@code CustomModelData} 100 y 101) reponen
 * <b>lo mismo</b>: el shoyu solo cuesta un alga mas de craftear. La unica diferencia real es el modelo.
 */
public final class RamenItem extends Item {

    public RamenItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu.ramen.tooltip",
                FoodChakraHandler.FOOD_CHAKRA + FoodChakraHandler.RAMEN_EXTRA_CHAKRA)
                .withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
