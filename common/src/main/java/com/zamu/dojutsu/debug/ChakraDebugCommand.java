package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.chakra.RegenService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.network.NinjaSync;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsuchakra get|consume <n>|restore <n>|full|setmax <n>} para validar en
 * runtime el chakra (DJT-030): consumo "todo o nada", reposicion con clamp, persistencia y sync al cliente. El
 * {@code get} muestra ademas los factores de regen <b>derivados del estado real</b> (DJT-057). {@code setmax}
 * (F4/DJT-085) sube el maximo para probar jutsus de coste alto (great_fire_annihilation 180, kirin 300...) que
 * exceden el maximo base de 100.
 *
 * <p>Andamiaje temporal. Muta el estado solo por la puerta {@link ChakraService} (nunca directamente) y
 * sincroniza tras cada cambio.
 */
public final class ChakraDebugCommand {

    private ChakraDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsuchakra")
                        .then(Commands.literal("get").executes(ctx -> {
                            PlayerNinjaData d = NinjaData.get(ctx.getSource().getPlayerOrException());
                            ChakraState c = d.chakra();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "chakra=" + c.chakra() + "/" + c.maxChakra()
                                            + " natural=" + c.naturalChakra() + "/" + c.maxNaturalChakra()
                                            + " chakraTimer=" + c.chakraTimer() + " jugoTimer=" + c.jugoTimer()
                                            + " regenFactors=" + RegenService.factorsFor(d)
                                            + " jugoKg=" + RegenService.hasJugoKekkeiGenkai(d)), false);
                            return 1;
                        }))
                        .then(Commands.literal("consume")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    PlayerNinjaData data = NinjaData.get(player);
                                    boolean ok = ChakraService.consume(data, amount);
                                    NinjaSync.sendDelta(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            (ok ? "consumido " : "rechazado (insuficiente) ") + amount
                                                    + " -> chakra=" + data.chakra().chakra() + "/" + data.chakra().maxChakra()), false);
                                    return ok ? 1 : 0;
                                })))
                        .then(Commands.literal("restore")
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    PlayerNinjaData data = NinjaData.get(player);
                                    ChakraService.restore(data, amount);
                                    NinjaSync.sendDelta(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "chakra <- " + data.chakra().chakra() + "/" + data.chakra().maxChakra()), false);
                                    return 1;
                                })))
                        .then(Commands.literal("full").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            PlayerNinjaData data = NinjaData.get(player);
                            ChakraService.restoreFull(data);
                            NinjaSync.sendDelta(player);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "chakra <- " + data.chakra().chakra() + "/" + data.chakra().maxChakra()), false);
                            return 1;
                        }))
                        .then(Commands.literal("setmax")
                                .then(Commands.argument("max", IntegerArgumentType.integer(1)).executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int max = IntegerArgumentType.getInteger(ctx, "max");
                                    PlayerNinjaData data = NinjaData.get(player);
                                    ChakraService.setMax(data, max, true); // rellena de paso (misma puerta que el join de clan)
                                    NinjaSync.sendDelta(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "maximo <- " + data.chakra().chakra() + "/" + data.chakra().maxChakra()), false);
                                    return 1;
                                })))
                ));
        // El subcomando 'regen <factor>' (andamiaje de F2) quedo retirado en DJT-057: los factores se derivan
        // del estado real (clan/ADN/KG Jugo) y se inspeccionan con 'get'. El Modo Sabio llega con F6.
    }
}
