package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.jutsu.JutsuCastPipeline;
import com.zamu.dojutsu.jutsu.JutsuEntry;
import com.zamu.dojutsu.jutsu.JutsuRegistry;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * Comando de depuracion del registro de jutsus (F4/DJT-073). Valida en runtime la carga
 * data-driven y el {@code /reload}:
 * <ul>
 *   <li>{@code /dojutsujutsu list} — cuantos y cuales jutsus hay cargados.</li>
 *   <li>{@code /dojutsujutsu get <id>} — detalle de una entrada (coste/cd/afinidad/categoria/efecto).</li>
 *   <li>{@code /dojutsujutsu cast <id>} — lanza por el pipeline (DJT-074; simula la tecla de DJT-080).</li>
 *   <li>{@code /dojutsujutsu learn <id>} — aprende un jutsu sin pasar por el pergamino (F11/DJT-231).</li>
 *   <li>{@code /dojutsujutsu select <id>} — elige el jutsu de la tecla de cast; {@code none} lo retira.</li>
 *   <li>{@code /dojutsujutsu known} — que jutsus conoces, cual llevas seleccionado y tu rueda.</li>
 *   <li>{@code /dojutsujutsu slot <1-8> <id>} — coloca un jutsu en un hueco de la rueda; {@code none} lo
 *       vacia (F11/DJT-234; redundante con el libro desde DJT-235).</li>
 * </ul>
 *
 * <p><b>Por que {@code learn}/{@code select} existen:</b> hasta que llegue la rueda (DJT-232) no hay ninguna
 * via en juego para elegir jutsu, y sin eso la tecla de cast no se puede probar. Se retiran con el resto del
 * comando cuando la interfaz cubra el catalogo.
 *
 * <p>Temporal; se retira cuando el menu/HUD de F4 muestre el catalogo y la tecla cubra el cast.
 */
public final class JutsuDebugCommand {

