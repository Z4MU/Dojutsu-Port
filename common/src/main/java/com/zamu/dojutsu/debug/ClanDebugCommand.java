package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.clans.ClanDefinition;
import com.zamu.dojutsu.clans.ClanJutsuUnlockService;
import com.zamu.dojutsu.clans.ClanRegistry;
import com.zamu.dojutsu.clans.ClanService;
import com.zamu.dojutsu.core.onboarding.OnboardingController;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ClanMembership;
import com.zamu.dojutsu.natures.KekkeiGenkaiService;
import com.zamu.dojutsu.natures.NatureService;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsuclans list|get|set <clan>|clear} para validar en runtime las
 * definiciones de clan y la pertenencia (DJT-056): carga, recarga con {@code /reload} y persistencia.
 *
 * <p>Andamiaje temporal: se retira cuando el onboarding (DJT-059/060) cubra la eleccion de clan.
 * {@code set} escribe la <b>pertenencia en crudo</b> (sin aplicar bonus): el join real con bonus es
 * {@code ClanService.join} (DJT-057).
 */
public final class ClanDebugCommand {

    /** Sugiere los ids de clan cargados del datapack. */
    private static final SuggestionProvider<CommandSourceStack> CLANS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ClanRegistry.all().keySet(), builder);

    private ClanDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsuclans")
                        .then(Commands.literal("list").executes(ClanDebugCommand::list))
                        .then(Commands.literal("get").executes(ClanDebugCommand::get))
                        .then(Commands.literal("join")
                                .then(Commands.argument("clan", StringArgumentType.word())
                                        .suggests(CLANS)
                                        .executes(ClanDebugCommand::join)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("clan", StringArgumentType.word())
                                        .suggests(CLANS)
                                        .executes(ClanDebugCommand::set)))
                        .then(Commands.literal("reset").executes(ClanDebugCommand::reset))
                        .then(Commands.literal("clear").executes(ClanDebugCommand::clear))));
    }

    /** Reset real ({@code ClanService.reset}, DJT-058): defaults atomicos de los 4 sub-estados de gameplay. */
    private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ClanService.reset(player);
        OnboardingController.refresh(player); // sin clan -> re-embarque inmediato (SELECTING_CLAN, DJT-059)
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "clan reseteado -> clan=" + data.clan().clan().orElse("(sin clan)")
                        + " naturalezas=" + data.natures().natures()
                        + " kg=" + data.natures().kekkeiGenkai()
                        + " maxChakra=" + data.chakra().maxChakra()
                        + " ninjaXp=" + data.progression().ninjaXp()
                        + " puntos=" + data.progression().skillPoints()), false);
        return 1;
    }

    /** Muestra las definiciones cargadas (verifica la carga y el {@code /reload}). */
    private static int list(CommandContext<CommandSourceStack> ctx) {
        StringBuilder sb = new StringBuilder("clanes cargados: " + ClanRegistry.all().size());
        for (ClanDefinition def : ClanRegistry.all().values()) {
            sb.append('\n').append(def.id())
                    .append(" maxChakra=").append(def.maxChakra())
                    .append(" xp=").append(def.ninjaXp());
            if (!def.natures().isEmpty()) {
                sb.append(" naturalezas=").append(def.natures().stream().map(NatureService::id).toList());
            }
            if (!def.kekkeiGenkai().isEmpty()) {
                sb.append(" kg=").append(def.kekkeiGenkai().stream().map(KekkeiGenkaiService::id).toList());
            }
            if (!def.affinityBonus().isEmpty()) {
                sb.append(" aff+=").append(def.affinityBonus());
            }
            if (!def.skillBonus().isEmpty()) {
                sb.append(" skill+=").append(def.skillBonus());
            }
            if (!def.startingJutsu().isEmpty()) {
                sb.append(" jutsu=").append(def.startingJutsu());
            }
            if (!def.startingItems().isEmpty()) {
                sb.append(" items=").append(def.startingItems());
            }
            if (!def.potentialDojutsu().isEmpty()) {
                sb.append(" dojutsu=").append(def.potentialDojutsu());
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return ClanRegistry.all().size();
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "clan=" + data.clan().clan().orElse("(sin clan)")
                        + " initialized=" + data.clan().initialized()
                        + " jutsuTime=" + data.clan().jutsuTime()
                        + (ClanJutsuUnlockService.reachedThreshold(data) ? " (LISTO: entrega en F4)" : "")), false);
        return 1;
    }

    /** Join real ({@code ClanService.join}, DJT-057): aplica los bonus del clan exactamente una vez. */
    private static int join(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "clan").toLowerCase(Locale.ROOT);
        boolean joined = ClanService.join(player, id);
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(joined
                ? "unido a " + id
                        + " -> maxChakra=" + data.chakra().maxChakra() + " chakra=" + data.chakra().chakra()
                        + " ninjaXp=" + data.progression().ninjaXp()
                        + " naturalezas=" + data.natures().natures()
                        + " kg=" + data.natures().kekkeiGenkai()
                : "sin efecto (clan inexistente, ya tiene clan o bonus ya aplicados): " + id), false);
        return joined ? 1 : 0;
    }

    /** Escribe la pertenencia en crudo (sin bonus: eso es ClanService.join, DJT-057). */
    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "clan").toLowerCase(Locale.ROOT);
        if (ClanRegistry.get(id).isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("clan desconocido: " + id
                    + " (cargados: " + ClanRegistry.all().keySet() + ")"));
            return 0;
        }
        PlayerNinjaData data = NinjaData.get(player);
        data.setClan(data.clan().withClan(id));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "pertenencia <- " + id + " (en crudo, sin bonus: el join real es DJT-057)"), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        NinjaData.get(player).setClan(ClanMembership.DEFAULT);
        ctx.getSource().sendSuccess(() -> Component.literal("pertenencia <- (sin clan)"), false);
        return 1;
    }
}
