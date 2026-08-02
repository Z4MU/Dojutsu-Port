package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
 * </ul>
 *
 * <p>Temporal; se retira cuando el menu/HUD de F4 muestre el catalogo y la tecla cubra el cast.
 */
public final class JutsuDebugCommand {

    private JutsuDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsujutsu")
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
                        .then(Commands.literal("lastcast").executes(JutsuDebugCommand::lastCast))));
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
