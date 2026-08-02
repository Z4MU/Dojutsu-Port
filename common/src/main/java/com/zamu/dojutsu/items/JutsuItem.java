package com.zamu.dojutsu.items;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.jutsu.JutsuCastPipeline;
import com.zamu.dojutsu.registry.ModDataComponents;
import com.zamu.dojutsu.registry.ModItems;
import java.util.List;
import net.minecraft.core.component.DataComponents;
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
 * Item de jutsu (F4/DJT-075): porta una tecnica aprendida, identificada por
 * {@link JutsuComponent}. Reemplaza el {@code carrot_on_a_stick{JutsuItem:1b, JutsuId:Nb}} del
 * datapack por un item real con componente tipado.
 *
 * <p>Se lanza de dos formas equivalentes, ambas <b>validadas en servidor</b>: la tecla CastJutsu
 * (DJT-080, {@code CastJutsuPayload}) y el click derecho ({@link #use}, corre server-side, lee su
 * propio componente y valida {@code known}). El cliente nunca decide el resultado; el
 * {@code JutsuCastPipeline} aplica las puertas de chakra/cd/afinidad. El datapack usaba click
 * derecho; la tecla es la modernizacion del ADD (RM-1).
 */
public final class JutsuItem extends Item {

    public JutsuItem(Properties properties) {
        super(properties);
    }

    /**
     * Crea el ItemStack de un jutsu: componente tipado + nombre localizado
     * ({@code jutsu.dojutsu.<id>}).
     */
    public static ItemStack createStack(String jutsuId) {
        ItemStack stack = new ItemStack(ModItems.JUTSU.get());
        JutsuComponent component = new JutsuComponent(jutsuId);
        stack.set(ModDataComponents.JUTSU.get(), component);
        stack.set(DataComponents.ITEM_NAME, Component.translatable("jutsu.dojutsu." + component.jutsuId()));
        // Modelo por tecnica: custom_model_data (predicado vanilla) → el modelo base lo mapea (DJT-085b).
        int modelData = JutsuModelData.of(component.jutsuId());
        if (modelData > 0) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                    new net.minecraft.world.item.component.CustomModelData(modelData));
        }
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            JutsuComponent component = stack.get(ModDataComponents.JUTSU.get());
            // Solo casteable si se conoce (item por comando/residuo no cuenta); el pipeline aplica las puertas.
            if (component != null && NinjaData.get(serverPlayer).jutsu().knows(component.jutsuId())) {
                JutsuCastPipeline.cast(serverPlayer, component.jutsuId());
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu.jutsu.tooltip"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
