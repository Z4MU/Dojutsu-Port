package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EmpowermentState;
import com.zamu.dojutsu.domain.cursedseal.CursedSealTable;
import com.zamu.dojutsu.domain.cursedseal.CursedSealType;
import com.zamu.dojutsu.empowerment.cursedseal.CursedSealService;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsuseal} para validar en runtime el sello maldito de F6 (DJT-140) sin depender
 * del azar de la inyeccion (40% de fallar) ni del Injector, que aun no existe como item (llega en F8).
 *
 * <ul>
 *   <li>{@code get} — muestra el sello que portas, su nivel, muertes acumuladas y coste por pulso.</li>
 *   <li>{@code inject} — tira la inyeccion <b>real</b> (60% exito): para probar tambien el camino del fallo.</li>
 *   <li>{@code give &lt;type&gt;} — otorga un sello concreto sin tirar dados (util para comparar auras).</li>
 *   <li>{@code remove} — quita el sello.</li>
 *   <li>{@code level &lt;1-2&gt;} — fija el nivel (2 = aura fuerte y coste <b>menor</b>: 8 en vez de 12).</li>
 *   <li>{@code deaths &lt;n&gt;} — fija las muertes acumuladas (ponlo en 9 para ver la degradacion al morir).</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b> (como {@code /dojutsusage}): se retira cuando el Injector sea obtenible in-game.
 * Muta el estado solo por la puerta {@code PlayerNinjaData.setEmpowerment}.
 */
public final class CursedSealDebugCommand {

    /** Sugiere los seis sellos. */
    private static final SuggestionProvider<CommandSourceStack> TYPES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(CursedSealType.values()).map(CursedSealType::id), builder);

    private CursedSealDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsuseal")
                        .then(Commands.literal("get").executes(CursedSealDebugCommand::get))
                        .then(Commands.literal("inject").executes(CursedSealDebugCommand::inject))
                        .then(Commands.literal("give")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPES)
                                        .executes(CursedSealDebugCommand::give)))
                        .then(Commands.literal("remove").executes(CursedSealDebugCommand::remove))
                        .then(Commands.literal("level")
                                .then(Commands.argument("value",
                                                IntegerArgumentType.integer(1, CursedSealTable.MAX_LEVEL))
                                        .executes(CursedSealDebugCommand::level)))
                        .then(Commands.literal("deaths")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(CursedSealDebugCommand::deaths)))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        EmpowermentState state = NinjaData.get(player).empowerment();
        ctx.getSource().sendSuccess(() -> Component.literal(describe(state, player)), false);
        return 1;
    }

    private static int inject(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return CursedSealService.inject(player).map(type -> {
            ctx.getSource().sendSuccess(() -> Component.literal("inyeccion exitosa: sello " + type.id()), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "inyeccion fallida (" + (100 - CursedSealTable.SUCCESS_CHANCE_PERCENT) + "% de probabilidad)"),
                    false);
            return 1;
        });
    }

    private static int give(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String typeId = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        return CursedSealType.byId(typeId).map(type -> {
            PlayerNinjaData data = NinjaData.get(player);
            data.setEmpowerment(data.empowerment().withCursedSeal(type.id()));
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "sello=" + type.id() + " nivel 1 (fuerza " + type.strength(1) + ", velocidad " + type.speed(1)
                            + ", salto " + type.jumpBoost(1) + ", resistencia " + type.resistance(1) + ")"), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("sello desconocido: " + typeId));
            return 0;
        });
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        data.setEmpowerment(data.empowerment().withoutCursedSeal());
        CursedSealService.forget(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("sello retirado"), false);
        return 1;
    }

    private static int level(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        if (!data.empowerment().hasCursedSeal()) {
            ctx.getSource().sendFailure(Component.literal("no portas ningun sello (usa give o inject antes)"));
            return 0;
        }
        data.setEmpowerment(data.empowerment().withCursedSealLevel(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "nivel del sello=" + value + " (cuesta " + CursedSealTable.chakraCost(value) + " chakra cada "
                        + CursedSealTable.DRAIN_INTERVAL_TICKS + "t)"), false);
        return 1;
    }

    private static int deaths(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        EmpowermentState state = data.empowerment();
        // el estado solo sabe sumar de una en una (el wither modela el evento, no un setter)
        EmpowermentState reset = new EmpowermentState(state.cursedSeal(), state.cursedSealLevel(), value,
                state.gateStrain(), state.soahCharge(), state.soahGranted());
        data.setEmpowerment(reset);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "muertes por agotamiento=" + value + "/" + CursedSealTable.DEATHS_TO_DEGRADE
                        + " (al llegar, el sello degrada a nivel 1 si esta en nivel 2)"), false);
        return 1;
    }

    private static String describe(EmpowermentState state, ServerPlayer player) {
        int level = Math.max(state.cursedSealLevel(), 1);
        return "sello=" + state.cursedSeal().orElse("(ninguno)")
                + " nivel=" + state.cursedSealLevel()
                + " muertes=" + state.cursedSealDeaths() + "/" + CursedSealTable.DEATHS_TO_DEGRADE
                + " activo=" + CursedSealService.isActive(player.getUUID())
                + " | coste " + CursedSealTable.chakraCost(level) + " chakra cada "
                + CursedSealTable.DRAIN_INTERVAL_TICKS + "t";
    }
}
