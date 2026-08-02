package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.ai.EdoTenseiService;
import com.zamu.dojutsu.ai.ShinobiEntity;
import com.zamu.dojutsu.domain.ai.ShinobiCombatPlan;
import com.zamu.dojutsu.domain.ai.ThreatLevel;
import com.zamu.dojutsu.jutsu.JutsuRegistry;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Comando de depuracion {@code /dojutsushinobi} para validar en runtime la IA de F8 (DJT-185). En el datapack los
 * shinobi salen de un huevo de invocacion (item, sin ticket propio); hasta entonces, este es el disparador.
 *
 * <ul>
 *   <li>{@code spawn &lt;threat&gt;} — reanima un shinobi de ese nivel delante de ti (Edo Tensei).</li>
 *   <li>{@code info &lt;threat&gt;} — su vida, sus jutsus y si estan en el catalogo.</li>
 *   <li>{@code killall} — retira todos los shinobi del mundo.</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b>, como los de F6 y F7.
 */
public final class ShinobiDebugCommand {

    /** Sugiere los seis niveles de amenaza. */
    private static final SuggestionProvider<CommandSourceStack> THREATS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(ThreatLevel.values()).map(ThreatLevel::id), builder);

    /** Bloques por delante del invocador donde aparece. */
    private static final double SPAWN_DISTANCE = 4.0;

    private ShinobiDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsushinobi")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("threat", StringArgumentType.word())
                                        .suggests(THREATS)
                                        .executes(ShinobiDebugCommand::spawn)))
                        .then(Commands.literal("info")
                                .then(Commands.argument("threat", StringArgumentType.word())
                                        .suggests(THREATS)
                                        .executes(ShinobiDebugCommand::info)))
                        .then(Commands.literal("killall").executes(ShinobiDebugCommand::killAll))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "threat").toLowerCase(Locale.ROOT);
        return ThreatLevel.byId(id).map(threat -> {
            Vec3 spot = player.position().add(player.getLookAngle().scale(SPAWN_DISTANCE));
            ShinobiEntity shinobi = EdoTenseiService.reanimate((ServerLevel) player.level(), threat, spot);
            if (shinobi == null) {
                ctx.getSource().sendFailure(Component.literal("el mundo rechazo la entidad"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "reanimado un " + threat.id() + " con " + (int) threat.maxHealth() + " de vida"), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("nivel de amenaza desconocido: " + id));
            return 0;
        });
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "threat").toLowerCase(Locale.ROOT);
        return ThreatLevel.byId(id).map(threat -> {
            StringBuilder sb = new StringBuilder(threat.id() + " (nivel " + threat.datapackId() + ")"
                    + " vida=" + (int) threat.maxHealth()
                    + " | decide cada " + ShinobiCombatPlan.DECISION_INTERVAL_TICKS + "t con un jugador a "
                    + (int) ShinobiCombatPlan.ACTIVATION_RADIUS + " bloques"
                    + " | melee <=" + (int) ShinobiCombatPlan.MELEE_RANGE
                    + ", kunai <=" + (int) ShinobiCombatPlan.THROWING_RANGE + ", jutsu mas alla");
            if (threat.jutsus().isEmpty()) {
                sb.append("\n  sin jutsus: solo melee y kunai");
            } else {
                threat.jutsus().forEach(jutsu -> sb.append("\n  ").append(jutsu)
                        .append(JutsuRegistry.get(jutsu).isPresent() ? " (en catalogo)" : " (NO EXISTE)"));
            }
            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("nivel de amenaza desconocido: " + id));
            return 0;
        });
    }

    private static int killAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        int[] removed = {0};
        level.getAllEntities().forEach(entity -> {
            if (entity instanceof ShinobiEntity shinobi) {
                shinobi.discard();
                removed[0]++;
            }
        });
        ctx.getSource().sendSuccess(() -> Component.literal("retirados " + removed[0] + " shinobi"), false);
        return removed[0];
    }
}
