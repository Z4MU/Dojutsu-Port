package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.data.state.SusanooState;
import com.zamu.dojutsu.domain.eye.EyeFamily;
import com.zamu.dojutsu.domain.susanoo.SusanooCostTable;
import com.zamu.dojutsu.domain.susanoo.SusanooTier;
import com.zamu.dojutsu.dojutsu.EyeDefinition;
import com.zamu.dojutsu.dojutsu.EyeRegistry;
import com.zamu.dojutsu.dojutsu.susanoo.SusanooService;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Comando de depuracion {@code /dojutsudojutsu} para validar en runtime el sistema de ojos de F5 (DJT-113…118)
 * sin esperar las mecanicas time-gated: activar un ojo (probar menu N y habilidades R), fijar el uso (disparar
 * la evolucion), fijar la fatiga (probar weakness/blindness/apagado) y el flag eterno (eximir strain / EMS).
 *
 * <ul>
 *   <li>{@code get} — muestra el {@code EyeState} actual.</li>
 *   <li>{@code activate <eye>} — fija el ojo activo (de {@code EyeRegistry}) y marca su familia despierta.</li>
 *   <li>{@code deactivate} — apaga el ojo.</li>
 *   <li>{@code use <family> <level>} — fija el uso acumulado de una familia (dispara la evolucion).</li>
 *   <li>{@code strain <0-100>} — fija la fatiga ocular.</li>
 *   <li>{@code eternal <true|false>} — fija el flag eterno (EMS: exime de fatiga y de ceguera/apagado).</li>
 *   <li>{@code susanoo level <1-6>} — fija el nivel del Susanoo (DJT-120): recorre la tabla entera sin esperar
 *       los 32000 ticks de uso por nivel.</li>
 *   <li>{@code susanoo use <n>} — fija el uso acumulado (ponlo cerca del umbral para ver la subida de nivel).</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal:</b> se retira cuando el sistema de ojos sea plenamente obtenible in-game (despertar
 * real + assets). Muta el estado solo por la puerta {@code PlayerNinjaData.setEye} (nunca campos directos).
 * Ids de ojo/familia <b>por nombre</b>, nunca por indice ({@code [ADD/07]}).
 */
public final class EyeDebugCommand {

    /** Sugiere los ids de ojo cargados en el registro. */
    private static final SuggestionProvider<CommandSourceStack> EYES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(EyeRegistry.all().keySet(), builder);

