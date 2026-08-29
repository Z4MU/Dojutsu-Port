package com.zamu.dojutsu.items;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.jutsu.JutsuSelectionService;
import com.zamu.dojutsu.jutsu.JutsuSelectionService.SelectOutcome;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.registry.ModDataComponents;
import com.zamu.dojutsu.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
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
 * <p><b>Ya no es una via de lanzamiento (F11/DJT-232).</b> El item es el <b>pergamino</b> de la tecnica:
 * prueba de que la aprendiste, botin, y objeto de intercambio. Quien lanza es el jugador, no lo que lleve en
 * la mano: la tecla de cast lanza el jutsu <b>seleccionado</b> en su estado ({@code JutsuKnowledge}).
 *
 * <p><b>Que hace ahora el click derecho:</b> <b>selecciona</b> este jutsu, por la misma puerta de servidor
 * que usa la rueda ({@code JutsuSelectionService}). Quitar el cast sin poner nada dejaria un pergamino que
 * no responde al usarlo, y seleccionar es lo que un jugador espera de "usar" una tecnica que no lanza. No
 * abre una segunda via de lanzamiento: si no conoces el jutsu, no se selecciona, y el cast sigue estando
 * donde estaba.
 *
 * <p><b>Historico:</b> hasta F11 el click derecho casteaba (era el gesto del datapack) y la tecla leia el
 * item de la mano (DJT-080). Las dos vias pasaban por el {@code JutsuCastPipeline}; ninguna existe ya.
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
            if (component != null) {
                // Seleccionar, no lanzar (F11/DJT-232). El servicio rechaza lo no aprendido, asi que un
                // pergamino dado por comando o residual sigue sin servir de nada.
                select(serverPlayer, component.jutsuId());
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Selecciona el jutsu del pergamino y avisa al jugador; el estado real manda (no el item). */
    private static void select(ServerPlayer player, String jutsuId) {
        SelectOutcome outcome = JutsuSelectionService.select(NinjaData.get(player), jutsuId);
        Component name = Component.translatable("jutsu.dojutsu." + jutsuId);
        switch (outcome) {
            case SELECTED -> {
                player.displayClientMessage(Component.translatable("jutsu.dojutsu.selected", name)
                        .withStyle(ChatFormatting.AQUA), true);
                NinjaSync.sendDelta(player); // la seleccion es observable
            }
            case NOT_KNOWN -> player.displayClientMessage(
                    Component.translatable("jutsu.dojutsu.not_learned", name).withStyle(ChatFormatting.RED), true);
            default -> {
                // ya lo tenia seleccionado: sin mensaje ni trafico, igual que en la rueda
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu.jutsu.tooltip"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
