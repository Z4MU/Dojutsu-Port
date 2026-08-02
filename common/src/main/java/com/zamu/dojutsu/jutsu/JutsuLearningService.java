package com.zamu.dojutsu.jutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.items.JutsuComponent;
import com.zamu.dojutsu.items.JutsuItem;
import com.zamu.dojutsu.items.LearnerComponent;
import com.zamu.dojutsu.natures.LearnerService;
import com.zamu.dojutsu.registry.ModDataComponents;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

/**
 * Aprendizaje de jutsus con el Skill Learner (F4/DJT-075): el arbol {@code jutsu/learner/*} del
 * datapack como servicio. Cierra el {@code TODO F4/DJT-075} de {@code LearnerService.use} (F3).
 *
 * <p><b>Flujo fiel a {@code learn_<elem>.mcfunction}</b> (auditado, regla 16): el aprendizaje es
 * <b>secuencial</b> por elemento ({@link LearnSequence}); cada uso valida posesion del elemento →
 * "ya los aprendiste todos" (con cooldown, como el datapack) → cooldown ({@code d.lrn_cd} = 14
 * ticks, <b>uno por jugador</b>, compartido entre elementos) → entrega el <b>siguiente</b> jutsu:
 * lo marca en {@link JutsuKnowledge#known}, da el {@link JutsuItem} y el <b>ultimo de la
 * secuencia consume el learner</b> ({@code replace ... with air}).
 *
 * <p><b>Contenido incremental:</b> si el siguiente jutsu de la secuencia aun no esta en el
 * {@link JutsuRegistry} (su lote llega en DJT-085/086), se avisa y <b>no</b> se avanza la
 * secuencia ni se aplica cooldown. Un elemento sin secuencia no ensena (fiel: los learners de
 * yin/yang eran placeholders).
 *
 * <p>El cooldown de 14 ticks es <b>transitorio</b> (anti doble-click): no se persiste — un
 * reinicio del servidor lo limpia, sin el riesgo de tick absoluto de DJT-077 — y se olvida al
 * desconectar ({@link #forget}, mismo patron que el anti-spam de {@code MenuActionHandler}).
 * La escritura de {@code JutsuKnowledge} pasa solo por aqui, por la entrega del learner
 * ({@code LearnerService.deliver} resetea el progreso del elemento) y por el reset atomico.
 */
public final class JutsuLearningService {

    /** Cooldown del learner tras cada uso ({@code d.lrn_cd} del datapack): 14 ticks. */
    public static final int LEARN_COOLDOWN_TICKS = 14;

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Cooldown transitorio por jugador (tick de juego hasta el que esta en cd). */
    private static final Map<UUID, Long> LEARN_COOLDOWNS = new ConcurrentHashMap<>();

    private JutsuLearningService() {
    }

    /** Olvida el cooldown transitorio de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LEARN_COOLDOWNS.remove(playerId);
    }

    /** Resultado puro de evaluar un uso del learner (testeable sin Minecraft). */
    public record LearnOutcome(Status status, String jutsuId, int newProgress, boolean consumesLearner) {

        public enum Status {
            /** Ya aprendio todos los jutsus del elemento (aplica cooldown, como el datapack). */
            ALL_LEARNED,
            /** El learner esta en cooldown. */
            ON_COOLDOWN,
            /** El siguiente jutsu de la secuencia aun no tiene definicion cargada (lote pendiente). */
            CONTENT_PENDING,
            /** Aprende {@code jutsuId} y avanza a {@code newProgress}. */
            LEARN
        }

        static LearnOutcome of(Status status) {
            return new LearnOutcome(status, null, -1, false);
        }
    }

    /**
     * Decision pura del aprendizaje. La posesion del elemento la valida el llamador (es la
     * primera comprobacion del datapack, {@code jutsu/learner/check}).
     *
     * @param knowledge   conocimiento actual del jugador
     * @param sequence    secuencia del elemento del learner
     * @param onCooldown  si el learner del jugador esta en cooldown
     * @param jutsuLoaded si un id de jutsu tiene definicion cargada en el registry
     */
    public static LearnOutcome evaluate(JutsuKnowledge knowledge, LearnSequence sequence,
            boolean onCooldown, Predicate<String> jutsuLoaded) {
        Objects.requireNonNull(knowledge, "knowledge");
        Objects.requireNonNull(sequence, "sequence");
        Objects.requireNonNull(jutsuLoaded, "jutsuLoaded");
        int progress = knowledge.progressOf(sequence.element());
        if (progress >= sequence.size()) {
            // El datapack avisa "ya los tienes todos" ANTES que el cooldown (lrnd_all quita on_cd).
            return LearnOutcome.of(LearnOutcome.Status.ALL_LEARNED);
        }
        if (onCooldown) {
            return LearnOutcome.of(LearnOutcome.Status.ON_COOLDOWN);
        }
        String next = sequence.jutsuAt(progress);
        if (!jutsuLoaded.test(next)) {
            return LearnOutcome.of(LearnOutcome.Status.CONTENT_PENDING);
        }
        int newProgress = progress + 1;
        return new LearnOutcome(LearnOutcome.Status.LEARN, next, newProgress, newProgress >= sequence.size());
    }

