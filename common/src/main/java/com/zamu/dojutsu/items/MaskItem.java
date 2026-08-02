package com.zamu.dojutsu.items;

import com.zamu.dojutsu.domain.items.MaskType;
import java.util.List;
import java.util.Locale;
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
 * Mascara ({@code [S14]}, F8/DJT-170): un item por mascara, porque lo unico que las distingue es cual eres.
 *
 * <p>A diferencia del ADN, aqui <b>no hay componente</b>: el tipo es el propio item. Un componente solo se paga
 * cuando el estado varia dentro de un mismo objeto (el ADN se identifica, la mascara no cambia nunca).
 *
 * <p>Ponersela <b>no ocupa el casco</b> (ver {@link MaskService}); el item se queda en tu inventario y quitartela
 * es volver a hacer clic.
 */
public final class MaskItem extends Item {

    private final MaskType mask;

    public MaskItem(MaskType mask, Properties properties) {
        super(properties);
        this.mask = mask;
    }

    /** Mascara que representa este item. */
    public MaskType mask() {
        return mask;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            boolean wearingThis = MaskService.worn(serverPlayer).filter(worn -> worn == mask).isPresent();
            if (wearingThis) {
                MaskService.unequip(serverPlayer);
            } else {
                MaskService.equip(serverPlayer, mask);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mask.dojutsu.type." + mask.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.dojutsu.mask.tooltip").withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
