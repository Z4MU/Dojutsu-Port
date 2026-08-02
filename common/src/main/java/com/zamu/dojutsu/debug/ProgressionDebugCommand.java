package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.SkillPointService;
import com.zamu.dojutsu.progression.XpApi;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsuprogression get|award <n>|spend <skill> <n>} para validar en runtime la
 * progresion (DJT-035+): ganar/gastar skill points, ver nivel de habilidades, ninja_xp y rango.
 *
 * <p>Andamiaje temporal: se retira cuando el menu (DJT-040) permita observar y gastar in-game.
 */
public final class ProgressionDebugCommand {

    private ProgressionDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsuprogression")
                        .then(Commands.literal("get").executes(ctx -> {
                            ProgressionState p = NinjaData.get(ctx.getSource().getPlayerOrException()).progression();
                            StringBuilder sb = new StringBuilder("skillPoints=" + p.skillPoints()
                                    + " ninjaXp=" + p.ninjaXp() + " rank=" + p.rank().title());
                            for (Skill skill : Skill.values()) {
                                sb.append(' ').append(skill.name().toLowerCase(Locale.ROOT))
                                        .append('=').append(p.skill(skill).level())
                                        .append('/').append(p.skill(skill).xp());
                            }
                            sb.append(" unlocked=").append(p.unlocked());
                            ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
                            return 1;
                        }))
                        .then(Commands.literal("award")
                                .then(Commands.argument("points", IntegerArgumentType.integer(1)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int points = IntegerArgumentType.getInteger(ctx, "points");
                                    PlayerNinjaData data = NinjaData.get(player);
                                    SkillPointService.award(data, points);
                                    NinjaSync.sendDelta(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "skillPoints <- " + data.progression().skillPoints()), false);
                                    return 1;
                                })))
                        .then(Commands.literal("spend")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .then(Commands.argument("points", IntegerArgumentType.integer(1)).executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "skill").toUpperCase(Locale.ROOT);
                                            Skill skill;
                                            try {
                                                skill = Skill.valueOf(name);
                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().sendFailure(Component.literal("skill invalida: " + name));
                                                return 0;
                                            }
                                            int points = IntegerArgumentType.getInteger(ctx, "points");
                                            boolean ok = SkillPointService.spend(player, skill, points);
                                            NinjaSync.sendDelta(player);
                                            PlayerNinjaData data = NinjaData.get(player);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    (ok ? "gastado " : "rechazado ") + points + " en " + name
                                                            + " -> nivel=" + data.progression().skill(skill).level()
                                                            + " skillPoints=" + data.progression().skillPoints()
                                                            + " ninjaXp=" + data.progression().ninjaXp()), false);
                                            return ok ? 1 : 0;
                                        }))))
                        .then(Commands.literal("xp")
                                .then(Commands.argument("skill", StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String name = StringArgumentType.getString(ctx, "skill").toUpperCase(Locale.ROOT);
                                            Skill skill;
                                            try {
                                                skill = Skill.valueOf(name);
                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().sendFailure(Component.literal("skill invalida: " + name));
                                                return 0;
                                            }
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            XpApi.addSkillXp(player, skill, amount);
                                            NinjaSync.sendDelta(player);
                                            PlayerNinjaData data = NinjaData.get(player);
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "xp +" + amount + " en " + name + " -> nivel="
                                                            + data.progression().skill(skill).level()
                                                            + " xp=" + data.progression().skill(skill).xp()), false);
                                            return 1;
                                        }))))
                        .then(Commands.literal("ninjaxp")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    XpApi.addNinjaXp(player, amount);
                                    NinjaSync.sendDelta(player);
                                    PlayerNinjaData data = NinjaData.get(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "ninjaXp <- " + data.progression().ninjaXp()
                                                    + " rank=" + data.progression().rank().title()), false);
                                    return 1;
                                })))));
    }
}