    /**
     * Uso del Skill Learner (click derecho, server-side). Entrada unica desde
     * {@code LearnerItem}: valida componente y posesion (delegado en {@code LearnerService},
     * mensajes del datapack) y ejecuta la decision de {@link #evaluate}.
     */
    public static void use(ServerPlayer player, ItemStack learnerStack) {
        LearnerComponent component = learnerStack.get(ModDataComponents.LEARNER.get());
        if (component == null || !component.isValid()) {
            LOGGER.warn("[{}] Learner sin elemento valido en manos de {}: {}",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(),
                    component == null ? "(sin componente)" : component.element());
            return;
        }
        PlayerNinjaData data = NinjaData.get(player);
        if (!LearnerService.possesses(data, component)) {
            // jutsu/learner/check: mensaje exacto por tipo de elemento.
            player.sendSystemMessage(Component.translatable(component.nature().isPresent()
                    ? "learner.dojutsu.missing_nature"
                    : "learner.dojutsu.missing_kekkei_genkai"));
            return;
        }
        Component elementName = Component.translatable("element.dojutsu." + component.element());
        LearnSequence sequence = LearnSequenceRegistry.get(component.element()).orElse(null);
        if (sequence == null) {
            // Sin secuencia cargada: el learner no ensena (yin/yang del datapack, o lote pendiente).
            player.sendSystemMessage(Component.translatable("learner.dojutsu.jutsu_pending", elementName));
            return;
        }

        long now = player.serverLevel().getGameTime();
        boolean onCooldown = LEARN_COOLDOWNS.getOrDefault(player.getUUID(), Long.MIN_VALUE) > now;
        LearnOutcome outcome = evaluate(data.jutsu(), sequence, onCooldown,
                id -> JutsuRegistry.get(id).isPresent());

        switch (outcome.status()) {
            case ALL_LEARNED -> {
                player.sendSystemMessage(Component.translatable("learner.dojutsu.all_learned", elementName));
                LEARN_COOLDOWNS.put(player.getUUID(), now + LEARN_COOLDOWN_TICKS);
            }
            case ON_COOLDOWN ->
                    player.sendSystemMessage(Component.translatable("learner.dojutsu.on_cooldown"));
            case CONTENT_PENDING ->
                    player.sendSystemMessage(Component.translatable("learner.dojutsu.jutsu_pending", elementName));
            case LEARN -> applyLearn(player, data, learnerStack, sequence, outcome);
        }
    }

    /** Aplica un aprendizaje: estado, item de jutsu, cooldown y consumo del learner si es el ultimo. */
    private static void applyLearn(ServerPlayer player, PlayerNinjaData data, ItemStack learnerStack,
            LearnSequence sequence, LearnOutcome outcome) {
        data.setJutsu(data.jutsu()
                .withKnown(outcome.jutsuId())
                .withProgress(sequence.element(), outcome.newProgress()));

        ItemStack jutsuStack = JutsuItem.createStack(outcome.jutsuId());
        if (!player.getInventory().add(jutsuStack)) {
            player.drop(jutsuStack, false);
        }
        LEARN_COOLDOWNS.put(player.getUUID(), player.serverLevel().getGameTime() + LEARN_COOLDOWN_TICKS);

        Component jutsuName = Component.translatable("jutsu.dojutsu." + outcome.jutsuId());
        player.sendSystemMessage(Component.translatable("learner.dojutsu.learned", jutsuName));
        String next = sequence.jutsuAt(outcome.newProgress());
        if (next != null) {
            // El datapack mostraba el siguiente jutsu en el lore del learner; aqui, por chat.
            player.sendSystemMessage(Component.translatable("learner.dojutsu.next",
                    Component.translatable("jutsu.dojutsu." + next)));
        }
        if (outcome.consumesLearner()) {
            learnerStack.shrink(1); // el ultimo jutsu consume el learner (replace ... with air)
        }
        LOGGER.info("[{}] {} aprendio el jutsu {} ({} {}/{}).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), outcome.jutsuId(),
                sequence.element(), outcome.newProgress(), sequence.size());
    }

    /**
     * Retira del inventario los items de jutsu cuyas tecnicas el jugador <b>ya no conoce</b>
     * (tras un reset). Extiende la politica "sin residuos" de {@code removeStaleLearners}
     * ({@code [S03-R2]}) a los items de jutsu.
     *
     * @return cuantos items retiro
     */
    public static int removeStaleJutsuItems(ServerPlayer player) {
        JutsuKnowledge knowledge = NinjaData.get(player).jutsu();
        int removed = player.getInventory().clearOrCountMatchingItems(
                stack -> {
                    JutsuComponent component = stack.get(ModDataComponents.JUTSU.get());
                    return component != null && !knowledge.knows(component.jutsuId());
                }, -1, player.inventoryMenu.getCraftSlots());
        if (removed > 0) {
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.slotsChanged(player.getInventory());
            LOGGER.info("[{}] {} perdio {} item(s) de jutsu no conocidos.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), removed);
        }
        return removed;
    }
}