    private JutsuDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsujutsu")
                        .then(Commands.literal("list").executes(JutsuDebugCommand::list))
                        .then(Commands.literal("get")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                JutsuRegistry.all().keySet(), builder))
                                        .executes(JutsuDebugCommand::get)))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                JutsuRegistry.all().keySet(), builder))
                                        .executes(JutsuDebugCommand::cast)))
                        .then(Commands.literal("learn")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                JutsuRegistry.all().keySet(), builder))
                                        .executes(JutsuDebugCommand::learn)))
                        .then(Commands.literal("select")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        // Sugiere solo lo APRENDIDO: es lo unico que el servidor aceptaria.
                                        .suggests(JutsuDebugCommand::suggestKnown)
                                        .executes(JutsuDebugCommand::select)))
                        .then(Commands.literal("known").executes(JutsuDebugCommand::known))
                        .then(Commands.literal("slot")
                                .then(Commands.argument("index", IntegerArgumentType.integer(
                                                1, JutsuKnowledge.SLOT_COUNT))
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .suggests(JutsuDebugCommand::suggestKnown)
                                                .executes(JutsuDebugCommand::slot))))
                        .then(Commands.literal("lastcast").executes(JutsuDebugCommand::lastCast))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
            suggestKnown(CommandContext<CommandSourceStack> ctx,
                    com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("none");
        if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            options.addAll(com.zamu.dojutsu.data.NinjaData.get(player).jutsu().known());
        }
        return SharedSuggestionProvider.suggest(options, builder);
    }

    /** Aprende un jutsu directamente (atajo del pergamino, para probar el selector). */
    private static int learn(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        if (JutsuRegistry.get(id).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("jutsu desconocido: " + id));
            return 0;
        }
        boolean granted = com.zamu.dojutsu.jutsu.JutsuGrantService.grant(player, id);
        ctx.getSource().sendSuccess(() -> Component.literal(
                granted ? "aprendido: " + id : "ya lo conocias: " + id), false);
        return granted ? 1 : 0;
    }

    /** Selecciona el jutsu de la tecla de cast por la misma puerta de servidor que usa el payload. */
    private static int select(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        var data = com.zamu.dojutsu.data.NinjaData.get(player);
        var outcome = "none".equals(id)
                ? com.zamu.dojutsu.jutsu.JutsuSelectionService.clear(data)
                : com.zamu.dojutsu.jutsu.JutsuSelectionService.select(data, id);
        com.zamu.dojutsu.network.NinjaSync.sendDelta(player); // que el cliente vea lo mismo que el servidor
        ctx.getSource().sendSuccess(() -> Component.literal("select " + id + " -> " + outcome), false);
        return outcome == com.zamu.dojutsu.jutsu.JutsuSelectionService.SelectOutcome.NOT_KNOWN ? 0 : 1;
    }

    /** Coloca (o vacia) un hueco de la rueda. El indice es 1-8 en pantalla, 0-7 en el estado. */
    private static int slot(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        int index = IntegerArgumentType.getInteger(ctx, "index") - 1;
        String id = StringArgumentType.getString(ctx, "id");
        var data = com.zamu.dojutsu.data.NinjaData.get(player);
        if (!"none".equals(id) && !data.jutsu().knows(id)) {
            ctx.getSource().sendFailure(Component.literal("no has aprendido: " + id));
            return 0;
        }
        data.setJutsu("none".equals(id) ? data.jutsu().withoutSlot(index) : data.jutsu().withSlot(index, id));
        com.zamu.dojutsu.network.NinjaSync.sendDelta(player); // la rueda del cliente lo dibuja
        ctx.getSource().sendSuccess(() -> Component.literal(
                "hueco " + (index + 1) + " <- " + id), false);
        return 1;
    }

    /** Muestra los jutsus aprendidos, el seleccionado (lo que lanzaria la tecla) y la rueda. */
    private static int known(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        var knowledge = com.zamu.dojutsu.data.NinjaData.get(player).jutsu();
        String ids = String.join(", ", knowledge.known());
        ctx.getSource().sendSuccess(() -> Component.literal(
                "aprendidos (" + knowledge.known().size() + "): " + (ids.isEmpty() ? "(ninguno)" : ids)), false);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "seleccionado: " + knowledge.selectedJutsu().orElse("(ninguno)")), false);
        for (int i = 0; i < JutsuKnowledge.SLOT_COUNT; i++) {
            int shown = i + 1;
            String id = knowledge.slot(i).orElse("(libre)");
            ctx.getSource().sendSuccess(() -> Component.literal("  hueco " + shown + ": " + id), false);
        }
        return knowledge.known().size();
    }

    /** Lista los casts recientes copiables (radio 20, como el Sharingan del datapack) cerca del jugador. */
    private static int lastCast(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        var casts = com.zamu.dojutsu.jutsu.LastCastRegistry.recentCastsNear(
                player.serverLevel(), player.position(), 20.0D, player.getUUID());
        long now = player.serverLevel().getGameTime();
        if (casts.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("sin casts recientes copiables cerca"), false);
            return 0;
        }
        for (var rec : casts) {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "  " + rec.jutsuId() + " por " + rec.caster() + " (expira en " + (rec.expiresAtTick() - now) + "t)"), false);
        }
        return casts.size();
    }

    private static int cast(CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");
        var outcome = JutsuCastPipeline.cast(player, id);
        ctx.getSource().sendSuccess(() -> Component.literal("cast de " + id + " -> " + outcome), false);
        return outcome.isSuccess() ? 1 : 0;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        var all = JutsuRegistry.all();
        String ids = all.keySet().stream().collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "jutsus cargados (" + all.size() + "): " + (ids.isEmpty() ? "(ninguno)" : ids)), false);
        return all.size();
    }

    private static int get(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        return JutsuRegistry.get(id).map(entry -> {
            ctx.getSource().sendSuccess(() -> Component.literal(describe(entry)), false);
            // Si lo ejecuta un jugador, muestra tambien el cooldown restante (DJT-077).
            if (ctx.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                long remaining = com.zamu.dojutsu.data.NinjaData.get(player).jutsu()
                        .remainingCooldown(id, player.serverLevel().getGameTime());
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  cooldown restante: " + remaining + "t"), false);
            }
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("jutsu desconocido: " + id));
            return 0;
        });
    }

    private static String describe(JutsuEntry entry) {
        var def = entry.definition();
        String affinity = def.affinityRequirement()
                .map(req -> req.natureId() + " >= " + req.amount())
                .orElse("(universal)");
        return def.id() + ": coste=" + def.chakraCost() + ", cd=" + def.cooldownTicks()
                + "t, afinidad=" + affinity + ", nivel=" + def.levelRequirement()
                + ", categoria=" + def.category() + ", efecto=" + def.effectId()
                + ", params=" + entry.effectParams();
    }
}
