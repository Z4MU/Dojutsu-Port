package com.zamu.dojutsu.items;

import com.zamu.dojutsu.bijuu.entity.TailedBeastEntity;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Huevo de invocacion de <b>Bestia con Cola</b> ({@code [S09]}/{@code [S23]}, F10/DJT-225): un item por bestia,
 * como el datapack ({@code give/tailed_beast/spawn_egg/*}).
 *
 * <h2>Por que no es un {@code SpawnEggItem} vanilla</h2>
 * El huevo vanilla invoca <b>un tipo de entidad</b>, y las nueve bestias son <b>la misma</b>
 * {@code EntityType} con una identidad distinta dentro. Un {@code SpawnEggItem} las invocaria todas iguales
 * (saldria siempre Shukaku) salvo cargandole un {@code EntityTag} — que es exactamente el rodeo que hacia el
 * datapack, obligado a colgar la identidad de un {@code polar_bear_spawn_egg} con NBT. Aqui el tipo <b>es</b> el
 * item, como las mascaras y las arrojadizas.
 *
 * <h2>Nueve, no diez</h2>
 * El datapack define diez huevos pero <b>solo nueve tienen receta</b> ({@code main/internal/crafting/tb/*}): el
 * del Diez Colas se obtiene unicamente por comando. Aqui esos nueve son items y el Diez Colas sigue en
 * {@code /dojutsubeast summon ten_tails}, que es su equivalente exacto.
 *
 * <h2>Se invoca al usarlo sobre un bloque</h2>
 * Como cualquier huevo: la bestia aparece encima de la cara clicada. No hay comprobacion de que esa bestia ya
 * exista en el mundo — el datapack tampoco la hace, y el registro global de F7 solo controla el <b>sellado</b>,
 * no cuantas hay sueltas.
 */
public final class BeastSpawnEggItem extends Item {

    private final TailedBeast beast;

    public BeastSpawnEggItem(TailedBeast beast, Properties properties) {
        super(properties);
        this.beast = beast;
    }

    /** Bestia que invoca este huevo. */
    public TailedBeast beast() {
        return beast;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        BlockPos spot = context.getClickedPos().relative(context.getClickedFace());
        TailedBeastEntity summoned = TailedBeastEntity.summon(level, beast,
                spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
        if (summoned == null) {
            return InteractionResult.FAIL;
        }
        if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.dojutsu.spawn_egg.tooltip").withStyle(ChatFormatting.BLUE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
