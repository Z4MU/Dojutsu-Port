package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.soah.SoahTable;
import com.zamu.dojutsu.empowerment.soah.SoahSealService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsusoah} para validar en runtime el Sello de la Fuerza de un Centenar de F6
 * (DJT-150) sin subir la medicina a 375, la afinidad Yin a 150 y el chakra maximo a 20000.
 *
 * <ul>
 *   <li>{@code get} — reserva del sello, si esta activo/curando, y los tres requisitos con tu valor actual.</li>
 *   <li>{@code grant} — te otorga el sello sin esperar al hito de medicina.</li>
 *   <li>{@code store &lt;n&gt;} — fija la reserva directamente (recargar de verdad cuesta 20000 de chakra).</li>
 *   <li>{@code recharge} — hace la recarga <b>real</b>, pagando el chakra.</li>
 *   <li>{@code toggle} — enciende o apaga el sello (lo mismo que la tecla J).</li>
 *   <li>{@code clearcd} — limpia el veto de alternancia (apagarlo deja 600 ticks).</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b>, como los otros tres de F6. El item del sello llega en F8.
 */
public final class SoahDebugCommand {

    private SoahDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsusoah")
                        .then(Commands.literal("get").executes(SoahDebugCommand::get))
                        .then(Commands.literal("grant").executes(SoahDebugCommand::grant))
                        .then(Commands.literal("store")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(SoahDebugCommand::store)))
                        .then(Commands.literal("recharge").executes(SoahDebugCommand::recharge))
                        .then(Commands.literal("toggle").executes(SoahDebugCommand::toggle))
                        .then(Commands.literal("clearcd").executes(SoahDebugCommand::clearCooldown))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        int medical = data.progression().skill(Skill.MEDICAL).level();
        int yin = data.natures().affinity(NatureType.YIN).level();
        int maxChakra = data.chakra().maxChakra();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "sello=" + data.empowerment().soahGranted()
                        + " reserva=" + data.empowerment().soahCharge()
                        + " activo=" + SoahSealService.isActive(player.getUUID())
                        + " chakra=" + data.chakra().chakra() + "/" + maxChakra
                        + " | requisitos: medicina " + medical + "/" + SoahTable.UNLOCK_MEDICAL
                        + ", yin " + yin + "/" + SoahTable.REQUIRED_YIN_AFFINITY
                        + ", chakra maximo " + maxChakra + "/" + SoahTable.REQUIRED_MAX_CHAKRA
                        + " | cura por debajo de " + SoahTable.HEAL_TRIGGER_CHAKRA), false);
        return 1;
    }

    private static int grant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        data.setEmpowerment(data.empowerment().withSoahGranted(true));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "sello otorgado (recargalo con recharge o store antes de usarlo)"), false);
        return 1;
    }

    private static int store(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setEmpowerment(data.empowerment().withSoahCharge(value));
        ctx.getSource().sendSuccess(() -> Component.literal("reserva del sello=" + value), false);
        return 1;
    }

    private static int recharge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = SoahSealService.recharge(player);
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "recargado: -" + SoahTable.RECHARGE_AMOUNT + " de chakra"
                : "no se pudo recargar (mira el aviso en el chat)"), false);
        return ok ? 1 : 0;
    }

    private static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean active = SoahSealService.toggle(player, false);
        ctx.getSource().sendSuccess(() -> Component.literal("sello activo=" + active), false);
        return 1;
    }

    private static int clearCooldown(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SoahSealService.clearCooldown(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("veto limpiado"), false);
        return 1;
    }
}
