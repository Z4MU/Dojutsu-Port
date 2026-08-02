package com.zamu.dojutsu.items;

import com.zamu.dojutsu.domain.items.ScrollType;
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
 * Pergamino de un solo uso ({@code [S14]}, F10/DJT-225): un item por {@link ScrollType}, porque cada uno
 * restaura una cosa distinta.
 *
 * <p><b>Sin componente</b>, como las mascaras o las arrojadizas: el tipo es el propio item y nunca cambia. El
 * item solo captura el input y delega en {@link ScrollService}, que decide si el uso prende y quien paga.
 */
public final class ScrollItem extends Item {

    private final ScrollType type;

    public ScrollItem(ScrollType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    /** Pergamino que representa este item. */
    public ScrollType type() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer
                && ScrollService.use(serverPlayer, type)
                && !serverPlayer.isCreative()) {
            stack.shrink(1); // clear @s ... 1, salvo en creativo (igual que el datapack)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu." + type.itemPath() + ".tooltip")
                .withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
