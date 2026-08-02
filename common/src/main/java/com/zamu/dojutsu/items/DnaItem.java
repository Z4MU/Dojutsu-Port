package com.zamu.dojutsu.items;

import com.zamu.dojutsu.domain.items.DnaTable;
import com.zamu.dojutsu.registry.ModDataComponents;
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
 * Muestra de <b>ADN</b> ({@code [S14]}, F8/DJT-170). Un solo item para los 27 estados posibles: sin identificar,
 * o identificado como cualquiera de los 26 tipos ({@link DnaComponent}).
 *
 * <p><b>El clic derecho hace lo que toque</b>: si no esta identificado, lo identifica; si ya lo esta, intenta
 * implantarlo. Es el mismo gesto del datapack sobre dos items distintos, aqui unificado porque el componente ya
 * distingue el estado.
 *
 * <p>El item solo captura el input y <b>delega en el servidor</b> ({@link DnaService}). Lo unico que decide aqui
 * es si el stack se consume: identificar y fallar lo gastan; <b>que ya lo tengas, no</b> ({@code idna_alr}).
 */
public final class DnaItem extends Item {

    public DnaItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            DnaComponent dna = stack.getOrDefault(ModDataComponents.DNA.get(), DnaComponent.UNIDENTIFIED);
            if (dna.isUnidentified()) {
                DnaService.identify(serverPlayer).ifPresent(revealed -> {
                    // identificar no gasta el ADN: lo convierte (get_id sustituye el DNAId:0b por el suyo)
                    stack.set(ModDataComponents.DNA.get(), DnaComponent.of(revealed));
                });
            } else {
                dna.resolved().ifPresent(type -> {
                    DnaTable.ImplantOutcome outcome = DnaService.implant(serverPlayer, type);
                    boolean consumes = outcome == DnaTable.ImplantOutcome.SUCCESS
                            || outcome == DnaTable.ImplantOutcome.FAILURE;
                    if (consumes && !serverPlayer.isCreative()) {
                        stack.shrink(1);
                    }
                });
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DnaComponent dna = stack.getOrDefault(ModDataComponents.DNA.get(), DnaComponent.UNIDENTIFIED);
        if (dna.isUnidentified()) {
            tooltip.add(Component.translatable("item.dojutsu.dna.unidentified").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.dojutsu.dna.tooltip_identify").withStyle(ChatFormatting.BLUE));
        } else {
            dna.resolved().ifPresent(type -> tooltip.add(
                    Component.translatable("dna.dojutsu.type." + type.id()).withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.translatable("item.dojutsu.dna.tooltip_implant").withStyle(ChatFormatting.BLUE));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
