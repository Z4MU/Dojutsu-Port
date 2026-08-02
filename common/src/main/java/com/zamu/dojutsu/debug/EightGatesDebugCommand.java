package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EmpowermentState;
import com.zamu.dojutsu.domain.eightgates.EightGatesTable;
import com.zamu.dojutsu.domain.eightgates.Gate;
import com.zamu.dojutsu.empowerment.eightgates.EightGatesService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsugates} para validar en runtime las Ocho Puertas de F6 (DJT-145) sin
 * tener que subir el taijutsu a 425 ni esperar los pulsos de fatiga (recuperar 1 punto tarda 200 ticks, o 400
 * si eres del clan Lee).
 *
 * <ul>
 *   <li>{@code get} — puerta abierta, fatiga, puerta maxima segun tu taijutsu, maestria y los dos vetos.</li>
 *   <li>{@code open} — abre la siguiente puerta (lo mismo que la tecla H).</li>
 *   <li>{@code close} — las cierra todas, cobrando el coste de cierre.</li>
 *   <li>{@code strain &lt;0-100&gt;} — fija la fatiga (ponla en 99 para ver el cierre forzado al siguiente pulso).</li>
 *   <li>{@code clearcd} — limpia los dos vetos para poder encadenar aperturas al instante.</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b> (como {@code /dojutsusage} y {@code /dojutsuseal}). Para desbloquear puertas usa
 * el comando de progresion: la puerta maxima se <b>deriva</b> del taijutsu, no se guarda.
 */
public final class EightGatesDebugCommand {

    private EightGatesDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsugates")
                        .then(Commands.literal("get").executes(EightGatesDebugCommand::get))
                        .then(Commands.literal("open").executes(EightGatesDebugCommand::open))
                        .then(Commands.literal("close").executes(EightGatesDebugCommand::close))
                        .then(Commands.literal("strain")
                                .then(Commands.argument("value",
                                                IntegerArgumentType.integer(0, EmpowermentState.MAX_GATE_STRAIN))
                                        .executes(EightGatesDebugCommand::strain)))
                        .then(Commands.literal("clearcd").executes(EightGatesDebugCommand::clearCooldowns))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        int taijutsu = data.progression().skill(com.zamu.dojutsu.data.state.Skill.TAIJUTSU).level();
        int maxGate = EightGatesTable.maxGate(taijutsu);
        boolean mastered = EightGatesTable.hasMastery(taijutsu);
        int stage = EightGatesService.stageOf(player);
        String open = Gate.forStage(stage).map(Gate::id).orElse("(cerradas)");
        ctx.getSource().sendSuccess(() -> Component.literal(
                "puerta=" + open + " (" + stage + ")"
                        + " fatiga=" + data.empowerment().gateStrain() + "/" + EmpowermentState.MAX_GATE_STRAIN
                        + " taijutsu=" + taijutsu
                        + " max=" + maxGate + "/" + EmpowermentState.MAX_GATES
                        + " maestria=" + mastered
                        + " | encadenar tarda " + EightGatesTable.openCooldownTicks(maxGate, mastered) + "t"
                        + ", primer mordisco " + EightGatesTable.firstStrainTickTicks(maxGate, mastered) + "t"
                        + ", luego 1 cada " + EightGatesTable.STRAIN_PULSE_TICKS + "t"), false);
        return 1;
    }

    private static int open(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean opened = EightGatesService.openNext(player);
        ctx.getSource().sendSuccess(() -> Component.literal(opened
                ? "abierta la puerta " + EightGatesService.stageOf(player)
                : "no se pudo abrir (mira el aviso en pantalla)"), false);
        return opened ? 1 : 0;
    }

    private static int close(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EightGatesService.close(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "puertas cerradas (veto de " + EightGatesTable.CLOSE_COOLDOWN_TICKS + "t)"), false);
        return 1;
    }

    private static int strain(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setEmpowerment(data.empowerment().withGateStrain(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "fatiga corporal=" + value
                        + (value >= EightGatesTable.FORCE_CLOSE_AT ? " (cierre forzado en el proximo tick)"
                        : value >= EightGatesTable.PUNISH_HARD_AT ? " (castigo severo activo)"
                        : value >= EightGatesTable.PUNISH_AT ? " (castigo leve activo)" : "")), false);
        return 1;
    }

    private static int clearCooldowns(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EightGatesService.clearCooldowns(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("vetos limpiados"), false);
        return 1;
    }
}
