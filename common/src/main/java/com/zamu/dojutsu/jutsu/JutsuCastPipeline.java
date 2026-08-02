package com.zamu.dojutsu.jutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.event.JutsuCastEvent;
import com.zamu.dojutsu.domain.jutsu.CastContext;
import com.zamu.dojutsu.domain.jutsu.CastFailureReason;
import com.zamu.dojutsu.domain.jutsu.CastResult;
import com.zamu.dojutsu.domain.jutsu.Jutsu;
import com.zamu.dojutsu.domain.jutsu.JutsuDefinition;
import com.zamu.dojutsu.jutsu.effect.JutsuEffectRegistry;
import com.zamu.dojutsu.network.NinjaSync;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

/**
 * Pipeline de casteo (F4/DJT-074): la secuencia canonica de todo lanzamiento de tecnica —
 * <b>validar (chakra → cd → naturaleza → afinidad, via {@link Jutsu#evaluatePreconditions}) →
 * consumir → cooldown → efecto → evento</b>. Es el unico camino de cast del mod (jutsus F4,
 * kenjutsu DJT-083, oculares F5, IA F8) y reemplaza las ~141 maquinas {@code <jutsu>/cast}.
 *
 * <p><b>Caster generico por construccion</b> (S17-R3): la decision usa {@link CastContext} y el
 * pago un {@link ChakraSink} funcional — un shinobi IA (F8) enchufa los suyos sin tocar nada.
 * El nucleo ({@link #cast(UUID, JutsuEntry, CastContext, ChakraSink, Runnable, DomainEventBus)})
 * es puro y testeable; el glue de jugador anade mensajes (los del datapack), sonido y sync.
 *
 * <p>Garantias:
 * <ul>
 *   <li><b>Todo-o-nada:</b> el chakra se consume solo si TODAS las puertas pasan, incluida la
 *       disponibilidad del efecto (castear un jutsu cuya familia no esta implementada no cuesta).</li>
 *   <li><b>Aislamiento de fallos:</b> un efecto que lanza excepcion no crashea el tick; el chakra
 *       ya esta gastado y el evento se emite igual (el datapack daba la XP <b>antes</b> del summon).</li>
 *   <li><b>Cooldown:</b> la comprobacion pasa por el contexto; la aplicacion (DJT-077) escribe el tick
 *       absoluto de mundo en {@code JutsuKnowledge.cooldowns} via el {@code cooldownApplier} del cast.</li>
 * </ul>
 */
public final class JutsuCastPipeline {

    /** Pago del coste de chakra del caster generico (todo-o-nada; el jugador usa ChakraService). */
    @FunctionalInterface
    public interface ChakraSink {

        /** Intenta consumir; {@code false} si no habia suficiente (no muta nada). */
        boolean consume(int amount);
    }

    /** Resultado tipado del pipeline (superset de {@link CastFailureReason} con los fallos del mod). */
    public enum CastOutcome {
        SUCCESS,
        /** El id no existe en el {@code JutsuRegistry}. */
        UNKNOWN_JUTSU,
        /** La familia de efecto no esta implementada (contenido incremental; sin coste). */
        EFFECT_UNAVAILABLE,
        /** El efecto no tiene objetivo valido ("Nothing nearby" del datapack; sin coste — DJT-076). */
        NO_TARGET,
        INSUFFICIENT_CHAKRA,
        ON_COOLDOWN,
        /** El caster no posee el Kekkei Genkai requerido ({@code check_kekkei_genkai}, DJT-091). */
        MISSING_KEKKEI_GENKAI,
        MISSING_NATURE,
        INSUFFICIENT_AFFINITY,
        INSUFFICIENT_SKILL;

        public boolean isSuccess() {
            return this == SUCCESS;
        }
    }

    private static final Logger LOGGER = LogUtils.getLogger();

    private JutsuCastPipeline() {
    }

    // ------------------------------------------------------------------
    // Nucleo puro (testeable sin Minecraft)
    // ------------------------------------------------------------------

    /**
     * Ejecuta la secuencia canonica para un caster generico. El efecto llega como {@link Runnable}
     * ya ligado (nivel/caster/entrada): este nucleo no conoce Minecraft.
     *
     * @param casterId     identidad del caster (jugador o IA)
     * @param entry        entrada del jutsu (el llamador ya la resolvio del registry)
     * @param ctx          contexto de precondiciones del caster
     * @param chakraSink   pago del coste (todo-o-nada)
     * @param effectRunner efecto a ejecutar tras cobrar, o {@code null} si no esta implementado
     * @param bus          bus de dominio donde emitir {@link JutsuCastEvent}
     */
    public static CastOutcome cast(UUID casterId, JutsuEntry entry, CastContext ctx,
            ChakraSink chakraSink, Runnable effectRunner, DomainEventBus bus) {
        return cast(casterId, entry, ctx, chakraSink, effectRunner, () -> true, () -> { }, bus);
    }

