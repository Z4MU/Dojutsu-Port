package com.zamu.dojutsu.core.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zamu.dojutsu.core.onboarding.OnboardingController;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.jutsu.JutsuLearningService;
import com.zamu.dojutsu.natures.LearnerService;
import com.zamu.dojutsu.network.NinjaSync;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comandos de administracion equivalentes a {@code cheats/} del datapack, bajo {@code /dojutsu} y con
 * permiso de op (nivel 2).
 *
 * <p>El unico completo en F1 es {@code reset_ninja}, que reemplaza {@code PlayerNinjaData} por una instancia
 * nueva (reset total, corrige S01-R2). El resto ({@code give_six_paths}, {@code reset_cooldowns},
 * {@code restore_all}, {@code set_test_ninja}, {@code spawn_shinobi_villager}) quedan como stubs que se
 * completan en su fase.
 */
public final class AdminCommands {

    /** Nivel de permiso de op (gamemasters). */
    private static final int OP_LEVEL = 2;

    private AdminCommands() {
    }

    /** Suscribe el registro de los comandos. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsu")
                        .requires(source -> source.hasPermission(OP_LEVEL))
                        .then(Commands.literal("reset_ninja")
                                .executes(ctx -> resetNinja(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> resetNinja(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                        .then(stub("give_six_paths"))
                        .then(stub("reset_cooldowns"))
                        .then(stub("restore_all"))
                        .then(stub("set_test_ninja"))
                        .then(stub("spawn_shinobi_villager"))));
    }

    private static int resetNinja(CommandSourceStack source, ServerPlayer target) {
        NinjaData.reset(target);
        LearnerService.removeStaleLearners(target); // sin elementos -> sin learners residuales ([S03-R2], DJT-058)
        JutsuLearningService.removeStaleJutsuItems(target); // sin conocimiento -> sin items de jutsu (DJT-075)
        // Re-embarque INMEDIATO (NOT_STARTED -> SELECTING_CLAN + opciones de clan): sin esto, la Screen de
        // creacion solo reaparecia al reloguear (el refresh del login). Mismo orden que DojutsuLifecycle.onJoin.
        OnboardingController.refresh(target);
        NinjaSync.sendFull(target); // el cliente refleja los defaults tras el reset
        source.sendSuccess(() -> Component.literal(
                "Ninja de " + target.getGameProfile().getName() + " reseteado (instancia nueva)."), true);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> stub(String name) {
        return Commands.literal(name).executes(ctx -> {
            ctx.getSource().sendSuccess(
                    () -> Component.literal("/dojutsu " + name + ": pendiente (se completa en su fase)."), false);
            return 1;
        });
    }
}
