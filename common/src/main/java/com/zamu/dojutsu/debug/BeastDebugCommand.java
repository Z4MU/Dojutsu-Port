package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.bijuu.ai.BeastMoveRegistry;
import com.zamu.dojutsu.bijuu.entity.TailedBeastEntity;
import com.zamu.dojutsu.bijuu.jinchuriki.FormService;
import com.zamu.dojutsu.bijuu.jinchuriki.SealingService;
import com.zamu.dojutsu.bijuu.jinchuriki.TailedBeastRegistry;
import com.zamu.dojutsu.domain.bijuu.BeastStats;
import com.zamu.dojutsu.domain.bijuu.SealingTable;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
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
 * Comando de depuracion {@code /dojutsubeast} para validar en runtime las Bestias con Cola de F7 (DJT-160). En el
 * datapack se invocan con huevos de spawn (items, F8); hasta entonces, este es el unico disparador.
 *
 * <ul>
 *   <li>{@code summon &lt;beast&gt;} — invoca la bestia delante de ti, con su barra de jefe.</li>
 *   <li>{@code info &lt;beast&gt;} — sus numeros: vida, tecnica insignia, ritmo de IA y recompensas.</li>
 *   <li>{@code killall} — retira todas las bestias del mundo (y sus barras).</li>
 *   <li>{@code seal &lt;beast&gt;} — te la sella dentro (F7/DJT-165): el disparador real es el item de chakra
 *       que suelta al morir, y llega en F8.</li>
 *   <li>{@code extract} — te la saca y devuelve todo lo prestado.</li>
 *   <li>{@code form} — alterna la forma de jinchuriki (lo mismo que la tecla K).</li>
 *   <li>{@code baryon} — alterna el Modo Baryon, que es terminal.</li>
 *   <li>{@code registry} — que bestias estan tomadas y por quien (el registro global de la partida).</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b>, como los cuatro de F6.
 */
public final class BeastDebugCommand {