    /**
     * Variante completa del nucleo (DJT-076/077): la precondicion del efecto
     * ({@code JutsuEffect.canApply}, "Nothing nearby") se evalua tras las puertas del dominio y
     * antes de consumir; el {@code cooldownApplier} persiste el cooldown del caster tras el pago.
     *
     * @param effectApplicable precondicion del efecto (sin objetivo → {@code NO_TARGET}, sin coste)
     * @param cooldownApplier  aplica el cooldown del caster (jugador: escribe {@code JutsuKnowledge})
     */
    public static CastOutcome cast(UUID casterId, JutsuEntry entry, CastContext ctx,
            ChakraSink chakraSink, Runnable effectRunner, java.util.function.BooleanSupplier effectApplicable,
            Runnable cooldownApplier, DomainEventBus bus) {
        Objects.requireNonNull(casterId, "casterId");
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(chakraSink, "chakraSink");
        Objects.requireNonNull(effectApplicable, "effectApplicable");
        Objects.requireNonNull(cooldownApplier, "cooldownApplier");
        Objects.requireNonNull(bus, "bus");

        JutsuDefinition definition = entry.definition();
        // Disponibilidad del efecto ANTES de las puertas: sin familia implementada, sin coste.
        if (effectRunner == null) {
            return CastOutcome.EFFECT_UNAVAILABLE;
        }
        CastResult preconditions = asJutsu(definition).evaluatePreconditions(ctx);
        if (preconditions.isFailure()) {
            return outcomeOf(preconditions.failureReason().orElseThrow());
        }
        if (!effectApplicable.getAsBoolean()) {
            return CastOutcome.NO_TARGET; // "Nothing nearby": tras las puertas, antes del cobro
        }
        if (!chakraSink.consume(definition.chakraCost())) {
            return CastOutcome.INSUFFICIENT_CHAKRA; // defensivo: el sink es la autoridad final del pago
        }
        cooldownApplier.run(); // orden del ADD: consumir -> cooldown -> efecto
        try {
            effectRunner.run();
        } catch (RuntimeException e) {
            // Aislamiento de fallos: el chakra ya se pago y el cast ocurrio; un efecto roto no
            // crashea el tick ni suprime el evento (el datapack daba la XP antes del summon).
            LOGGER.error("[{}] Fallo el efecto '{}' del jutsu {} (cast de {}).",
                    DojutsuMod.MOD_NAME, definition.effectId(), definition.id(), casterId, e);
        }
        bus.publish(new JutsuCastEvent(casterId, definition.id()));
        return CastOutcome.SUCCESS;
    }

    /** Envuelve la definicion en el contrato de dominio para reutilizar su maquina de precondiciones. */
    private static Jutsu asJutsu(JutsuDefinition definition) {
        return new Jutsu() {
            @Override
            public JutsuDefinition definition() {
                return definition;
            }

            @Override
            public CastResult cast(CastContext ctx) {
                return evaluatePreconditions(ctx);
            }
        };
    }

    private static CastOutcome outcomeOf(CastFailureReason reason) {
        return switch (reason) {
            case INSUFFICIENT_CHAKRA -> CastOutcome.INSUFFICIENT_CHAKRA;
            case ON_COOLDOWN -> CastOutcome.ON_COOLDOWN;
            case MISSING_KEKKEI_GENKAI -> CastOutcome.MISSING_KEKKEI_GENKAI;
            case MISSING_NATURE -> CastOutcome.MISSING_NATURE;
            case INSUFFICIENT_AFFINITY -> CastOutcome.INSUFFICIENT_AFFINITY;
            case INSUFFICIENT_SKILL -> CastOutcome.INSUFFICIENT_SKILL;
        };
    }

    // ------------------------------------------------------------------
    // Glue de jugador
    // ------------------------------------------------------------------

