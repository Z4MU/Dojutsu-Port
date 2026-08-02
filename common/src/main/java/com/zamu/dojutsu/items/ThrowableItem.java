package com.zamu.dojutsu.items;

import com.zamu.dojutsu.domain.throwing.ThrowableType;
import com.zamu.dojutsu.throwing.ThrowingService;
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
 * Arma arrojadiza ({@code [S16]}, F8/DJT-180): un item por tipo, porque cada uno vuela y pega distinto.
 *
 * <p><b>Sin componente</b>, a diferencia del ADN o el contrato: el tipo es el propio item y nunca cambia. El
 * kunai del Dios del Trueno es el unico especial y lo es por lo que <b>hace</b>, no por lo que guarda.
 *
 * <p>El item solo captura el input y delega en {@link ThrowingService}. El daño en mano lo pone el atributo del
 * item ({@link ThrowableType#meleeDamage()}) y no este codigo.
 */
public final class ThrowableItem extends Item {

    private final ThrowableType type;

    public ThrowableItem(ThrowableType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    /** Tipo de arma que representa este item. */
    public ThrowableType type() {
        return type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer
                && ThrowingService.throwWeapon(serverPlayer, type, stack)
                && !serverPlayer.isCreative()) {
            stack.shrink(1); // item modify remove_1, salvo en creativo (igual que el datapack)
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu.throwable.tooltip").withStyle(ChatFormatting.BLUE));
        if (type.isMark()) {
            tooltip.add(Component.translatable("item.dojutsu.throwable.mark").withStyle(ChatFormatting.YELLOW));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
