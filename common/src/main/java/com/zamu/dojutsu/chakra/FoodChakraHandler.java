package com.zamu.dojutsu.chakra;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.items.RamenItem;
import com.zamu.dojutsu.network.NinjaSync;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Reposicion de chakra por comer ({@code chakra/eat_food} del datapack, {@code [S02]}). Al terminar de comer
 * un alimento, repone chakra (con clamp al maximo). Reemplaza el advancement revocado {@code player/eat_food}
 * por el evento real de fin de uso de item.
 *
 * <p>Logica loader-agnostica: la <b>deteccion</b> del fin de comer la hace cada adaptador — NeoForge con el
 * evento {@code LivingEntityUseItemEvent.Finish}; Fabric con un mixin en {@code LivingEntity.completeUsingItem}
 * (fabric-api no expone un evento de fin de uso de item). Ver {@code [ADD/05]}.
 *
 * <h2>El ramen repone 150, no 100 (F10/DJT-225)</h2>
 * Es facil portarlo mal. El ramen es {@code cooked_beef} con {@code Ramen:1b}, y {@code cooked_beef}
 * <b>esta dentro del tag {@code dojutsu:food}</b>, asi que al comerlo se cumplen <b>los dos</b> advancements y
 * corren <b>las dos</b> funciones: {@code chakra/eat_food} (+{@value #FOOD_CHAKRA}) y {@code chakra/eat_ramen}
 * (+{@value #RAMEN_EXTRA_CHAKRA}). El ramen no sustituye la reposicion base: la <b>suma</b>. Leyendo solo
 * {@code eat_ramen} uno concluiria 100 y se quedaria un tercio corto.
 */
public final class FoodChakraHandler {

    /** Chakra repuesto al comer un alimento cualquiera ({@code chakra/eat_food}, {@code [S02]}). */
    public static final int FOOD_CHAKRA = 50;

    /**
     * Chakra <b>adicional</b> del ramen ({@code chakra/eat_ramen}, F10/DJT-225): se suma al base, no lo
     * reemplaza. Un ramen repone {@value #FOOD_CHAKRA} + {@value #RAMEN_EXTRA_CHAKRA} = 150.
     */
    public static final int RAMEN_EXTRA_CHAKRA = 100;

    private FoodChakraHandler() {
    }

    /**
     * Handler de fin de uso de item. Solo repone si el item es <b>comida</b> (tiene componente
     * {@link DataComponents#FOOD}): excluye pociones, leche, etc., fiel a {@code eat_food}.
     */
    public static void onFinishEating(ServerPlayer player, ItemStack stack) {
        if (!stack.has(DataComponents.FOOD)) {
            return; // no es comida (pocion/leche/otros usos): sin reposicion
        }
        PlayerNinjaData data = NinjaData.get(player);
        ChakraService.restore(data, chakraFor(stack)); // clamp al maximo dentro del servicio
        NinjaSync.sendDelta(player);
    }

    /** Chakra que repone ese alimento: el base, y {@value #RAMEN_EXTRA_CHAKRA} mas si es ramen. Puro. */
    public static int chakraFor(ItemStack stack) {
        return stack.getItem() instanceof RamenItem
                ? FOOD_CHAKRA + RAMEN_EXTRA_CHAKRA
                : FOOD_CHAKRA;
    }
}
