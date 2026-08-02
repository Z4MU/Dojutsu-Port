package com.zamu.dojutsu.world.crafting;

import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.crafting.CraftingTable;
import com.zamu.dojutsu.domain.items.ScrollType;
import com.zamu.dojutsu.items.ContractComponent;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.registry.ModDataComponents;
import com.zamu.dojutsu.registry.ModItems;
import dev.architectury.event.events.common.PlayerEvent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * La <b>puerta de habilidad</b> de las recetas de mesa {@code [S23]} (F9/DJT-206; ampliada a las cuatro en
 * F10/DJT-225).
 *
 * <h2>Lo que la ficha no contaba</h2>
 * Cuatro de las 13 recetas del datapack no entregan el item sin mas: {@code crafting/summoning_contract},
 * {@code medical_scroll}, {@code removal_scroll} y {@code sage_scroll} comprueban una habilidad y, si falta,
 * llaman a {@code crafting/failed}, que <b>devuelve los ingredientes</b> ({@code loot_tables/crafting/items_*}),
 * avisa en rojo y suena un yunque. DJT-206 solo pudo cablear el contrato porque los tres pergaminos no existian
 * como item; DJT-225 los crea y cierra la puerta entera.
 *
 * <h2>Por que se deshace el crafteo en vez de impedirlo</h2>
 * Una {@code Recipe} de Minecraft <b>no ve al jugador</b>: {@code matches(CraftingInput, Level)} no lo recibe, y
 * el unico punto comun a los dos loaders donde aparece es {@code PlayerEvent.CRAFT_ITEM}, que salta cuando el
 * resultado ya esta en manos del jugador. Asi que se hace <b>exactamente lo que hace el datapack</b>: el crafteo
 * ocurre y despues se deshace —se quita el item recien hecho y se devuelven los ingredientes—, con el mismo
 * aviso y el mismo yunque. El balance de items es cero.
 *
 * <p>El item retirado puede no ser el recien crafteado sino otro que el jugador ya tuviera: son indistinguibles
 * (para el contrato, cualquiera sin firmar). Lo que importa es la cuenta, y la cuenta cuadra.
 *
 * <h2>El pergamino de sabio ademas cobra</h2>
 * Es el unico de los cuatro que <b>gasta</b> al tener exito: {@value CraftingTable#SAGE_SCROLL_CHARGE_COST} de
 * energia natural ({@code scoreboard players remove @s d.sagechakra 50}). Y solo cobra en la rama que salio
 * bien: un crafteo rechazado no cuesta energia.
 */
public final class CraftingGateService {

    /** Ticks entre avisos por jugador: al crafteo con mayusculas (shift-click) le salta la puerta muchas veces. */
    private static final long WARN_COOLDOWN_TICKS = 20L;

    private static final Map<UUID, Long> LAST_WARNED = new ConcurrentHashMap<>();

    private CraftingGateService() {
    }

    /** Suscribe la puerta al crafteo. Llamar una sola vez durante la inicializacion. */
    public static void register() {
        PlayerEvent.CRAFT_ITEM.register(CraftingGateService::onCraft);
    }

    /** Olvida el anti-spam del aviso al desconectar. */
    public static void forget(UUID playerId) {
        LAST_WARNED.remove(playerId);
    }

    private static void onCraft(Player player, ItemStack constructed, Container grid) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (constructed.is(ModItems.CONTRACT.get())) {
            gateContract(serverPlayer, constructed);
            return;
        }
        for (ScrollType type : ScrollType.values()) {
            if (constructed.is(ModItems.SCROLLS.get(type).get())) {
                gateScroll(serverPlayer, constructed, type);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Las cuatro puertas
    // ------------------------------------------------------------------

    private static void gateContract(ServerPlayer player, ItemStack constructed) {
        int fuinjutsu = NinjaData.get(player).progression().skill(Skill.FUINJUTSU).level();
        if (CraftingTable.canCraftContract(fuinjutsu)) {
            return;
        }
        if (!undo(player, constructed, ModItems.CONTRACT.get(), CraftingGateService::isBlankContract)) {
            return; // no se encontro nada que retirar: no se devuelve nada, para no regalar ingredientes
        }
        Inventory inventory = player.getInventory();
        inventory.placeItemBackInInventory(new ItemStack(Items.PAPER, CraftingTable.CONTRACT_REFUND_PAPER));
        inventory.placeItemBackInInventory(new ItemStack(Items.INK_SAC, CraftingTable.CONTRACT_REFUND_INK));
        warn(player, "crafting.dojutsu.low_fuinjutsu", CraftingTable.CONTRACT_FUINJUTSU);
    }

    private static void gateScroll(ServerPlayer player, ItemStack constructed, ScrollType type) {
        PlayerNinjaData data = NinjaData.get(player);
        Item item = ModItems.SCROLLS.get(type).get();
        if (allows(data, type)) {
            charge(player, data, type);
            return;
        }
        if (!undo(player, constructed, item, stack -> stack.is(item))) {
            return;
        }
        refund(player, refundOf(type));
        switch (type) {
            case MEDICAL -> warn(player, "crafting.dojutsu.low_medical", CraftingTable.MEDICAL_SCROLL_MEDICAL);
            case REMOVAL -> warn(player, "crafting.dojutsu.low_fuinjutsu", CraftingTable.REMOVAL_SCROLL_FUINJUTSU);
            case SAGE -> warn(player, "crafting.dojutsu.not_sage_enough", CraftingTable.SAGE_SCROLL_SENJUTSU);
        }
    }

    /** Si el estado del jugador satisface la puerta del pergamino. */
    private static boolean allows(PlayerNinjaData data, ScrollType type) {
        return switch (type) {
            case MEDICAL -> CraftingTable.canCraftMedicalScroll(data.progression().skill(Skill.MEDICAL).level());
            case REMOVAL -> CraftingTable.canCraftRemovalScroll(data.progression().skill(Skill.FUINJUTSU).level());
            case SAGE -> CraftingTable.canCraftSageScroll(data.sage().sage(),
                    data.progression().skill(Skill.SENJUTSU).level(), data.sage().charge());
        };
    }

    /** Cobro del crafteo con exito: solo el pergamino de sabio cobra, y solo si salio bien. */
    private static void charge(ServerPlayer player, PlayerNinjaData data, ScrollType type) {
        if (type != ScrollType.SAGE) {
            return;
        }
        SageState sage = data.sage();
        data.setSage(sage.withCharge(sage.charge() - CraftingTable.SAGE_SCROLL_CHARGE_COST));
        NinjaSync.sendDelta(player);
    }

    private static List<CraftingTable.Refund> refundOf(ScrollType type) {
        return switch (type) {
            case MEDICAL -> CraftingTable.MEDICAL_SCROLL_REFUND;
            case REMOVAL -> CraftingTable.REMOVAL_SCROLL_REFUND;
            case SAGE -> CraftingTable.SAGE_SCROLL_REFUND;
        };
    }

    private static void refund(ServerPlayer player, List<CraftingTable.Refund> refunds) {
        Inventory inventory = player.getInventory();
        for (CraftingTable.Refund refund : refunds) {
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(refund.itemId()))
                    .ifPresent(item -> inventory.placeItemBackInInventory(new ItemStack(item, refund.count())));
        }
    }

    // ------------------------------------------------------------------
    // Retirada del resultado
    // ------------------------------------------------------------------

    /**
     * Retira <b>una</b> unidad del resultado de donde haya quedado: el cursor (clic normal) o el inventario
     * (shift-click, donde el stack del evento ya viene vacio).
     */
    private static boolean undo(ServerPlayer player, ItemStack constructed, Item item,
            java.util.function.Predicate<ItemStack> matches) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null && menu.getCarried() == constructed && matches.test(constructed)) {
            constructed.shrink(1);
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (matches.test(stack)) {
                stack.shrink(1);
                return true;
            }
        }
        if (menu != null && matches.test(menu.getCarried())) {
            menu.getCarried().shrink(1);
            return true;
        }
        return false;
    }

    private static boolean isBlankContract(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.CONTRACT.get())
                && stack.getOrDefault(ModDataComponents.CONTRACT.get(), ContractComponent.BLANK).isBlank();
    }

    /** Aviso rojo + yunque, como {@code crafting/failed}, sin repetirlo una vez por unidad crafteada. */
    private static void warn(ServerPlayer player, String reasonKey, int requirement) {
        long now = player.level().getGameTime();
        Long last = LAST_WARNED.get(player.getUUID());
        if (last != null && now - last < WARN_COOLDOWN_TICKS) {
            return;
        }
        LAST_WARNED.put(player.getUUID(), now);
        player.sendSystemMessage(Component.translatable("crafting.dojutsu.failed").withStyle(ChatFormatting.DARK_RED)
                .append(Component.translatable(reasonKey, requirement).withStyle(ChatFormatting.RED)));
        player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3F, 1.0F);
    }
}