    /**
     * Cast de un jugador por id de jutsu: resuelve el registry, construye contexto/pago sobre su
     * {@code PlayerNinjaData}, ejecuta el nucleo y traduce el resultado a los mensajes del datapack
     * (actionbar "Casted: X!", {@code failed_cast} con sonido de yunque, {@code check_affinity}).
     */
    public static CastOutcome cast(ServerPlayer player, String jutsuId) {
        JutsuEntry entry = JutsuRegistry.get(jutsuId).orElse(null);
        if (entry == null) {
            player.sendSystemMessage(Component.translatable("jutsu.dojutsu.unknown", jutsuId)
                    .withStyle(ChatFormatting.RED));
            return CastOutcome.UNKNOWN_JUTSU;
        }
        PlayerNinjaData data = NinjaData.get(player);
        long now = player.serverLevel().getGameTime();
        var effect = JutsuEffectRegistry.get(entry.definition().effectId()).orElse(null);
        Runnable effectRunner = effect == null ? null
                : () -> effect.apply(player.serverLevel(), player, entry);
        java.util.function.BooleanSupplier applicable = effect == null ? () -> true
                : () -> effect.canApply(player.serverLevel(), player, entry);
        Runnable cooldownApplier = () -> data.setJutsu(
                data.jutsu().withCooldown(entry.id(), now, entry.definition().cooldownTicks()));
        CastOutcome outcome = cast(player.getUUID(), entry, new PlayerCastContext(data, now),
                amount -> ChakraService.consume(data, amount), effectRunner, applicable, cooldownApplier,
                DomainEvents.bus());
        feedback(player, entry, outcome);
        if (outcome.isSuccess()) {
            NinjaSync.sendDelta(player); // el chakra es observable: empuja el cambio al HUD
            LOGGER.info("[{}] {} casteo {} (coste {}).", DojutsuMod.MOD_NAME,
                    player.getGameProfile().getName(), entry.id(), entry.definition().chakraCost());
        }
        return outcome;
    }

    /** Mensajes fieles al datapack por resultado (actionbar; fallos generales con el yunque). */
    private static void feedback(ServerPlayer player, JutsuEntry entry, CastOutcome outcome) {
        Component jutsuName = Component.translatable("jutsu.dojutsu." + entry.id());
        switch (outcome) {
            case SUCCESS -> player.displayClientMessage(
                    Component.translatable("jutsu.dojutsu.casted", jutsuName).withStyle(ChatFormatting.GOLD), true);
            case INSUFFICIENT_CHAKRA, ON_COOLDOWN -> failedCast(player); // failed_cast: mensaje generico + yunque
            case MISSING_KEKKEI_GENKAI -> player.displayClientMessage(
                    Component.translatable("jutsu.dojutsu.missing_kekkei_genkai").withStyle(ChatFormatting.RED), true);
            case MISSING_NATURE -> player.displayClientMessage(
                    Component.translatable("jutsu.dojutsu.missing_nature").withStyle(ChatFormatting.RED), true);
            case INSUFFICIENT_AFFINITY -> {
                // Con afinidad multiple (jutsus KG), reporta la naturaleza concreta cuyo nivel no se alcanza.
                String natureId = deficientAffinity(player, entry);
                player.displayClientMessage(Component.translatable("jutsu.dojutsu.higher_affinity",
                        Component.translatable("element.dojutsu." + natureId)).withStyle(ChatFormatting.RED), true);
            }
            case INSUFFICIENT_SKILL -> {
                String skillId = entry.definition().skillRequirement().orElseThrow().skillId();
                player.displayClientMessage(Component.translatable("jutsu.dojutsu.higher_skill",
                        Component.translatable("skill.dojutsu." + skillId)).withStyle(ChatFormatting.RED), true);
            }
            case EFFECT_UNAVAILABLE -> player.displayClientMessage(
                    Component.translatable("jutsu.dojutsu.effect_pending", jutsuName).withStyle(ChatFormatting.RED), true);
            case NO_TARGET -> player.displayClientMessage(
                    Component.translatable("jutsu.dojutsu.no_target").withStyle(ChatFormatting.RED), true);
            case UNKNOWN_JUTSU -> {
                // Ya avisado en cast(player, jutsuId).
            }
        }
    }

    /**
     * La naturaleza cuyo nivel de afinidad no alcanza el requisito (la primera no cumplida). Para jutsus de
     * una sola afinidad es la unica; para los KG (varias componentes) es la deficiente concreta, para que el
     * mensaje "Requires higher X Affinity!" nombre la correcta. Fallback: la primera declarada.
     */
    private static String deficientAffinity(ServerPlayer player, JutsuEntry entry) {
        PlayerCastContext ctx = new PlayerCastContext(NinjaData.get(player), player.serverLevel().getGameTime());
        return entry.definition().affinityRequirements().stream()
                .filter(req -> ctx.affinity(req.natureId()) < req.amount())
                .findFirst()
                .or(() -> entry.definition().affinityRequirement())
                .map(com.zamu.dojutsu.domain.jutsu.AffinityRequirement::natureId)
                .orElse("unknown");
    }

    /** El {@code failed_cast} del datapack: actionbar rojo + campanazo de yunque (0.3). */
    private static void failedCast(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("jutsu.dojutsu.cast_failed").withStyle(ChatFormatting.RED), true);
        player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.MASTER, 0.3F, 1.0F);
    }
}
