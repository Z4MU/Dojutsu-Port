package com.zamu.dojutsu.items;

import com.zamu.dojutsu.jutsu.JutsuLearningService;
import java.util.List;
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
 * "Skill Learner" ({@code [S03]}/{@code [S06]}): item que ensena los jutsus de un elemento. Reemplaza el
 * {@code carrot_on_a_stick{CustomModelData:1000, LearnId:1b}} del datapack por un <b>item real</b> cuyo
 * elemento vive en un data component tipado ({@link LearnerComponent}).
 *
 * <p>El item solo captura el input ({@code use} = click derecho, como el carrot original) y <b>delega en el
 * servidor</b> ({@link JutsuLearningService#use}, F4/DJT-075): posesion del elemento, secuencia de aprendizaje,
 * cooldown de 14 ticks y entrega del jutsu; el ultimo de la secuencia consume el learner.
 */
public final class LearnerItem extends Item {

    public LearnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            JutsuLearningService.use(serverPlayer, stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu.learner.tooltip"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
