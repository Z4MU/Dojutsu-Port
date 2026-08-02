package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zamu.dojutsu.core.ServerTickDispatcher;
import com.zamu.dojutsu.core.identity.PlayerRef;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion para validar en runtime el {@link ServerTickDispatcher} y el
 * {@link com.zamu.dojutsu.core.scheduler.ModScheduler} (DJT-022):
 * <ul>
 *   <li>{@code /dojutsusched count} — muestra los ticks de servidor procesados (el dispatcher corre).</li>
 *   <li>{@code /dojutsusched in <ticks>} — programa un mensaje para dentro de N ticks (el scheduler corre a tiempo).</li>
 * </ul>
 *
 * <p>Andamiaje temporal; se retira con los {@code AdminCommands} de DJT-026. Usa {@link PlayerRef} para no
 * retener al jugador si se desconecta antes de que la tarea corra.
 */
public final class SchedulerDebugCommand {

    private SchedulerDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsusched")
                        .then(Commands.literal("count").executes(ctx -> {
                            long ticks = ServerTickDispatcher.get().tickCount();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("server ticks procesados = " + ticks), false);
                            return 1;
                        }))
                        .then(Commands.literal("in")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            MinecraftServer server = ctx.getSource().getServer();
                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                            PlayerRef ref = PlayerRef.of(player);
                                            ServerTickDispatcher.get().scheduler().schedule(ticks, () ->
                                                    ref.resolve(server).ifPresent(p -> p.sendSystemMessage(
                                                            Component.literal("[sched] ejecutada tras " + ticks + " ticks"))));
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("programada tarea a +" + ticks + "t"), false);
                                            return 1;
                                        })))));
    }
}
