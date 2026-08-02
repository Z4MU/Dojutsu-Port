package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.network.NinjaSync;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsuninja get|set <fase>} para validar en runtime la persistencia de
 * {@link PlayerNinjaData} (save/relogin/respawn/dimension) en ambos loaders (DJT-019).
 *
 * <p>Andamiaje temporal: se retira con los {@code AdminCommands} reales de DJT-026. Manipula la fase de
 * {@link OnboardingState} por ser el unico campo observable del agregado minimo de F1.
 */
public final class NinjaDataDebugCommand {

    private NinjaDataDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsuninja")
                        .then(Commands.literal("get").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerNinjaData data = NinjaData.get(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "dataVersion=" + data.dataVersion()
                                            + " onboarding=" + data.onboarding().phase()), false);
                            return 1;
                        }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("phase", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "phase")
                                                    .toUpperCase(Locale.ROOT);
                                            OnboardingState.Phase phase;
                                            try {
                                                phase = OnboardingState.Phase.valueOf(name);
                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().sendFailure(
                                                        Component.literal("Fase invalida: " + name));
                                                return 0;
                                            }
                                            PlayerNinjaData data = NinjaData.get(player);
                                            data.setOnboarding(data.onboarding().withPhase(phase));
                                            NinjaSync.sendDelta(player); // sync del cambio observable
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("onboarding <- " + phase), false);
                                            return 1;
                                        })))));
    }
}
