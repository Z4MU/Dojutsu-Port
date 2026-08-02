package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zamu.dojutsu.entity.AbilityAnchor;
import com.zamu.dojutsu.entity.MarkerEntity;
import com.zamu.dojutsu.registry.ModEntities;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion del ancla y el marcador (F4/DJT-071). Ejercita el ciclo de vida de
 * la ficha: ancla con vida corta que ejecuta su tick y expira limpiamente, multiples anclas
 * del mismo jugador, ancla huerfana (el callback resuelve al dueno por {@code PlayerRef} y
 * no hace nada si esta offline) y recuento para verificar que nada queda colgado.
 * <ul>
 *   <li>{@code /dojutsuanchor spawn [life]} — ancla en tu posicion (default 100t): avisa por
 *       chat cada segundo de vida y al expirar.</li>
 *   <li>{@code /dojutsuanchor marker [life]} — marcador en tu posicion (default 100t),
 *       silencioso (verificar con {@code count}).</li>
 *   <li>{@code /dojutsuanchor count} — anclas y marcadores vivos en tu dimension.</li>
 * </ul>
 *
 * <p>Temporal; se retira cuando las habilidades ancladas (F5/F6) cubran el uso real.
 */
public final class AnchorDebugCommand {

    private static final int DEFAULT_LIFE_TICKS = 100;

    private AnchorDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsuanchor")
                        .then(Commands.literal("spawn")
                                .executes(ctx -> spawnAnchor(ctx, DEFAULT_LIFE_TICKS))
                                .then(Commands.argument("life", IntegerArgumentType.integer(1, 12000))
                                        .executes(ctx -> spawnAnchor(ctx,
                                                IntegerArgumentType.getInteger(ctx, "life")))))
                        .then(Commands.literal("marker")
                                .executes(ctx -> spawnMarker(ctx, DEFAULT_LIFE_TICKS))
                                .then(Commands.argument("life", IntegerArgumentType.integer(1, 12000))
                                        .executes(ctx -> spawnMarker(ctx,
                                                IntegerArgumentType.getInteger(ctx, "life")))))
                        .then(Commands.literal("count").executes(AnchorDebugCommand::count))));
    }

    private static int spawnAnchor(CommandContext<CommandSourceStack> ctx, int life)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getLevel();
        MinecraftServer server = ctx.getSource().getServer();

        AbilityAnchor anchor = AbilityAnchor.create(level, player, life)
                .withTickCallback(a -> {
                    // Latido visible: 1 mensaje por segundo de vida (el tick corre de verdad).
                    if (a.tickCount % 20 == 0) {
                        a.ownerRef().flatMap(ref -> ref.resolve(server)).ifPresent(owner ->
                                owner.sendSystemMessage(Component.literal(
                                        "[ancla " + a.getId() + "] activa: " + a.tickCount
                                                + "t de " + a.maxLifetimeTicks() + "t")));
                    }
                })
                .withExpireCallback(a -> a.ownerRef().flatMap(ref -> ref.resolve(server)).ifPresent(owner ->
                        owner.sendSystemMessage(Component.literal(
                                "[ancla " + a.getId() + "] EXPIRADA a los " + a.maxLifetimeTicks() + "t"))));
        level.addFreshEntity(anchor);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "ancla " + anchor.getId() + " creada (vida " + life + "t)"), false);
        return 1;
    }

    private static int spawnMarker(CommandContext<CommandSourceStack> ctx, int life)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerLevel level = ctx.getSource().getLevel();

        MarkerEntity marker = MarkerEntity.create(level, player.position(), life).withOwner(player);
        level.addFreshEntity(marker);

        ctx.getSource().sendSuccess(() -> Component.literal(
                "marcador " + marker.getId() + " creado (vida " + life + "t)"), false);
        return 1;
    }

    private static int count(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int anchors = level.getEntities(ModEntities.ABILITY_ANCHOR.get(), e -> true).size();
        int markers = level.getEntities(ModEntities.MARKER.get(), e -> true).size();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "vivos en esta dimension: anclas=" + anchors + ", marcadores=" + markers), false);
        return 1;
    }
}
