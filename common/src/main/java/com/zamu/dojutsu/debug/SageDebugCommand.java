package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.domain.sage.NaturalEnergyTable;
import com.zamu.dojutsu.domain.sage.SageModeTable;
import com.zamu.dojutsu.domain.sage.SageType;
import com.zamu.dojutsu.sage.SageService;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsusage} para validar en runtime el modo sabio de F6 (DJT-135/136) sin
 * esperar las mecanicas time-gated (la carga real tarda 60 s de quietud + 800 ticks por punto en el nivel 0).
 *
 * <ul>
 *   <li>{@code get} — muestra el {@link SageState} actual y los numeros de su nivel.</li>
 *   <li>{@code charge <0-100>} — fija la energia natural (100 = listo para volverse sabio / activar).</li>
 *   <li>{@code become <type>} — convierte en sabio del linaje dado, sin depender del bioma.</li>
 *   <li>{@code level <1-5>} — fija el nivel de sabio (recorre la tabla de drenaje/activacion).</li>
 *   <li>{@code use <n>} — fija el uso acumulado (ponlo cerca del umbral para ver la subida de nivel).</li>
 *   <li>{@code oiled <true|false>} — pone o quita el aceite de sapo (duplica la velocidad de carga).</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b> (como {@code /dojutsudojutsu}): se retira cuando el sage sea plenamente obtenible
 * in-game. Muta el estado solo por la puerta {@code PlayerNinjaData.setSage}.
 */
public final class SageDebugCommand {

    /** Sugiere los linajes de sabio. */
    private static final SuggestionProvider<CommandSourceStack> TYPES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(SageType.values()).map(SageType::id), builder);

    private SageDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsusage")
                        .then(Commands.literal("get").executes(SageDebugCommand::get))
                        .then(Commands.literal("charge")
                                .then(Commands.argument("value",
                                                IntegerArgumentType.integer(0, NaturalEnergyTable.MAX_CHARGE))
                                        .executes(SageDebugCommand::charge)))
                        .then(Commands.literal("become")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPES)
                                        .executes(SageDebugCommand::become)))
                        .then(Commands.literal("level")
                                .then(Commands.argument("value",
                                                IntegerArgumentType.integer(1, NaturalEnergyTable.MAX_SAGE_LEVEL))
                                        .executes(SageDebugCommand::level)))
                        .then(Commands.literal("use")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(SageDebugCommand::use)))
                        .then(Commands.literal("oiled")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(SageDebugCommand::oiled)))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SageState state = NinjaData.get(player).sage();
        int level = Math.max(state.level(), 1);
        ctx.getSource().sendSuccess(() -> Component.literal(describe(state, player)
                + " | nivel " + level + ": activar necesita " + SageModeTable.activationMinimum(level)
                + " energia, gasta 1 cada " + SageModeTable.drainInterval(level) + "t"
                + ", cargar 1 tarda " + NaturalEnergyTable.gatherTicks(state.level(), state.oiled()) + "t"), false);
        return 1;
    }

    private static int charge(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setSage(data.sage().withCharge(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "energia natural=" + data.sage().charge() + "/" + NaturalEnergyTable.MAX_CHARGE
                        + (data.sage().fullyCharged() && !data.sage().sage()
                        ? " (al maximo: quedate quieto un tick para volverte sabio)" : "")), false);
        return 1;
    }

    private static int become(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String typeId = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        return SageType.byId(typeId).map(type -> {
            PlayerNinjaData data = NinjaData.get(player);
            data.setSage(data.sage().becomeSage(type));
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "ahora eres sabio de tipo " + type.id() + " (nivel " + data.sage().level() + ")"), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("linaje desconocido: " + typeId));
            return 0;
        });
    }

    private static int level(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setSage(data.sage().withLevel(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "nivel de sabio=" + data.sage().level()
                        + " (activar necesita " + SageModeTable.activationMinimum(value) + " energia; gasta 1 cada "
                        + SageModeTable.drainInterval(value) + "t)"), false);
        return 1;
    }

    private static int use(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setSage(data.sage().withUse(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "uso del modo=" + data.sage().use() + " (sube de nivel con el modo ACTIVO; requisitos compuestos: "
                        + "chakra maximo + Senjutsu + uso)"), false);
        return 1;
    }

    private static int oiled(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean value = BoolArgumentType.getBool(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setSage(data.sage().withOiled(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "aceite de sapo=" + data.sage().oiled() + " (cargar 1 tarda "
                        + NaturalEnergyTable.gatherTicks(data.sage().level(), value) + "t)"), false);
        return 1;
    }

    private static String describe(SageState state, ServerPlayer player) {
        return "sabio=" + state.sage()
                + " tipo=" + state.type().map(SageType::id).orElse("(ninguno)")
                + " nivel=" + state.level()
                + " energia=" + state.charge()
                + " quieto=" + state.stillTicks() + "t"
                + " uso=" + state.use()
                + " aceite=" + state.oiled()
                + " modo_activo=" + SageService.isActive(player.getUUID());
    }
}
