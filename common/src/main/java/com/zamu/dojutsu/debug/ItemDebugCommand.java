package com.zamu.dojutsu.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.domain.items.DnaTable;
import com.zamu.dojutsu.domain.items.DnaType;
import com.zamu.dojutsu.domain.items.MaskType;
import com.zamu.dojutsu.domain.items.PillTable;
import com.zamu.dojutsu.domain.items.ScrollType;
import com.zamu.dojutsu.items.DnaComponent;
import com.zamu.dojutsu.items.DnaService;
import com.zamu.dojutsu.items.MaskService;
import com.zamu.dojutsu.items.PillService;
import com.zamu.dojutsu.registry.ModDataComponents;
import com.zamu.dojutsu.registry.ModItems;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Comando de depuracion {@code /dojutsuitem} para validar en runtime los items de F8 (DJT-170) sin depender de la
 * medicina (identificar pide {@value DnaTable#IDENTIFY_MEDICAL}, implantar {@value DnaTable#IMPLANT_MEDICAL}) ni
 * del azar del implante (solo prende una de cada tres veces).
 *
 * <ul>
 *   <li>{@code get} — mascara puesta, resaca pendiente y los dos requisitos de medicina con tu nivel actual.</li>
 *   <li>{@code dna give [type]} — te da una muestra: sin argumento, <b>sin identificar</b>.</li>
 *   <li>{@code dna implant &lt;type&gt;} — intento <b>real</b>, con su tirada y sus consecuencias.</li>
 *   <li>{@code mask &lt;type&gt;} — te la pone (o te la quita si ya la llevabas).</li>
 *   <li>{@code pills} — te da un bote de {@value PillTable#PILLS_PER_ITEM} pildoras.</li>
 *   <li>{@code scroll &lt;type&gt;} — te da uno de los tres pergaminos (F10/DJT-225).</li>
 *   <li>{@code egg &lt;beast&gt;} — te da el huevo de una de las <b>nueve</b> bestias con huevo.</li>
 *   <li>{@code smoke_bomb} / {@code explosive_tag} / {@code ramen} — los tres restantes de DJT-225, para no
 *       tener que craftearlos al probar.</li>
 * </ul>
 *
 * <p><b>Andamiaje temporal</b>, como los de F6 y F7.
 */
public final class ItemDebugCommand {

    /** Sugiere los 26 tipos de ADN. */
    private static final SuggestionProvider<CommandSourceStack> DNA_TYPES = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(DnaType.values()).map(DnaType::id), builder);

    /** Sugiere las seis mascaras. */
    private static final SuggestionProvider<CommandSourceStack> MASKS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(MaskType.values()).map(MaskType::id), builder);

    /** Sugiere los tres pergaminos (F10/DJT-225). */
    private static final SuggestionProvider<CommandSourceStack> SCROLLS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(Arrays.stream(ScrollType.values()).map(ScrollType::id), builder);

    /** Sugiere las nueve bestias que tienen huevo (el Diez Colas no lo tiene; F10/DJT-225). */
    private static final SuggestionProvider<CommandSourceStack> EGG_BEASTS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ModItems.BEAST_EGGS.keySet().stream().map(TailedBeast::id), builder);

    private ItemDebugCommand() {
    }

    /** Suscribe el registro del comando. Debe llamarse durante la inicializacion del mod. */
    public static void register() {
        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(DebugCommands.root("dojutsuitem")
                        .then(Commands.literal("get").executes(ItemDebugCommand::get))
                        .then(Commands.literal("dna")
                                .then(Commands.literal("give")
                                        .executes(ctx -> giveDna(ctx, null))
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(DNA_TYPES)
                                                .executes(ctx -> giveDna(ctx,
                                                        StringArgumentType.getString(ctx, "type")))))
                                .then(Commands.literal("implant")
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .suggests(DNA_TYPES)
                                                .executes(ItemDebugCommand::implantDna))))
                        .then(Commands.literal("mask")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(MASKS)
                                        .executes(ItemDebugCommand::mask)))
                        .then(Commands.literal("pills").executes(ItemDebugCommand::pills))
                        .then(Commands.literal("scroll")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(SCROLLS)
                                        .executes(ItemDebugCommand::scroll)))
                        .then(Commands.literal("egg")
                                .then(Commands.argument("beast", StringArgumentType.word())
                                        .suggests(EGG_BEASTS)
                                        .executes(ItemDebugCommand::egg)))
                        .then(Commands.literal("smoke_bomb")
                                .executes(ctx -> giveStack(ctx, new ItemStack(ModItems.SMOKE_BOMB.get()))))
                        .then(Commands.literal("explosive_tag")
                                .executes(ctx -> giveStack(ctx, new ItemStack(ModItems.EXPLOSIVE_TAG.get()))))
                        .then(Commands.literal("ramen")
                                .executes(ctx -> giveStack(ctx, new ItemStack(ModItems.RAMEN.get()))))));
    }

    /** Entrega una pila y lo dice. Los items de DJT-225 no llevan componente: basta el item. */
    private static int giveStack(CommandContext<CommandSourceStack> ctx, ItemStack stack)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.getInventory().placeItemBackInInventory(stack);
        ctx.getSource().sendSuccess(() -> Component.literal("entregado " + stack.getItem()), false);
        return 1;
    }

    private static int scroll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        Optional<ScrollType> type = ScrollType.byId(id);
        if (type.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("pergamino desconocido: " + id));
            return 0;
        }
        return giveStack(ctx, new ItemStack(ModItems.SCROLLS.get(type.get()).get()));
    }

    private static int egg(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String id = StringArgumentType.getString(ctx, "beast").toLowerCase(Locale.ROOT);
        Optional<TailedBeast> beast = TailedBeast.byId(id).filter(ModItems.BEAST_EGGS::containsKey);
        if (beast.isEmpty()) {
            // el Diez Colas cae aqui: existe como bestia pero no tiene huevo (no tiene receta en el datapack)
            ctx.getSource().sendFailure(Component.literal("sin huevo para: " + id
                    + " (el Diez Colas se invoca con /dojutsubeast summon ten_tails)"));
            return 0;
        }
        return giveStack(ctx, new ItemStack(ModItems.BEAST_EGGS.get(beast.get()).get()));
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        PlayerNinjaData data = NinjaData.get(player);
        int medical = data.progression().skill(Skill.MEDICAL).level();
        ctx.getSource().sendSuccess(() -> Component.literal(
                "mascara=" + data.items().mask().orElse("(ninguna)")
                        + " resaca_pendiente=" + PillService.hangoverPending(player.getUUID())
                        + " | medicina " + medical
                        + ": identificar " + (DnaTable.canIdentify(medical) ? "SI" : "NO")
                        + " (" + DnaTable.IDENTIFY_MEDICAL + ")"
                        + ", implantar " + (DnaTable.canImplant(medical) ? "SI" : "NO")
                        + " (" + DnaTable.IMPLANT_MEDICAL + ")"
                        + " | el implante prende " + DnaTable.SUCCESS_CHANCE_PERCENT + "% de las veces"), false);
        return 1;
    }

    private static int giveDna(CommandContext<CommandSourceStack> ctx, String typeId) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack stack = new ItemStack(ModItems.DNA.get());
        if (typeId == null) {
            stack.set(ModDataComponents.DNA.get(), DnaComponent.UNIDENTIFIED);
            player.getInventory().placeItemBackInInventory(stack);
            ctx.getSource().sendSuccess(() -> Component.literal("ADN sin identificar entregado"), false);
            return 1;
        }
        String id = typeId.toLowerCase(Locale.ROOT);
        return DnaType.byId(id).map(type -> {
            stack.set(ModDataComponents.DNA.get(), DnaComponent.of(type));
            player.getInventory().placeItemBackInInventory(stack);
            ctx.getSource().sendSuccess(() -> Component.literal("ADN de " + type.id() + " entregado"), false);
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("ADN desconocido: " + id));
            return 0;
        });
    }

    private static int implantDna(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        return DnaType.byId(id).map(type -> {
            DnaTable.ImplantOutcome outcome = DnaService.implant(player, type);
            ctx.getSource().sendSuccess(() -> Component.literal("resultado: " + outcome), false);
            return outcome == DnaTable.ImplantOutcome.SUCCESS ? 1 : 0;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("ADN desconocido: " + id));
            return 0;
        });
    }

    private static int mask(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "type").toLowerCase(Locale.ROOT);
        return MaskType.byId(id).map(mask -> {
            boolean wearing = MaskService.worn(player).filter(worn -> worn == mask).isPresent();
            if (wearing) {
                MaskService.unequip(player);
                ctx.getSource().sendSuccess(() -> Component.literal("mascara retirada"), false);
            } else {
                MaskService.equip(player, mask);
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "mascara " + mask.id() + " puesta (tu nombre queda oculto)"), false);
            }
            return 1;
        }).orElseGet(() -> {
            ctx.getSource().sendFailure(Component.literal("mascara desconocida: " + id));
            return 0;
        });
    }

    private static int pills(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.RATION_PILLS.get()));
        ctx.getSource().sendSuccess(() -> Component.literal(
                "bote de " + PillTable.PILLS_PER_ITEM + " pildoras entregado (la resaca llega "
                        + PillTable.HANGOVER_DELAY_TICKS + "t despues del primer mordisco)"), false);
        return 1;
    }
}
