package com.zamu.dojutsu.items;

import com.zamu.dojutsu.registry.ModDataComponents;
import com.zamu.dojutsu.summoning.ContractRegistry;
import com.zamu.dojutsu.summoning.ContractService;
import com.zamu.dojutsu.summoning.SummonService;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Pergamino de <b>contrato de invocacion</b> ({@code [S15]}, F8/DJT-175). Dos gestos sobre un mismo item, como en
 * el datapack:
 * <ul>
 *   <li><b>Golpear a una criatura</b> con un contrato en blanco → intenta firmarlo con esa especie
 *       ({@code attempt_form_contract}).</li>
 *   <li><b>Clic derecho</b> con un contrato firmado → invoca ({@code summon_main}).</li>
 * </ul>
 *
 * <p>El item solo captura el input y delega en {@link ContractService} y {@link SummonService}. Firmar
 * <b>convierte</b> el contrato (rellena su componente) en vez de consumirlo: un contrato firmado es reutilizable,
 * que es justo lo que lo hace valioso.
 */
public final class ContractItem extends Item {

    public ContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
            InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        ContractComponent contract = stack.getOrDefault(ModDataComponents.CONTRACT.get(), ContractComponent.BLANK);
        if (!contract.isBlank()) {
            return InteractionResult.PASS; // ya firmado: golpear no hace nada
        }
        String species = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(target.getType()).getPath();
        if (ContractService.form(serverPlayer, target) == ContractService.FormOutcome.FORMED) {
            stack.set(ModDataComponents.CONTRACT.get(), ContractComponent.of(species));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            ContractComponent contract =
                    stack.getOrDefault(ModDataComponents.CONTRACT.get(), ContractComponent.BLANK);
            if (contract.isBlank()) {
                serverPlayer.sendSystemMessage(Component.translatable("contract.dojutsu.blank")
                        .withStyle(ChatFormatting.RED));
            } else {
                contract.species().flatMap(ContractRegistry::get)
                        .ifPresent(def -> SummonService.summon(serverPlayer, def));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ContractComponent contract = stack.getOrDefault(ModDataComponents.CONTRACT.get(), ContractComponent.BLANK);
        if (contract.isBlank()) {
            tooltip.add(Component.translatable("item.dojutsu.contract.blank").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.dojutsu.contract.tooltip_form").withStyle(ChatFormatting.BLUE));
        } else {
            contract.species().flatMap(ContractRegistry::get).ifPresent(def -> {
                tooltip.add(Component.translatable("item.dojutsu.contract.signed", def.id())
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("summon.dojutsu.tier." + def.tier().id())
                        .withStyle(ChatFormatting.DARK_GRAY));
            });
            tooltip.add(Component.translatable("item.dojutsu.contract.tooltip_summon")
                    .withStyle(ChatFormatting.BLUE));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
