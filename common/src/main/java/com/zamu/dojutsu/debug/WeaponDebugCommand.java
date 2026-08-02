package com.zamu.dojutsu.debug;

import com.mojang.brigadier.context.CommandContext;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.jutsu.kenjutsu.WeaponAttributeService;
import com.zamu.dojutsu.registry.ModItems;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Comando de depuracion de las armas ninja (F4/DJT-082): entrega un arma con el daño ya aplicado a tu nivel de
 * kenjutsu, para validar la reversibilidad (equipar/desequipar sin atributo pegado) y la escala de proficiencia.
 * <ul>
 *   <li>{@code /dojutsuweapon chakra_blade} — Chakra Blade (base +3).</li>
 *   <li>{@code /dojutsuweapon kubikiribocho} — Kubikiribocho (base +8).</li>
 *   <li>{@code /dojutsuweapon samehada} — Samehada (base +6, absorbe chakra).</li>
 *   <li>{@code /dojutsuweapon tanto} — Tanto (base +6, 1000 de durabilidad; F10/DJT-225).</li>
 *   <li>{@code /dojutsuweapon bonus} — muestra tu nivel de kenjutsu y el bonus de proficiencia actual.</li>
 * </ul>
 *
 * <p>Temporal; se retira cuando los items de F8 cubran la entrega real. El tanto ya tiene receta de mesa
 * ({@code data/dojutsu/recipe/tanto.json}); este atajo evita tener que fabricarlo para probar el atributo.
 */
public final class WeaponDebugCommand {

    private WeaponDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsuweapon")
                        .then(Commands.literal("chakra_blade")
                                .executes(ctx -> give(ctx, ModItems.chakraBlade())))
                        .then(Commands.literal("kubikiribocho")
                                .executes(ctx -> give(ctx, ModItems.kubikiribocho())))
                        .then(Commands.literal("samehada")
                                .executes(ctx -> give(ctx, ModItems.samehada())))
                        .then(Commands.literal("tanto")
                                .executes(ctx -> give(ctx, ModItems.tanto())))
                        .then(Commands.literal("bonus").executes(WeaponDebugCommand::bonus))));
    }

    private static int give(CommandContext<CommandSourceStack> ctx, ItemStack weapon)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int level = NinjaData.get(player).progression().skill(Skill.KENJUTSU).level();
        WeaponAttributeService.applyModifiers(weapon, level); // daño acorde al nivel actual
        if (!player.getInventory().add(weapon)) {
            player.drop(weapon, false);
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "arma entregada (kenjutsu " + level + ", bonus +" + WeaponAttributeService.proficiencyBonus(level) + ")"), false);
        return 1;
    }

    private static int bonus(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int level = NinjaData.get(player).progression().skill(Skill.KENJUTSU).level();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "kenjutsu=" + level + " -> bonus dano +" + WeaponAttributeService.proficiencyBonus(level)
                        + ", penalizacion velocidad=" + (WeaponAttributeService.hasSpeedPenalty(level) ? "si (-0.03)" : "no")), false);
        return 1;
    }
}
