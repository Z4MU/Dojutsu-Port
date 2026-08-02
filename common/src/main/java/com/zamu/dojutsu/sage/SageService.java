package com.zamu.dojutsu.sage;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.SageState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.sage.SageMode;
import com.zamu.dojutsu.domain.sage.SageModeContext;
import com.zamu.dojutsu.domain.sage.SageModeTable;
import com.zamu.dojutsu.empowerment.ToggleableModeService;
import com.zamu.dojutsu.empowerment.cursedseal.CursedSealService;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Servicio del <b>modo sabio</b> {@code [S08]} (F6/DJT-136): la glue del {@link SageMode}. <b>Primer consumidor
 * del arnes compartido</b> {@link ToggleableModeService} (F6/DJT-130) — el arnes lleva el hospedaje (modo por
 * jugador, tick, caida, evento y sync) y aqui solo queda lo propio del sage: el contexto, la <b>subida de
 * nivel</b> por uso y el aviso del motivo cuando no se puede encender.
 *
 * <p><b>El nivel alarga el modo</b> ({@link SageModeTable}): gasta 1 de energia natural cada 100 ticks en el
 * nivel 1 y cada 500 en el nivel 5. Encenderlo, en cambio, exige <b>mas</b> energia cuanto menor es el nivel
 * (100 en el 1, 1 en el 4-5). Subir de nivel exige requisitos <b>compuestos</b> (chakra maximo + Senjutsu + uso).
 *
 * <p><b>Seis Caminos queda fuera</b> a proposito: en el datapack no se obtiene meditando, converge en F9
 * (narrativa). La ficha de diseño lo excluye de F6 — el hook natural es anadir el tipo a {@code SageType} y su
 * fuente de obtencion cuando llegue esa fase.
 */
public final class SageService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Id del modo para {@code ModeToggledEvent}. */
    public static final String MODE_ID = "sage";

    /** Timers de drenaje por jugador (ejecucion, no guardado): el {@code d.sage_timer} del datapack. */
    private static final java.util.Map<UUID, Integer> DRAIN_TIMERS = new java.util.concurrent.ConcurrentHashMap<>();

    /** El arnes compartido: hospeda el modo, lo tickea y avisa cuando cae. */
    private static final ToggleableModeService<SageMode, SageModeContext> MODES =
            new ToggleableModeService<>(MODE_ID, SageMode::new, PlayerSageContext::new,
                    (player, context) -> onFall(player));

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        if (MODES.tick(player)) {
            accumulateUse(player); // solo se progresa mientras el modo esta activo (sage/main)
        }
    }

    /**
     * Enciende o apaga el modo sabio del jugador.
     *
     * @return {@code true} si quedo encendido
     */
    public static boolean toggle(ServerPlayer player) {
        return MODES.toggle(player);
    }

    /** Indica si el jugador esta en modo sabio. */
    public static boolean isActive(UUID playerId) {
        return MODES.isActive(playerId);
    }

    /** Olvida el modo y su timer (al desconectar): el modo sabio no sobrevive a la sesion. */
    public static void forget(UUID playerId) {
        MODES.forget(playerId);
        DRAIN_TIMERS.remove(playerId);
    }

    /** Registra el payload C2S de alternar el modo. Llamar en la inicializacion del mod. */
    public static void register() {
        com.zamu.dojutsu.platform.PlatformServices.NETWORK.register(
                com.zamu.dojutsu.platform.NetworkService.Side.C2S,
                com.zamu.dojutsu.network.ToggleSagePayload.TYPE,
                com.zamu.dojutsu.network.ToggleSagePayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        toggle(player);
                    }
                }));
    }

    /** Al caer el modo (sin chakra o sin energia natural): avisa; el aura se disipa sola al no refrescarse. */
    private static void onFall(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("sage.dojutsu.mode.exhausted")
                .withStyle(ChatFormatting.GRAY));
        DRAIN_TIMERS.remove(player.getUUID());
    }

    /**
     * Suma uso del modo y sube de nivel al cumplir los requisitos <b>compuestos</b> ({@code sm_lvlreq_N}:
     * chakra maximo + Senjutsu + uso). Solo progresa mientras el modo esta activo, como el datapack.
     */
    private static void accumulateUse(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        SageState state = data.sage();
        int use = state.use() + 1;
        int newLevel = SageModeTable.levelAfter(state.level(), data.chakra().maxChakra(),
                data.progression().skill(Skill.SENJUTSU).level(), use);
        if (newLevel == state.level()) {
            data.setSage(state.withUse(use));
            return;
        }
        data.setSage(state.withUse(use).withLevel(newLevel));
        player.sendSystemMessage(Component.translatable("sage.dojutsu.mode.level_up",
                Component.literal(String.valueOf(newLevel)).withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GOLD));
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4f, 1.2f);
        LOGGER.info("[{}] El modo sabio de {} subio a nivel {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), newLevel);
    }

    /** Contexto del modo sobre un jugador real: la unica pieza que conoce a la vez el modo y Minecraft. */
    private record PlayerSageContext(ServerPlayer player) implements SageModeContext {

        @Override
        public boolean isSage() {
            return NinjaData.get(player).sage().sage();
        }

        @Override
        public int level() {
            return Math.max(NinjaData.get(player).sage().level(), 1);
        }

        @Override
        public int naturalEnergy() {
            return NinjaData.get(player).sage().charge();
        }

        @Override
        public boolean consumeNaturalEnergy(int amount) {
            PlayerNinjaData data = NinjaData.get(player);
            SageState state = data.sage();
            if (state.charge() < amount) {
                return false;
            }
            data.setSage(state.withCharge(state.charge() - amount));
            return true;
        }

        @Override
        public boolean hasChakra() {
            return ChakraService.has(NinjaData.get(player), 1);
        }

        @Override
        public boolean cursedSealActive() {
            // El datapack hace excluyentes sage y sello maldito (toggle_check): energia natural y chakra maldito
            // no se sostienen a la vez. Cableado en DJT-140.
            return CursedSealService.isActive(player.getUUID());
        }

        @Override
        public int drainTimer() {
            return DRAIN_TIMERS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setDrainTimer(int ticks) {
            DRAIN_TIMERS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public void applyAura() {
            // duracion 2 ticks (no 1) para que no parpadee entre ticks del pipeline; verificados de sage/effects
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2, 1, true, false));      // Fuerza II
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2, 1, true, false));    // Velocidad II
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 2, 3, true, false));              // Salto IV
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, 1, true, false)); // Resistencia II
            if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
                Vec3 pos = player.position();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.x, pos.y + 1, pos.z, 3, 0.5, 0.8, 0.5, 0.01);
            }
        }

        @Override
        public void notifyToggled(boolean active) {
            player.sendSystemMessage(Component.translatable(
                    active ? "sage.dojutsu.mode.on" : "sage.dojutsu.mode.off").withStyle(ChatFormatting.GOLD));
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(),
                        active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 0.6f, 1.4f);
            }
        }

        @Override
        public void notifyBlocked(SageMode.BlockReason reason) {
            String key = switch (reason) {
                case NOT_SAGE -> "sage.dojutsu.mode.not_sage";
                case CURSED_SEAL -> "sage.dojutsu.mode.cursed_seal";
                case NOT_ENOUGH_ENERGY -> "sage.dojutsu.mode.no_energy";
            };
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
        }
    }
}
