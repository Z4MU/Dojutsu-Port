package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.NatureProfile;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.natures.KekkeiGenkaiDefinition;
import com.zamu.dojutsu.natures.KekkeiGenkaiRegistry;
import com.zamu.dojutsu.natures.KekkeiGenkaiService;
import com.zamu.dojutsu.natures.NatureRoll;
import com.zamu.dojutsu.natures.NatureRollTable;
import com.zamu.dojutsu.natures.NatureService;
import com.zamu.dojutsu.natures.TemporaryGrantService;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsunatures get|grant <nature>|revoke <nature>} para validar en runtime las
 * naturalezas (DJT-051): idempotencia, ninja_xp simetrica, evento y persistencia.
 *
 * <p>Andamiaje temporal: se retira cuando el onboarding (DJT-059/060) y la GUI permitan obtener y observar las
 * naturalezas in-game. Muta el estado solo por la puerta {@link NatureService} (nunca directamente).
 *
 * <p>La naturaleza se resuelve <b>por nombre</b>, nunca por indice ({@code [ADD/07]}).
 */
public final class NatureDebugCommand {

    /** Sugiere los nombres de naturaleza validos (en minusculas, como los acepta el comando). */
    private static final SuggestionProvider<CommandSourceStack> NATURES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(NatureType.values()).map(NatureService::id), builder);

    /** Sugiere los nombres de Kekkei Genkai validos. */
    private static final SuggestionProvider<CommandSourceStack> KEKKEI_GENKAI = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(KekkeiGenkai.values()).map(KekkeiGenkaiService::id), builder);

    private NatureDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("dojutsunatures")
                        .then(Commands.literal("get").executes(NatureDebugCommand::get))
                        .then(Commands.literal("grant")
                                .then(Commands.argument("nature", StringArgumentType.word())
                                        .suggests(NATURES)
                                        .executes(ctx -> apply(ctx, true))))
                        .then(Commands.literal("revoke")
                                .then(Commands.argument("nature", StringArgumentType.word())
                                        .suggests(NATURES)
                                        .executes(ctx -> apply(ctx, false))))
                        .then(Commands.literal("aff")
                                .then(Commands.argument("nature", StringArgumentType.word())
                                        .suggests(NATURES)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 500))
                                                .executes(NatureDebugCommand::setAffinity))))
                        .then(Commands.literal("roll")
                                .then(Commands.argument("veces", IntegerArgumentType.integer(1, 1_000_000))
                                        .executes(ctx -> simulateRolls(ctx, false))
                                        .then(Commands.literal("senju")
                                                .executes(ctx -> simulateRolls(ctx, true)))))
                        .then(Commands.literal("tmp")
                                .then(Commands.literal("grant")
                                        .then(Commands.argument("nature", StringArgumentType.word())
                                                .suggests(NATURES)
                                                .then(Commands.argument("source", StringArgumentType.word())
                                                        .executes(NatureDebugCommand::grantTemporaryNature))))
                                .then(Commands.literal("grant_kg")
                                        .then(Commands.argument("kg", StringArgumentType.word())
                                                .suggests(KEKKEI_GENKAI)
                                                .then(Commands.argument("source", StringArgumentType.word())
                                                        .executes(NatureDebugCommand::grantTemporaryKekkeiGenkai))))
                                .then(Commands.literal("revoke")
                                        .then(Commands.argument("source", StringArgumentType.word())
                                                .executes(NatureDebugCommand::revokeTemporarySource))))
                        .then(Commands.literal("kg")
                                .then(Commands.literal("list").executes(NatureDebugCommand::listKekkeiGenkai))
                                .then(Commands.literal("grant")
                                        .then(Commands.argument("kg", StringArgumentType.word())
                                                .suggests(KEKKEI_GENKAI)
                                                .executes(ctx -> applyKekkeiGenkai(ctx, true))))
                                .then(Commands.literal("revoke")
                                        .then(Commands.argument("kg", StringArgumentType.word())
                                                .suggests(KEKKEI_GENKAI)
                                                .executes(ctx -> applyKekkeiGenkai(ctx, false)))))));
    }

    /**
     * Fija el nivel de afinidad de una naturaleza (andamiaje de prueba: sin esto, subir afinidad solo se logra
     * casteando jutsus del elemento, +1 por cast — inviable para probar los de tier alto). Usa la puerta
     * {@link NatureService#setAffinityLevel} (la misma del onboarding); rechaza {@code YIN_YANG} (derivada).
     */
    private static int setAffinity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "nature").toLowerCase(Locale.ROOT);
        NatureType nature = parse(name);
        if (nature == null) {
            ctx.getSource().sendFailure(Component.literal("naturaleza invalida: " + name));
            return 0;
        }
        if (!nature.storesAffinity()) {
            ctx.getSource().sendFailure(Component.literal(name + " no almacena afinidad (es derivada)"));
            return 0;
        }
        int level = IntegerArgumentType.getInteger(ctx, "level");
        PlayerNinjaData data = NinjaData.get(player);
        NatureService.setAffinityLevel(data, nature, level); // afinidad no observable: no cruza la red (server es autoridad)
        ctx.getSource().sendSuccess(() -> Component.literal(
                "afinidad " + name + " fijada a " + data.natures().affinity(nature).level()), false);
        return 1;
    }

    /** Muestra las definiciones cargadas del datapack (verifica el {@code /reload}). */
    private static int listKekkeiGenkai(CommandContext<CommandSourceStack> ctx) {
        Map<KekkeiGenkai, KekkeiGenkaiDefinition> all = KekkeiGenkaiRegistry.all();
        StringBuilder sb = new StringBuilder("kekkei_genkai cargados: " + all.size() + "/" + KekkeiGenkai.values().length);
        for (KekkeiGenkaiDefinition def : all.values()) {
            sb.append('\n').append(KekkeiGenkaiService.id(def.kekkeiGenkai()))
                    .append(" xp=").append(def.ninjaXp())
                    .append(" componentes=").append(def.components().stream().map(NatureService::id).toList());
            if (!def.affinityBonus().isEmpty()) {
                sb.append(" bonusAfinidad=").append(def.affinityBonus());
            }
        }
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return all.size();
    }

    private static int applyKekkeiGenkai(CommandContext<CommandSourceStack> ctx, boolean grant)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "kg").toLowerCase(Locale.ROOT);
        KekkeiGenkai kg = parseKekkeiGenkai(name);
        if (kg == null) {
            ctx.getSource().sendFailure(Component.literal("kekkei genkai invalido: " + name));
            return 0;
        }
        boolean applied = grant ? KekkeiGenkaiService.grant(player, kg) : KekkeiGenkaiService.revoke(player, kg);
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(applied
                ? (grant ? "otorgado " : "revocado ") + name
                        + " -> kg=" + data.natures().kekkeiGenkai()
                        + " naturalezas=" + data.natures().natures()
                        + " ninjaXp=" + data.progression().ninjaXp()
                : "sin efecto (" + (grant ? "ya lo tenia o sin definicion" : "no lo tenia") + "): " + name), false);
        return applied ? 1 : 0;
    }

    /**
     * Simula {@code veces} tiradas de la asignacion aleatoria y muestra el reparto observado. No aplica nada:
     * sirve para comprobar en runtime que la distribucion es la del datapack (DJT-054).
     */
    private static int simulateRolls(CommandContext<CommandSourceStack> ctx, boolean senju) {
        int times = IntegerArgumentType.getInteger(ctx, "veces");
        RandomGenerator random = new Random();
        Map<String, Integer> counts = new TreeMap<>();
        int claimed = 0;
        int genjutsu = 0;
        int secondAffinity = 0;
        for (int i = 0; i < times; i++) {
            NatureRollTable.Outcome outcome = NatureRoll.rollKekkeiGenkai(random, senju);
            String key = outcome.kekkeiGenkai().map(KekkeiGenkaiService::id)
                    .or(() -> outcome.nature().map(NatureService::id))
                    .orElse("(sin premio)");
            counts.merge(key, 1, Integer::sum);
            if (outcome.claimed()) {
                claimed++;
            } else if (NatureRoll.rollSecondAffinity(random)) {
                secondAffinity++;
            }
            if (NatureRoll.rollGenjutsu(random)) {
                genjutsu++;
            }
        }
        StringBuilder sb = new StringBuilder(times + " tiradas" + (senju ? " (clan Senju)" : "") + ":");
        counts.forEach((key, count) -> sb.append('\n').append(key).append(": ").append(count)
                .append(" (").append(String.format(Locale.ROOT, "%.1f", 100.0 * count / times)).append("%)"));
        sb.append("\ncon premio (rkg_gkg, +20 xp): ").append(pct(claimed, times));
        sb.append(" | 2a afinidad: ").append(pct(secondAffinity, times));
        sb.append(" | genjutsu/yin: ").append(pct(genjutsu, times));
        ctx.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static String pct(int count, int total) {
        return count + " (" + String.format(Locale.ROOT, "%.1f", 100.0 * count / total) + "%)";
    }

    private static int grantTemporaryNature(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "nature").toLowerCase(Locale.ROOT);
        String source = StringArgumentType.getString(ctx, "source");
        NatureType nature = parse(name);
        if (nature == null) {
            ctx.getSource().sendFailure(Component.literal("naturaleza invalida: " + name));
            return 0;
        }
        boolean applied = TemporaryGrantService.grantNature(player, nature, source);
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(applied
                ? "prestada " + name + " por " + source + " -> ninjaXp=" + data.progression().ninjaXp()
                : "sin efecto (ya la tenia): " + name), false);
        return applied ? 1 : 0;
    }

    private static int grantTemporaryKekkeiGenkai(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "kg").toLowerCase(Locale.ROOT);
        String source = StringArgumentType.getString(ctx, "source");
        KekkeiGenkai kg = parseKekkeiGenkai(name);
        if (kg == null) {
            ctx.getSource().sendFailure(Component.literal("kekkei genkai invalido: " + name));
            return 0;
        }
        boolean applied = TemporaryGrantService.grantKekkeiGenkai(player, kg, source);
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(applied
                ? "prestado " + name + " por " + source + " -> ninjaXp=" + data.progression().ninjaXp()
                : "sin efecto (ya lo tenia o sin definicion): " + name), false);
        return applied ? 1 : 0;
    }

    private static int revokeTemporarySource(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String source = StringArgumentType.getString(ctx, "source");
        int removed = TemporaryGrantService.revokeAllFrom(player, source);
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "retirados " + removed + " prestamo(s) de " + source
                        + " -> naturalezas=" + data.natures().natures()
                        + " kg=" + data.natures().kekkeiGenkai()
                        + " ninjaXp=" + data.progression().ninjaXp()), false);
        return removed;
    }

    /** Resuelve el nombre a un KG, o {@code null} si no existe. */
    private static KekkeiGenkai parseKekkeiGenkai(String name) {
        for (KekkeiGenkai kg : KekkeiGenkai.values()) {
            if (KekkeiGenkaiService.id(kg).equals(name)) {
                return kg;
            }
        }
        return null;
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        NatureProfile profile = data.natures();
        StringBuilder affinities = new StringBuilder();
        for (NatureType nature : NatureType.values()) {
            var stat = profile.affinity(nature);
            affinities.append(' ').append(NatureService.id(nature)).append('=')
                    .append(stat.level())
                    .append(nature.storesAffinity() ? "(xp" + stat.xp() + ")" : "(derivada)");
        }
        ctx.getSource().sendSuccess(() -> Component.literal(
                "naturalezas=" + profile.natures()
                        + " kg=" + profile.kekkeiGenkai()
                        + " hashiramaDna=" + profile.hashiramaDna()
                        + " ninjaXp=" + data.progression().ninjaXp()
                        + "\nprestadas=" + profile.temporary().natures()
                        + " kgPrestados=" + profile.temporary().kekkeiGenkai()
                        + "\nafinidades:" + affinities), false);
        return 1;
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, boolean grant) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "nature").toLowerCase(Locale.ROOT);
        NatureType nature = parse(name);
        if (nature == null) {
            ctx.getSource().sendFailure(Component.literal("naturaleza invalida: " + name));
            return 0;
        }
        boolean applied = grant ? NatureService.grant(player, nature) : NatureService.revoke(player, nature);
        int ninjaXp = NinjaData.get(player).progression().ninjaXp();
        ctx.getSource().sendSuccess(() -> Component.literal(applied
                ? (grant ? "otorgada " : "revocada ") + name + " -> ninjaXp=" + ninjaXp
                : "sin efecto (" + (grant ? "ya la tenia" : "no la tenia") + "): " + name), false);
        return applied ? 1 : 0;
    }

    /** Resuelve el nombre a una naturaleza, o {@code null} si no existe. */
    private static NatureType parse(String name) {
        for (NatureType nature : NatureType.values()) {
            if (NatureService.id(nature).equals(name)) {
                return nature;
            }
        }
        return null;
    }
}
