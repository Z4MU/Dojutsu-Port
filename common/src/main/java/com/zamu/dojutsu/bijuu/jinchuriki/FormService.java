package com.zamu.dojutsu.bijuu.jinchuriki;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.bijuu.ai.BeastMoveContext;
import com.zamu.dojutsu.bijuu.ai.BeastMoveRegistry;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.JinchurikiState;
import com.zamu.dojutsu.domain.bijuu.BeastAiPlan;
import com.zamu.dojutsu.domain.bijuu.BondTable;
import com.zamu.dojutsu.domain.bijuu.JinchurikiForm;
import com.zamu.dojutsu.domain.bijuu.JinchurikiFormContext;
import com.zamu.dojutsu.domain.bijuu.JinchurikiFormTable;
import com.zamu.dojutsu.domain.bijuu.TailedBeast;
import com.zamu.dojutsu.empowerment.ToggleableModeService;
import com.zamu.dojutsu.progression.XpApi;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * Forma de jinchuriki, vinculo y Modo Baryon {@code [S09]} (F7/DJT-166). <b>Cierra F7</b> y es el septimo
 * consumidor del arnes {@link ToggleableModeService}.
 *
 * <h2>Cierra el circulo de {@code BeastMove}</h2>
 * DJT-160 creo el contrato compartido y DJT-161 lo estreno desde la IA; aqui entra <b>el otro extremo</b>: el
 * jinchuriki lanza <b>la misma instancia</b> de la tecnica que le lanzaria la bestia. Lo unico que cambia es el
 * pipeline —el jugador paga chakra, la bestia no—, que es exactamente lo que decia el javadoc de {@code BeastMove}
 * antes de que existiera ningun jugador que pudiera probarlo. Las tecnicas solo se desbloquean a partir del nivel
 * de vinculo {@value JinchurikiFormTable#MOVES_LEVEL}: los dos primeros niveles de forma son <b>solo aura</b>.
 *
 * <h2>Los ojos se suprimen, no se apagan</h2>
 * {@code [ADD/04 §8]} pedia "la forma desactiva los ojos". El datapack los <b>suprime</b>: el ojo sigue equipado
 * pero {@code dojutsu/main} no ejecuta su bloque mientras {@code is_in_jinchuriki_form}. La diferencia importa,
 * porque la forma del Diez Colas <b>exige el Rinnegan activo</b> — apagarlo de verdad la apagaria a ella misma.
 * Implementado como supresion: {@link #suppressesEyes} lo consulta {@code EyeAbilityService}.
 *
 * <h2>El Baryon no es un modo caro: es terminal</h2>
 * Dura {@value JinchurikiFormTable#BARYON_DURATION_TICKS} ticks (10 minutos) y al agotarse <b>la bestia se va</b>
 * ({@code baryon_mode/out_of_time} llama a {@code removal/*}, con despedida incluida). No es un coste que se
 * pueda pagar dos veces.
 */
public final class FormService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Id del modo para {@code ModeToggledEvent}. */
    public static final String MODE_ID = "jinchuriki_form";

    /** Veto de entrada en forma por jugador ({@code d.jin_formcd}); ejecucion, no guardado. */
    private static final Map<UUID, Integer> FORM_COOLDOWNS = new ConcurrentHashMap<>();

    /** Veto de alternancia del Baryon ({@code d.baryon_cd}). */
    private static final Map<UUID, Integer> BARYON_COOLDOWNS = new ConcurrentHashMap<>();

    /** Quien tiene el Baryon encendido ahora mismo ({@code d.jin_baryonmode}); no sobrevive a la sesion. */
    private static final Set<UUID> BARYON_ACTIVE = ConcurrentHashMap.newKeySet();

    /** Cooldown de la tecnica de bestia por jugador ({@code d.jinform_timer}). */
    private static final Map<UUID, Integer> MOVE_COOLDOWNS = new ConcurrentHashMap<>();

    private static final ToggleableModeService<JinchurikiForm, JinchurikiFormContext> MODES =
            new ToggleableModeService<>(MODE_ID, JinchurikiForm::new, PlayerFormContext::new,
                    (player, context) -> onFall(player));

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        countDown(FORM_COOLDOWNS, id);
        countDown(BARYON_COOLDOWNS, id);
        countDown(MOVE_COOLDOWNS, id);
        PlayerNinjaData data = NinjaData.get(player);
        if (!data.jinchuriki().isJinchuriki()) {
            return; // caso comun: sin bestia no hay nada que tickear
        }
        accumulateBond(player, data);
        tickBaryon(player, data);
        MODES.tick(player);
    }

    /**
     * Enciende o apaga la forma.
     *
     * @return {@code true} si quedo encendida
     */
    public static boolean toggle(ServerPlayer player) {
        return MODES.toggle(player);
    }

    /** Indica si el jugador esta transformado. */
    public static boolean isActive(UUID playerId) {
        return MODES.isActive(playerId);
    }

    /**
     * Indica si la forma esta <b>suprimiendo</b> los ojos del jugador ({@code dojutsu/main}: cada ojo corre
     * {@code unless is_in_jinchuriki_form}). Lo consulta {@code EyeAbilityService}: el ojo sigue equipado, pero
     * no responde mientras dure la forma.
     */
    public static boolean suppressesEyes(UUID playerId) {
        return isActive(playerId);
    }

    /** Indica si el Modo Baryon esta encendido. */
    public static boolean baryonActive(UUID playerId) {
        return BARYON_ACTIVE.contains(playerId);
    }

    /**
     * Lanza la tecnica de la bestia desde la forma: <b>el otro extremo</b> del {@link BeastMoveContext} que ya
     * usa la IA. Exige estar en forma y tener el vinculo al nivel {@value JinchurikiFormTable#MOVES_LEVEL}.
     *
     * @return {@code true} si se lanzo
     */
    public static boolean castBeastMove(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        Optional<TailedBeast> beast = data.jinchuriki().sealedBeast().flatMap(TailedBeast::byId);
        if (beast.isEmpty() || !isActive(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.not_in_form")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (!JinchurikiFormTable.movesUnlocked(data.jinchuriki().level())) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.bond_too_weak")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (MOVE_COOLDOWNS.getOrDefault(player.getUUID(), 0) > 0) {
            player.displayClientMessage(Component.translatable("jinchuriki.dojutsu.move_cooldown")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        // el jinchuriki lanza la Bola Bestia con Cola; las tecnicas insignia llegan con su lote (ver DJT-161)
        return BeastMoveRegistry.get(BeastAiPlan.TAILED_BEAST_BALL).map(move -> {
            if (!ChakraService.consume(data, move.chakraCost())) {
                player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.no_chakra")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            move.execute(new BeastMoveContext(player, beast.get()));
            MOVE_COOLDOWNS.put(player.getUUID(), move.cooldownTicks());
            com.zamu.dojutsu.network.NinjaSync.sendDelta(player);
            return true;
        }).orElse(false);
    }

    /**
     * Enciende o apaga el Modo Baryon. Exige nivel {@value JinchurikiFormTable#BARYON_LEVEL},
     * {@value JinchurikiFormTable#BARYON_MIN_CHAKRA} de chakra y <b>no</b> ser el portador del Diez Colas.
     *
     * @return {@code true} si quedo encendido
     */
    public static boolean toggleBaryon(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        JinchurikiState state = data.jinchuriki();
        UUID id = player.getUUID();
        if (BARYON_ACTIVE.contains(id)) {
            deactivateBaryon(player, data);
            BARYON_COOLDOWNS.put(id, JinchurikiFormTable.BARYON_COOLDOWN_OFF);
            return false;
        }
        Optional<TailedBeast> beast = state.sealedBeast().flatMap(TailedBeast::byId);
        if (beast.isEmpty() || beast.get().isTenTails()) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.baryon_wrong_beast")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (state.level() < JinchurikiFormTable.BARYON_LEVEL) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.baryon_bond")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        if (data.chakra().chakra() < JinchurikiFormTable.BARYON_MIN_CHAKRA) {
            player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.baryon_no_chakra")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        // activate: el chakra maximo se DUPLICA y se rellena entero
        ChakraService.setMax(data, data.chakra().maxChakra() * 2, true);
        BARYON_ACTIVE.add(id);
        BARYON_COOLDOWNS.put(id, JinchurikiFormTable.BARYON_COOLDOWN_ON);
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.baryon_on")
                .withStyle(ChatFormatting.GOLD));
        com.zamu.dojutsu.network.NinjaSync.sendDelta(player);
        return true;
    }

    /** Olvida los estados de ejecucion (al desconectar o respawnear). */
    public static void forget(UUID playerId) {
        MODES.forget(playerId);
        FORM_COOLDOWNS.remove(playerId);
        BARYON_COOLDOWNS.remove(playerId);
        BARYON_ACTIVE.remove(playerId);
        MOVE_COOLDOWNS.remove(playerId);
    }

    /** Registra los payloads C2S. Llamar en la inicializacion del mod. */
    public static void register() {
        com.zamu.dojutsu.platform.PlatformServices.NETWORK.register(
                com.zamu.dojutsu.platform.NetworkService.Side.C2S,
                com.zamu.dojutsu.network.JinchurikiFormPayload.TYPE,
                com.zamu.dojutsu.network.JinchurikiFormPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        switch (payload.action()) {
                            case TOGGLE_FORM -> toggle(player);
                            case CAST_MOVE -> castBeastMove(player);
                            case TOGGLE_BARYON -> toggleBaryon(player);
                        }
                    }
                }));
    }

    /** Suma vinculo mientras portes la bestia y sube de nivel al cruzar su umbral ({@code bond_main}). */
    private static void accumulateBond(ServerPlayer player, PlayerNinjaData data) {
        JinchurikiState state = data.jinchuriki();
        TailedBeast beast = state.sealedBeast().flatMap(TailedBeast::byId).orElse(null);
        if (beast == null || !BondTable.hasBond(beast) || state.level() >= JinchurikiFormTable.MAX_LEVEL) {
            return;
        }
        int bond = state.bondTicks() + 1;
        if (!BondTable.levelsUp(beast, state.level(), bond)) {
            data.setJinchuriki(state.withBondTicks(bond));
            return;
        }
        JinchurikiState raised = state.withBondTicks(bond).withBondLevelUp();
        data.setJinchuriki(raised);
        ChakraService.setMax(data, data.chakra().maxChakra() + BondTable.LEVEL_UP_MAX_CHAKRA, false);
        XpApi.addNinjaXp(player, BondTable.LEVEL_UP_NINJA_XP);
        String key = raised.level() == BondTable.FRIENDSHIP_LEVEL
                ? "jinchuriki.dojutsu.befriended" : "jinchuriki.dojutsu.bond_up";
        player.sendSystemMessage(Component.translatable(key,
                Component.translatable("entity.dojutsu.tailed_beast." + beast.id())
                        .withStyle(s -> s.withColor(beast.chakraColor()))).withStyle(ChatFormatting.GOLD));
        player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.4f, 1f);
        LOGGER.info("[{}] El vinculo de {} con {} sube a {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), beast.id(), raised.level());
    }

    /** Cuenta atras del Baryon; al agotarse, la bestia se va ({@code baryon_mode/out_of_time}). */
    private static void tickBaryon(ServerPlayer player, PlayerNinjaData data) {
        UUID id = player.getUUID();
        if (!BARYON_ACTIVE.contains(id)) {
            return;
        }
        JinchurikiState state = data.jinchuriki();
        int left = state.baryonUse() - 1;
        if (left > 0) {
            data.setJinchuriki(state.withBaryonUse(left));
            return;
        }
        data.setJinchuriki(state.withBaryonUse(0));
        deactivateBaryon(player, data);
        MODES.deactivate(player);
        player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.farewell")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        SealingService.extract(player); // out_of_time: el Baryon se cobra la bestia
    }

    /** Apaga el Baryon devolviendo el chakra maximo duplicado ({@code baryon_mode/deactivate}). */
    private static void deactivateBaryon(ServerPlayer player, PlayerNinjaData data) {
        if (!BARYON_ACTIVE.remove(player.getUUID())) {
            return;
        }
        int halved = Math.max(data.chakra().maxChakra() / 2, 1);
        ChakraService.setMax(data, halved, false);
        ChakraService.set(data, halved / 2); // deactivate: te deja a la mitad del nuevo maximo
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.baryon_off")
                .withStyle(ChatFormatting.RED));
        com.zamu.dojutsu.network.NinjaSync.sendDelta(player);
    }

    /** Al caer la forma por chakra: avisa y limpia lo que colgaba de ella. */
    private static void onFall(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("jinchuriki.dojutsu.form_exhausted")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void countDown(Map<UUID, Integer> timers, UUID id) {
        int value = timers.getOrDefault(id, 0);
        if (value > 0) {
            timers.put(id, value - 1);
        }
    }

    /** Contexto del modo sobre un jugador real. */
    private record PlayerFormContext(ServerPlayer player) implements JinchurikiFormContext {

        @Override
        public Optional<TailedBeast> beast() {
            return NinjaData.get(player).jinchuriki().sealedBeast().flatMap(TailedBeast::byId);
        }

        @Override
        public int level() {
            return NinjaData.get(player).jinchuriki().level();
        }

        @Override
        public int chakra() {
            return NinjaData.get(player).chakra().chakra();
        }

        @Override
        public int formCooldown() {
            return FORM_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setFormCooldown(int ticks) {
            FORM_COOLDOWNS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public boolean hasRinneganActive() {
            return NinjaData.get(player).eye().poweredEye() // F11/DJT-230: apagado no cuenta como activo
                    .map(eye -> eye.startsWith("rinnegan") || eye.equals("rinne_sharingan"))
                    .orElse(false);
        }

        @Override
        public void addMaxChakra(int amount) {
            PlayerNinjaData data = NinjaData.get(player);
            ChakraService.setMax(data, Math.max(data.chakra().maxChakra() + amount, 1), false);
        }

        @Override
        public void applyAura(JinchurikiFormTable.FormAura aura) {
            // duracion 2 ticks (no 1) para que no parpadee entre ticks del pipeline
            add(MobEffects.DAMAGE_BOOST, aura.strength());
            add(MobEffects.MOVEMENT_SPEED, aura.speed());
            add(MobEffects.JUMP, aura.jumpBoost());
            add(MobEffects.DAMAGE_RESISTANCE, aura.resistance());
            add(MobEffects.FIRE_RESISTANCE, aura.fireResistance());
            if (player.tickCount % 5 == 0 && player.level() instanceof ServerLevel level) {
                Vec3 pos = player.position();
                int color = beast().map(TailedBeast::chakraColor).orElse(0xFF8000);
                level.sendParticles(dust(color), pos.x, pos.y + 0.5, pos.z, 10, 0.3, 1, 0.3, 0);
            }
        }

        private void add(Holder<MobEffect> effect, int amplifier) {
            player.addEffect(new MobEffectInstance(effect, 2, amplifier, true, false));
        }

        /** Polvo del color del chakra de la bestia: es lo que distingue una forma de otra a la vista. */
        private static DustParticleOptions dust(int rgb) {
            return new DustParticleOptions(new Vector3f(
                    ((rgb >> 16) & 0xFF) / 255F, ((rgb >> 8) & 0xFF) / 255F, (rgb & 0xFF) / 255F), 1F);
        }

        @Override
        public boolean baryonActive() {
            return BARYON_ACTIVE.contains(player.getUUID());
        }

        @Override
        public void setFlight(boolean flying) {
            if (player.isCreative() || player.isSpectator()) {
                return; // no tocar las habilidades de quien ya vuela por modo de juego
            }
            player.getAbilities().mayfly = flying || BARYON_ACTIVE.contains(player.getUUID());
            if (!player.getAbilities().mayfly) {
                player.getAbilities().flying = false;
            }
            player.onUpdateAbilities();
        }

        @Override
        public void notifyToggled(boolean active) {
            player.sendSystemMessage(Component.translatable(
                    active ? "jinchuriki.dojutsu.form_on" : "jinchuriki.dojutsu.form_off")
                    .withStyle(ChatFormatting.GOLD));
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(),
                        active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 0.5f, 2f);
            }
        }

        @Override
        public void notifyBlocked(JinchurikiForm.BlockReason reason) {
            String key = switch (reason) {
                case NOT_JINCHURIKI -> "jinchuriki.dojutsu.not_jinchuriki";
                case COOLDOWN -> "jinchuriki.dojutsu.form_cooldown";
                case NEEDS_RINNEGAN -> "jinchuriki.dojutsu.needs_rinnegan";
            };
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_RED), true);
        }
    }
}
