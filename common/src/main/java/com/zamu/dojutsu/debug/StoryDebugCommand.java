package com.zamu.dojutsu.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.SixPathsState;
import com.zamu.dojutsu.data.state.StoryState;
import com.zamu.dojutsu.domain.story.SageEncounter;
import com.zamu.dojutsu.domain.story.SagePath;
import com.zamu.dojutsu.domain.sixpaths.SixPathsTable;
import com.zamu.dojutsu.domain.story.StoryTable;
import com.zamu.dojutsu.story.SageEncounterService;
import com.zamu.dojutsu.sixpaths.SixPathsService;
import com.zamu.dojutsu.story.SagePathService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsustory} para validar en runtime los encuentros con los Sabios de F9
 * (DJT-200) sin esperar a la cola global de {@value StoryTable#QUEUE_SECONDS} s ni a sacar un 5%.
 *
 * <ul>
 *   <li>{@code get} — tu sabio, la etapa, los intentos fallidos, las mitades y la probabilidad actual.</li>
 *   <li>{@code try} — <b>tira</b> el encuentro como haria la cola (respeta clan, rango y azar).</li>
 *   <li>{@code force} — se aparece <b>sin tirar</b> y abre el dialogo (sigue exigiendo un clan con sabio).</li>
 *   <li>{@code path &lt;yin|yang|none&gt;} — elige camino sin pasar por la pantalla.</li>
 *   <li>{@code otherhalf} — concede la mitad que falte ({@code gain_other_half}).</li>
 *   <li>{@code reset} — vuelve al estado de quien no ha visto a nadie.</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b>: el requisito real es rango Kage, el mas alto del datapack, y la cola tarda una
 * hora en dispararse.
 */
public final class StoryDebugCommand {

    private StoryDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsustory")
                        .then(Commands.literal("get").executes(StoryDebugCommand::get))
                        .then(Commands.literal("try").executes(StoryDebugCommand::tryEncounter))
                        .then(Commands.literal("force").executes(StoryDebugCommand::force))
                        .then(Commands.literal("path")
                                .then(Commands.literal("yin").executes(ctx -> path(ctx, SagePath.YIN)))
                                .then(Commands.literal("yang").executes(ctx -> path(ctx, SagePath.YANG)))
                                .then(Commands.literal("none").executes(ctx -> path(ctx, SagePath.NONE))))
                        .then(Commands.literal("otherhalf").executes(StoryDebugCommand::otherHalf))
                        .then(Commands.literal("sixpaths")
                                .then(Commands.literal("get").executes(StoryDebugCommand::sixPathsGet))
                                .then(Commands.literal("grant").executes(StoryDebugCommand::sixPathsGrant))
                                .then(Commands.literal("lend").executes(StoryDebugCommand::sixPathsLend))
                                .then(Commands.literal("revoke").executes(StoryDebugCommand::sixPathsRevoke))
                                .then(Commands.literal("sagemode").executes(StoryDebugCommand::sixPathsSageMode)))
                        .then(Commands.literal("reset").executes(StoryDebugCommand::reset))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        StoryState state = data.story();
        int rank = data.progression().rank().ordinal();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "clan=" + data.clan().clan().orElse("-")
                        + " sabio=" + SageEncounterService.sageOf(player).map(SageEncounter::id).orElse("ninguno")
                        + " | etapa=" + state.stage() + "/" + StoryTable.STAGE_DONE
                        + " fallos=" + state.failedAttempts()
                        + " probabilidad=" + StoryTable.chanceFor(state.failedAttempts()) + "%"
                        + " | yin=" + state.yin() + " yang=" + state.yang()
                        + " | rango " + rank + "/" + StoryTable.REQUIRED_RANK
                        + (StoryTable.rankAllows(rank) ? " (ok)" : " (insuficiente)")), false);
        return 1;
    }

    private static int tryEncounter(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var sage = SageEncounterService.tryEncounter(player);
        ctx.getSource().sendSuccess(() -> Component.literal(sage
                .map(found -> "se te aparecio " + found.id())
                .orElse("no hubo encuentro (clan, rango o azar)")), false);
        return sage.isPresent() ? 1 : 0;
    }

    private static int force(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var sage = SageEncounterService.forceEncounter(player);
        ctx.getSource().sendSuccess(() -> Component.literal(sage
                .map(found -> "forzado: " + found.id())
                .orElse("tu clan no desciende de ningun sabio (" + SageEncounter.allClans() + ")")), false);
        return sage.isPresent() ? 1 : 0;
    }

    private static int path(CommandContext<CommandSourceStack> ctx, SagePath path) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = SagePathService.grant(player, path, false);
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "camino " + path.id() + " concedido"
                : "no hay encuentro abierto (usa force antes)"), false);
        return ok ? 1 : 0;
    }

    private static int otherHalf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var granted = SageEncounterService.grantOtherHalf(player);
        ctx.getSource().sendSuccess(() -> Component.literal(granted
                .map(path -> "otra mitad concedida: " + path.id())
                .orElse("no procede (ya tiene las dos, o ninguna)")), false);
        return granted.isPresent() ? 1 : 0;
    }

    /** Estado del chakra de los Seis Caminos: si lo tiene, si es suyo o prestado, y por quien (F9/DJT-210). */
    private static int sixPathsGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        SixPathsState sp = NinjaData.get(player).sixPaths();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "chakra=" + sp.chakra()
                        + " origen=" + sp.loanSource().map(s -> "prestado por " + s).orElse("propio")
                        + " modoSabioSP=" + sp.sageMode()), false);
        return 1;
    }

    /** Concede el chakra en propiedad ({@code cheats/give_six_paths}). */
    private static int sixPathsGrant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = SixPathsService.grant(player, "debug");
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "concedido (+" + SixPathsTable.CHAKRA_BONUS + " chakra, 7 naturalezas, +"
                        + SixPathsTable.SENJUTSU_LEVELS + " senjutsu)"
                : "ya lo tenia en propiedad"), false);
        return ok ? 1 : 0;
    }

    /** Lo presta como haria el Diez Colas, para poder probar la retirada sin sellar la bestia. */
    private static int sixPathsLend(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = SixPathsService.lend(player, "debug");
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "prestado por 'debug' (solo chakra y XP, sin naturalezas)"
                : "ya lo tiene"), false);
        return ok ? 1 : 0;
    }

    private static int sixPathsRevoke(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = SixPathsService.revokeFrom(player, "debug");
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "retirado el prestamo de 'debug'"
                : "no lo prestaba 'debug' (si es suyo, no se le puede quitar)"), false);
        return ok ? 1 : 0;
    }

    private static int sixPathsSageMode(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = SixPathsService.unlockSageMode(player);
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "Modo Sabio de los Seis Caminos desbloqueado"
                : "no procede (hay que ser sabio primero, o ya lo tenia)"), false);
        return ok ? 1 : 0;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NinjaData.get(player).setStory(StoryState.DEFAULT);
        ctx.getSource().sendSuccess(() -> Component.literal("estado narrativo reiniciado"), false);
        return 1;
    }
}
