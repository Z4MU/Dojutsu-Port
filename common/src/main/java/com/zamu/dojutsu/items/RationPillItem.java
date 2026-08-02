package com.zamu.dojutsu.items;

import com.zamu.dojutsu.domain.items.PillTable;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Bote de <b>Pildoras de Racion Militar</b> ({@code [S14]}, F8/DJT-170): {@value PillTable#PILLS_PER_ITEM}
 * cargas, una por mordisco.
 *
 * <p>Las cargas van en el <b>daño</b> del item, no en un componente propio: es exactamente para lo que sirve la
 * durabilidad y ademas la pinta la barra vanilla sin escribir nada. El datapack necesitaba <b>cinco items
 * distintos</b> con su {@code CustomModelData} y su lore para representar lo mismo ({@code ration_pills/eat}
 * sustituye el item entero en cada mordisco); aqui es un contador.
 *
 * <p>El item solo captura el input y delega en {@link PillService}, que lleva el chakra, el veto entre mordiscos
 * y la resaca diferida.
 */
public final class RationPillItem extends Item {

    public RationPillItem(Properties properties) {
        super(properties.stacksTo(1).durability(PillTable.PILLS_PER_ITEM));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int left = pillsLeft(stack);
            if (PillService.eat(serverPlayer, left)) {
                stack.setDamageValue(stack.getDamageValue() + 1);
                if (pillsLeft(stack) <= 0) {
                    stack.shrink(1); // bote vacio: se acabo
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Pildoras que le quedan al bote. */
    public static int pillsLeft(ItemStack stack) {
        return PillTable.PILLS_PER_ITEM - stack.getDamageValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu.ration_pills.left", pillsLeft(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.dojutsu.ration_pills.tooltip").withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