    /** Sugiere los nombres de familia de ojo (en minusculas). */
    private static final SuggestionProvider<CommandSourceStack> FAMILIES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(EyeFamily.values()).map(f -> f.name().toLowerCase(Locale.ROOT)), builder);

    private EyeDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsudojutsu")
                        .then(Commands.literal("get").executes(EyeDebugCommand::get))
                        .then(Commands.literal("activate")
                                .then(Commands.argument("eye", StringArgumentType.word())
                                        .suggests(EYES)
                                        .executes(EyeDebugCommand::activate)))
                        .then(Commands.literal("deactivate").executes(EyeDebugCommand::deactivate))
                        .then(Commands.literal("toggle").executes(EyeDebugCommand::toggle))
                        .then(Commands.literal("use")
                                .then(Commands.argument("family", StringArgumentType.word())
                                        .suggests(FAMILIES)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(EyeDebugCommand::use))))
                        .then(Commands.literal("strain")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                        .executes(EyeDebugCommand::strain)))
                        .then(Commands.literal("eternal")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(EyeDebugCommand::eternal)))
                        .then(Commands.literal("susanoo")
                                .then(Commands.literal("level")
                                        .then(Commands.argument("value",
                                                        IntegerArgumentType.integer(1, SusanooCostTable.MAX_LEVEL))
                                                .executes(EyeDebugCommand::susanooLevel)))
                                .then(Commands.literal("use")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                .executes(EyeDebugCommand::susanooUse))))));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        SusanooState susanoo = data.susanoo();
        ctx.getSource().sendSuccess(() -> Component.literal(describe(data.eye())
                + " | susanoo=" + SusanooCostTable.tier(susanoo.level()).name()
                + " lvl=" + susanoo.level() + " uso=" + susanoo.use()
                + " activo=" + SusanooService.isActive(player.getUUID())), false);
        return 1;
    }

    private static int activate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String eyeId = StringArgumentType.getString(ctx, "eye").toLowerCase(Locale.ROOT);
        Optional<EyeDefinition> def = EyeRegistry.get(eyeId);
        if (def.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("ojo desconocido: " + eyeId));
            return 0;
        }
        PlayerNinjaData data = NinjaData.get(player);
        data.setEye(data.eye().withActiveEye(eyeId).withAwakened(def.get().family()));
        ctx.getSource().sendSuccess(() -> Component.literal("ojo activo: " + describe(data.eye())), false);
        return 1;
    }

    /**
     * Alterna el dojutsu pasando por {@link com.zamu.dojutsu.dojutsu.EyeToggleService} (F11/DJT-230), no
     * escribiendo el estado a mano: asi el QA cubre tambien el veto por fatiga y el rate-limit.
     */
    private static int toggle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean on = com.zamu.dojutsu.dojutsu.EyeToggleService.toggle(player);
        PlayerNinjaData data = NinjaData.get(player);
        ctx.getSource().sendSuccess(
                () -> Component.literal("toggle -> " + (on ? "ENCENDIDO" : "APAGADO") + ": " + describe(data.eye())),
                false);
        return 1;
    }

    private static int deactivate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        data.setEye(data.eye().withoutActiveEye());
        ctx.getSource().sendSuccess(
                () -> Component.literal("ojo RETIRADO (no es apagar, usa toggle): " + describe(data.eye())),
                false);
        return 1;
    }

    private static int use(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String familyName = StringArgumentType.getString(ctx, "family").toLowerCase(Locale.ROOT);
        EyeFamily family = parseFamily(familyName);
        if (family == null) {
            ctx.getSource().sendFailure(Component.literal("familia desconocida: " + familyName));
            return 0;
        }
        int level = IntegerArgumentType.getInteger(ctx, "level");
        PlayerNinjaData data = NinjaData.get(player);
        data.setEye(data.eye().withUseLevel(family, level));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "uso " + familyName + "=" + data.eye().useLevel(family) + " (la evolucion se evalua al tick)"), false);
        return 1;
    }

    private static int strain(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setEye(data.eye().withEyeStrain(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "eye_strain=" + data.eye().eyeStrain() + " (95=weakness, 98=blindness, 100=apaga; Rinne exento)"),
                false);
        return 1;
    }

    private static int eternal(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean value = BoolArgumentType.getBool(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setEye(data.eye().withEternal(value));
        ctx.getSource().sendSuccess(() -> Component.literal("eternal=" + data.eye().eternal()), false);
        return 1;
    }

    /**
     * Fija el nivel del Susanoo (1..6) reiniciando el uso: es el atajo para ver la tabla entera sin esperar los
     * 32000 ticks de uso por nivel — cada nivel cambia coste, intervalo de pulso, proteccion y lastre.
     */
    private static int susanooLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setSusanoo(new SusanooState(value, 0));
        SusanooTier tier = SusanooCostTable.tier(value);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "susanoo nivel=" + tier.level() + " (" + tier.name() + "): "
                        + tier.chakraPerPulse() + " chakra + " + tier.strainPerPulse() + " strain cada "
                        + tier.pulseTicks() + "t, resistance/fire amp " + tier.resistanceAmplifier()
                        + (tier.slowness() ? ", lastra (slowness)" : "")), false);
        return 1;
    }

    /** Fija el uso acumulado del Susanoo: ponlo en 31999 y enciendelo para ver la subida de nivel al instante. */
    private static int susanooUse(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int value = IntegerArgumentType.getInteger(ctx, "value");
        PlayerNinjaData data = NinjaData.get(player);
        data.setSusanoo(data.susanoo().withUse(value));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "susanoo uso=" + data.susanoo().use() + "/" + SusanooCostTable.USE_PER_LEVEL
                        + " (sube de nivel mientras esta activo)"), false);
        return 1;
    }

    private static EyeFamily parseFamily(String name) {
        for (EyeFamily family : EyeFamily.values()) {
            if (family.name().toLowerCase(Locale.ROOT).equals(name)) {
                return family;
            }
        }
        return null;
    }

    private static String describe(EyeState eye) {
        return "activeEye=" + eye.activeEye().orElse("(ninguno)")
                + " enabled=" + eye.enabled()
                + " eternal=" + eye.eternal()
                + " strain=" + eye.eyeStrain()
                + " selected=" + eye.selectedAbility().orElse("(ninguna)")
                + " awakened=" + eye.awakened()
                + " useLevels=" + eye.useLevels();
    }
}
