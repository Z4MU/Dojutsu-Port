package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.clans.ClanRegistry;
import com.zamu.dojutsu.core.onboarding.OnboardingController;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsuonboarding state|clan <id>|affinity <id>} para validar en runtime la
 * maquina de estados de la creacion del ninja (DJT-059) <b>antes de que exista la Screen</b> (DJT-060): simula
 * en el servidor las elecciones que la pantalla enviara por payload — pasa por el mismo
 * {@code OnboardingController} que el handler.
 *
 * <p>Andamiaje temporal: se retira cuando la Screen de DJT-060 cubra el flujo completo.
 */
public final class OnboardingDebugCommand {

    private static final SuggestionProvider<CommandSourceStack> CLANS = (ctx, builder) -> {
        var options = new ArrayList<>(ClanRegistry.all().keySet());
        options.add(OnboardingController.RANDOM_CHOICE); // join_random (DJT-060)
        return SharedSuggestionProvider.suggest(options, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AFFINITIES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    List.of("fire", "wind", "lightning", "earth", "water", OnboardingController.RANDOM_CHOICE),
                    builder);

    private OnboardingDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsuonboarding")
                        .then(Commands.literal("state").executes(OnboardingDebugCommand::state))
                        .then(Commands.literal("clan")
                                .then(Commands.argument("clan", StringArgumentType.word())
                                        .suggests(CLANS)
                                        .executes(ctx -> choose(ctx, true))))
                        .then(Commands.literal("affinity")
                                .then(Commands.argument("affinity", StringArgumentType.word())
                                        .suggests(AFFINITIES)
                                        .executes(ctx -> choose(ctx, false))))));
    }

    private static int state(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "fase=" + data.onboarding().phase()
                        + " clan=" + data.clan().clan().orElse("(sin clan)")
                        + " naturalezas=" + data.natures().natures()
                        + " kg=" + data.natures().kekkeiGenkai()
                        + " ninjaXp=" + data.progression().ninjaXp()), false);
        return 1;
    }

    private static int choose(CommandContext<CommandSourceStack> ctx, boolean clan) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String choice = StringArgumentType.getString(ctx, clan ? "clan" : "affinity").toLowerCase(Locale.ROOT);
        boolean applied = clan
                ? OnboardingController.chooseClan(player, choice)
                : OnboardingController.chooseAffinity(player, choice);
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(applied
                ? "eleccion aplicada -> fase=" + data.onboarding().phase()
                : "rechazada (fase o eleccion invalidas) -> fase=" + data.onboarding().phase()), false);
        return applied ? 1 : 0;
    }
}
