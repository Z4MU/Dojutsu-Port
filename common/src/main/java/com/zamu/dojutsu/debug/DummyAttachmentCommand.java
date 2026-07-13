package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsudummy get|set <n>} para validar la persistencia
 * del attachment dummy en ambos loaders (RM-6). Se registra mediante el evento
 * cross-loader de Architectury, evitando cablear comandos por loader.
 *
 * <p>Andamiaje temporal de F0; se retirara con {@link DummyAttachment}.
 */
public final class DummyAttachmentCommand {

    private DummyAttachmentCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsudummy")
                        .then(Commands.literal("get").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int value = DummyAttachment.handle().get(player);
                            ctx.getSource().sendSuccess(() -> Component.literal("dummy_value = " + value), false);
                            return value;
                        }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            DummyAttachment.handle().set(player, value);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("dummy_value <- " + value), false);
                                            return value;
                                        })))));
    }
}
