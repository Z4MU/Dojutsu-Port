package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zamu.dojutsu.domain.godtree.GodTreeTable;
import com.zamu.dojutsu.world.godtree.GodTreeRegistry;
import com.zamu.dojutsu.world.godtree.GodTreeService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsugodtree} para validar el Arbol Divino de F9 (DJT-215) sin ser jinchuriki
 * del Diez Colas ni esperar los {@value GodTreeTable#TOTAL_TICKS} ticks (25 minutos) que tarda en florecer.
 *
 * <ul>
 *   <li>{@code plant} — lo planta donde estas, cobrando el chakra como la forma real.</li>
 *   <li>{@code get} — etapa y ticks del arbol que tengas creciendo.</li>
 *   <li>{@code advance [etapas]} — adelanta el reloj sin esperar (por defecto, una etapa).</li>
 *   <li>{@code bloom} — te despierta el Rinne Sharingan directamente.</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b>: el disparador real es el slot 8 de la forma del Diez Colas.
 */
public final class GodTreeDebugCommand {

    private GodTreeDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsugodtree")
                        .then(Commands.literal("plant").executes(GodTreeDebugCommand::plant))
                        .then(Commands.literal("get").executes(GodTreeDebugCommand::get))
                        .then(Commands.literal("advance")
                                .executes(ctx -> advance(ctx, 1))
                                .then(Commands.argument("stages", IntegerArgumentType.integer(1, GodTreeTable.MAX_STAGE))
                                        .executes(ctx -> advance(ctx, IntegerArgumentType.getInteger(ctx, "stages")))))
                        .then(Commands.literal("bloom").executes(GodTreeDebugCommand::bloom))));
    }

    private static int plant(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = GodTreeService.plant(player);
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "plantado (-" + GodTreeTable.PLANT_CHAKRA + " chakra; " + GodTreeTable.MAX_STAGE
                        + " etapas de " + GodTreeTable.TICKS_PER_STAGE + " ticks)"
                : "no se pudo plantar (mira el aviso en el chat)"), false);
        return ok ? 1 : 0;
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        var tree = GodTreeRegistry.of(player.server).treeOf(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal(tree
                .map(t -> "etapa=" + t.stage() + "/" + GodTreeTable.MAX_STAGE
                        + " ticks=" + t.timer() + "/" + GodTreeTable.TICKS_PER_STAGE
                        + " en " + t.pos().toShortString())
                .orElse("no tienes ningun Arbol Divino creciendo")), false);
        return tree.isPresent() ? 1 : 0;
    }

    /** Adelanta el reloj poniendo el contador al borde de crecer, tantas veces como se pida. */
    private static int advance(CommandContext<CommandSourceStack> ctx, int stages) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        GodTreeRegistry registry = GodTreeRegistry.of(player.server);
        for (int i = 0; i < stages; i++) {
            var current = registry.treeOf(player.getUUID());
            if (current.isEmpty()) {
                break; // florecio por el camino
            }
            var tree = current.get();
            registry.replace(tree, new GodTreeRegistry.GrowingTree(tree.dimension(), tree.pos(), tree.owner(),
                    tree.stage(), GodTreeTable.TICKS_PER_STAGE - 1));
            GodTreeService.tick(player.server); // el tick real hace crecer y, si toca, florecer
        }
        ctx.getSource().sendSuccess(() -> Component.literal("adelantadas " + stages + " etapa(s)"), false);
        return 1;
    }

    private static int bloom(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean ok = GodTreeService.awaken(player);
        ctx.getSource().sendSuccess(() -> Component.literal(ok
                ? "Rinne Sharingan despertado"
                : "no procede (ya lo tenias)"), false);
        return ok ? 1 : 0;
    }
}