    /** Sugiere las diez bestias. */
    private static final SuggestionProvider<CommandSourceStack> BEASTS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(TailedBeast.values()).map(TailedBeast::id), builder);

    /** Bloques por delante del invocador donde aparece la bestia (no encima de ti: ocupa 2x2.6). */
    private static final double SUMMON_DISTANCE = 6.0;

    private BeastDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsubeast")
                        .then(Commands.literal("summon")
                                .then(Commands.argument("beast", StringArgumentType.word())
                                        .suggests(BEASTS)
                                        .executes(BeastDebugCommand::summon)))
                        .then(Commands.literal("info")
                                .then(Commands.argument("beast", StringArgumentType.word())
                                        .suggests(BEASTS)
                                        .executes(BeastDebugCommand::info)))
                        .then(Commands.literal("killall").executes(BeastDebugCommand::killAll))
                        .then(Commands.literal("seal")
                                .then(Commands.argument("beast", StringArgumentType.word())
                                        .suggests(BEASTS)
                                        .executes(BeastDebugCommand::seal)))
                        .then(Commands.literal("extract").executes(BeastDebugCommand::extract))
                        .then(Commands.literal("form").executes(BeastDebugCommand::form))
                        .then(Commands.literal("baryon").executes(BeastDebugCommand::baryon))
                        .then(Commands.literal("registry").executes(BeastDebugCommand::registry))));
    }

    /**
     * Alterna la forma de jinchuriki (lo mismo que la tecla <b>K</b>). Existe porque en la verificacion en
     * partida faltaba: sellar una bestia por comando y no poder entrar en forma sin buscar la tecla dejaba la
     * mitad del sistema sin probar (F10).
     */
    private static int form(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean active = FormService.toggle(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "forma de jinchuriki " + (active ? "ACTIVADA" : "desactivada")), false);
        return active ? 1 : 0;
    }

    /**
     * Alterna el <b>Modo Baryon</b>, que es terminal: al agotarse, la bestia se va. Sin comando habia que
     * esperar a que el vinculo subiera solo (horas) para verlo una vez.
     */
    private static int baryon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean active = FormService.toggleBaryon(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "modo baryon " + (active ? "ACTIVADO (terminal: al agotarse la bestia se va)" : "desactivado")),
                false);
        return active ? 1 : 0;
    }

    private static int summon(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "beast").toLowerCase(Locale.ROOT);
        return TailedBeast.byId(id).map(beast -> {
            Vec3 spot = player.position().add(player.getLookAngle().scale(SUMMON_DISTANCE));
            TailedBeastEntity entity = TailedBeastEntity.summon(
                    (ServerLevel) player.level(), beast, spot.x, spot.y, spot.z);
            if (entity == null) {
                ctx.getSource().sendFailure(Component.literal("el mundo rechazo la entidad"));
                return 0;
            }
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "invocada " + beast.id() + " con " + (int) BeastStats.MAX_HEALTH + " de vida"), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("bestia desconocida: " + id));
            return 0;
        });
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "beast").toLowerCase(Locale.ROOT);
        return TailedBeast.byId(id).map(beast -> {
            boolean implemented = BeastMoveRegistry.get(beast.signatureMove()).isPresent();
            ctx.getSource().sendSuccess(() -> Component.literal(
                    beast.id() + " (" + beast.tails() + " colas)"
                            + " vida=" + (int) BeastStats.MAX_HEALTH
                            + " armadura=" + (int) BeastStats.ARMOR
                            + " | insignia=" + beast.signatureMove()
                            + (implemented ? " (implementada)" : " (pendiente)")
                            + " | IA cada " + BeastStats.aiIntervalTicks(beast) + "t"
                            + " con un jugador a " + (int) BeastStats.AI_ACTIVATION_RADIUS + " bloques"
                            + " | al morir: " + BeastStats.ninjaXpReward(beast) + " ninja_xp y "
                            + BeastStats.experienceReward(beast) + " de exp a "
                            + (int) BeastStats.REWARD_RADIUS + " bloques"), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("bestia desconocida: " + id));
            return 0;
        });
    }

    private static int seal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "beast").toLowerCase(Locale.ROOT);
        return TailedBeast.byId(id).map(beast -> {
            SealingService.SealOutcome outcome = SealingService.seal(player, beast);
            ctx.getSource().sendSuccess(() -> Component.literal(switch (outcome) {
                case SEALED -> "sellada " + beast.id() + ": +" + SealingTable.maxChakraBonus(beast)
                        + " de chakra maximo, +" + SealingTable.ninjaXp(beast) + " ninja_xp"
                        + ", elementos prestados: " + SealingTable.naturesOf(beast).size()
                        + SealingTable.kekkeiGenkaiOf(beast).map(kg -> " + KG " + kg).orElse("");
                case ALREADY_JINCHURIKI -> "ya llevas una bestia dentro (usa extract)";
                case BEAST_TAKEN -> "esa bestia ya tiene jinchuriki (AllowMultJinchuriki esta desactivado)";
            }), false);
            return outcome == SealingService.SealOutcome.SEALED ? 1 : 0;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("bestia desconocida: " + id));
            return 0;
        });
    }

    private static int extract(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return SealingService.extract(player).map(beast -> {
            ctx.getSource().sendSuccess(() -> Component.literal(
                    "extraida " + beast.id() + " y devuelto todo lo prestado"), false);
            return 1;
        }).orElse(0);
    }

    private static int registry(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        TailedBeastRegistry registry = SealingService.registryOf(player);
        StringBuilder sb = new StringBuilder("bestias tomadas: " + registry.taken() + "/"
                + TailedBeast.values().length + " (AllowMultJinchuriki="
                + com.zamu.dojutsu.core.config.DojutsuConfig.get().allowMultJinchuriki() + ")");
        for (TailedBeast beast : TailedBeast.values()) {
            registry.carrierOf(beast).ifPresent(carrier -> sb.append("\n  ").append(beast.id()).append(" -> ")
                    .append(nameOf(player, carrier)));
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    /** Nombre del portador si esta conectado; si no, su UUID (el registro sobrevive a las desconexiones). */
    private static String nameOf(ServerPlayer source, java.util.UUID carrier) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(carrier);
        return online != null ? online.getGameProfile().getName() : carrier.toString() + " (desconectado)";
    }

    private static int killAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        int[] removed = {0};
        level.getAllEntities().forEach(entity -> {
            if (entity instanceof TailedBeastEntity beast) {
                beast.discard(); // discard, no kill: no reparte recompensa ni deja la barra colgada
                removed[0]++;
            }
        });
        ctx.getSource().sendSuccess(() -> Component.literal("retiradas " + removed[0] + " bestia(s)"), false);
        return removed[0];
    }
}
