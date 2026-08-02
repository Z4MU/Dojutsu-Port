package com.zamu.dojutsu.empowerment.soah;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EmpowermentState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.SkillLevelChangedEvent;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.soah.SoahMode;
import com.zamu.dojutsu.domain.soah.SoahModeContext;
import com.zamu.dojutsu.domain.soah.SoahTable;
import com.zamu.dojutsu.empowerment.ToggleableModeService;
import com.zamu.dojutsu.empowerment.cursedseal.CursedSealService;
import com.zamu.dojutsu.progression.XpApi;
import dev.architectury.utils.GameInstance;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * Servicio del <b>Sello de la Fuerza de un Centenar</b> {@code [S12]} (F6/DJT-150): la glue del
 * {@link SoahMode} sobre el arnes {@link ToggleableModeService}. <b>Cierra F6</b> y es el sexto y ultimo
 * consumidor del framework de modos.
 *
 * <p><b>Es una hucha de chakra, no una carga pasiva.</b> La ficha de {@code [S12]} pedia "carga lenta" y
 * "pulsos 200/400"; el datapack no tiene ninguna de las dos cosas. La reserva se llena <b>de golpe y a mano</b>
 * ({@link #recharge}, agachandote al activar) transfiriendo {@value SoahTable#RECHARGE_AMOUNT} de tu propio
 * chakra, y la devolucion ocurre <b>cada tick</b>, por tramos de 100/10/1. Portado como esta (regla 16).
 *
 * <p><b>Devuelve recurso en vez de cobrarlo</b>, unico entre los seis modos: por eso {@code onCost} no descuenta
 * nada, solo comprueba que quede reserva. Guardas 20000 hoy para que manana, cuando bajes de 5000, el sello te
 * rellene solo hasta el maximo.
 *
 * <p><b>Excluyente con el sello maldito</b> ({@code toggle}), tercera arista de exclusion de F6 junto a la de
 * sage↔sello (DJT-140).
 */
public final class SoahSealService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Id del modo para {@code ModeToggledEvent}. */
    public static final String MODE_ID = "soah";

    /** Veto de alternancia por jugador ({@code d.soah_cd}); ejecucion, no guardado. */
    private static final Map<UUID, Integer> COOLDOWNS = new ConcurrentHashMap<>();

    private static final ToggleableModeService<SoahMode, SoahModeContext> MODES =
            new ToggleableModeService<>(MODE_ID, SoahMode::new, PlayerSoahContext::new);

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        int cooldown = COOLDOWNS.getOrDefault(id, 0);
        if (cooldown > 0) {
            COOLDOWNS.put(id, cooldown - 1);
        }
        MODES.tick(player);
    }

    /**
     * Alterna el sello. Si el jugador va <b>agachado</b> y la reserva esta vacia, en vez de encenderlo lo
     * <b>recarga</b> — es el gesto del datapack ({@code toggle} → {@code recharge}).
     *
     * @param sneaking si el jugador estaba agachado al pulsar
     * @return {@code true} si el sello quedo encendido
     */
    public static boolean toggle(ServerPlayer player, boolean sneaking) {
        PlayerNinjaData data = NinjaData.get(player);
        boolean active = MODES.isActive(player.getUUID());
        if (sneaking && !active && data.empowerment().soahCharge() <= 0) {
            recharge(player);
            return false;
        }
        return MODES.toggle(player);
    }

    /**
     * Recarga el sello ({@code recharge}): pasa {@value SoahTable#RECHARGE_AMOUNT} de tu chakra a la reserva.
     * Todo-o-nada, como cualquier gasto de chakra del mod.
     *
     * @return {@code true} si la recarga se hizo
     */
    public static boolean recharge(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        if (!data.empowerment().soahGranted()) {
            player.sendSystemMessage(Component.translatable("soah.dojutsu.no_seal").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!ChakraService.consume(data, SoahTable.RECHARGE_AMOUNT)) {
            player.sendSystemMessage(Component.translatable("soah.dojutsu.recharge_failed")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        EmpowermentState state = data.empowerment();
        data.setEmpowerment(state.withSoahCharge(state.soahCharge() + SoahTable.RECHARGE_AMOUNT));
        player.sendSystemMessage(Component.translatable("soah.dojutsu.recharged",
                data.empowerment().soahCharge()).withStyle(ChatFormatting.AQUA));
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1f);
        com.zamu.dojutsu.network.NinjaSync.sendDelta(player);
        return true;
    }

    /** Indica si el sello esta activo. */
    public static boolean isActive(UUID playerId) {
        return MODES.isActive(playerId);
    }

    /** Olvida el modo y su veto (al desconectar). */
    public static void forget(UUID playerId) {
        MODES.forget(playerId);
        COOLDOWNS.remove(playerId);
    }

    /** Limpia el veto de alternancia. Solo la usa el comando de QA. */
    public static void clearCooldown(UUID playerId) {
        COOLDOWNS.remove(playerId);
    }

    /** Registra el payload C2S y el otorgamiento reactivo del sello. Llamar en la inicializacion del mod. */
    public static void register() {
        com.zamu.dojutsu.platform.PlatformServices.NETWORK.register(
                com.zamu.dojutsu.platform.NetworkService.Side.C2S,
                com.zamu.dojutsu.network.SoahSealPayload.TYPE,
                com.zamu.dojutsu.network.SoahSealPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        toggle(player, payload.sneaking());
                    }
                }));
        DomainEvents.bus().subscribe(SkillLevelChangedEvent.class, SoahSealService::onSkillLevelChanged);
    }

    /**
     * Otorga el sello al cruzar el hito de Ninjutsu Medico ({@code unlock}), reactivo e idempotente como el
     * resto de desbloqueos (patron de {@code UnlockService}). El <b>item</b> del sello llega en F8; el estado y
     * la XP son de aqui.
     */
    private static void onSkillLevelChanged(SkillLevelChangedEvent event) {
        if (!Skill.MEDICAL.name().toLowerCase(Locale.ROOT).equals(event.skillId())
                || !SoahTable.canUnlock(event.newLevel())) {
            return;
        }
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(event.player());
        if (player == null) {
            return;
        }
        PlayerNinjaData data = NinjaData.get(player);
        if (data.empowerment().soahGranted()) {
            return; // idempotente: ya lo tiene
        }
        data.setEmpowerment(data.empowerment().withSoahGranted(true));
        XpApi.addNinjaXp(player, SoahTable.UNLOCK_NINJA_XP);
        player.sendSystemMessage(Component.translatable("soah.dojutsu.granted").withStyle(ChatFormatting.AQUA));
        LOGGER.info("[{}] {} recibio el Sello de la Fuerza de un Centenar.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName());
    }

    /** Contexto del modo sobre un jugador real. */
    private record PlayerSoahContext(ServerPlayer player) implements SoahModeContext {

        @Override
        public boolean hasSeal() {
            return NinjaData.get(player).empowerment().soahGranted();
        }

        @Override
        public int storedChakra() {
            return NinjaData.get(player).empowerment().soahCharge();
        }

        @Override
        public int chakra() {
            return NinjaData.get(player).chakra().chakra();
        }

        @Override
        public int maxChakra() {
            return NinjaData.get(player).chakra().maxChakra();
        }

        @Override
        public int medicalLevel() {
            return NinjaData.get(player).progression().skill(Skill.MEDICAL).level();
        }

        @Override
        public int yinAffinity() {
            return NinjaData.get(player).natures().affinity(NatureType.YIN).level();
        }

        @Override
        public boolean cursedSealActive() {
            return CursedSealService.isActive(player.getUUID());
        }

        @Override
        public int cooldown() {
            return COOLDOWNS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setCooldown(int ticks) {
            COOLDOWNS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public void transferFromSeal(int amount) {
            PlayerNinjaData data = NinjaData.get(player);
            EmpowermentState state = data.empowerment();
            int moved = Math.min(amount, state.soahCharge());
            if (moved <= 0) {
                return;
            }
            data.setEmpowerment(state.withSoahCharge(state.soahCharge() - moved));
            ChakraService.set(data, Math.min(data.chakra().chakra() + moved, data.chakra().maxChakra()));
        }

        @Override
        public void emitAura() {
            if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel level) {
                Vec3 pos = player.position();
                level.sendParticles(new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.7f), 0.5f),
                        pos.x, pos.y + 2, pos.z, 1, 0, 0, 0, 0);
            }
        }

        @Override
        public void notifyActivated(int storedChakra) {
            player.sendSystemMessage(Component.translatable("soah.dojutsu.on").withStyle(ChatFormatting.GREEN));
            notifyStored(storedChakra);
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                        SoundSource.PLAYERS, 0.8f, 2f);
            }
        }

        @Override
        public void notifyDeactivated() {
            player.sendSystemMessage(Component.translatable("soah.dojutsu.off").withStyle(ChatFormatting.RED));
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                        SoundSource.PLAYERS, 0.8f, 0f);
            }
        }

        @Override
        public void notifyStored(int storedChakra) {
            player.sendSystemMessage(Component.translatable("soah.dojutsu.stored", storedChakra)
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        @Override
        public void notifyOutOfStoredChakra() {
            player.sendSystemMessage(Component.translatable("soah.dojutsu.empty")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            player.sendSystemMessage(Component.translatable("soah.dojutsu.how_to_recharge")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        @Override
        public void notifyBlocked(SoahMode.BlockReason reason) {
            String key = switch (reason) {
                case NO_SEAL -> "soah.dojutsu.no_seal";
                case CURSED_SEAL -> "soah.dojutsu.cursed_seal";
                case LOW_MEDICAL -> "soah.dojutsu.low_medical";
                case LOW_YIN -> "soah.dojutsu.low_yin";
                case LOW_MAX_CHAKRA -> "soah.dojutsu.low_max_chakra";
                case COOLDOWN -> "soah.dojutsu.cooldown";
                case EMPTY_SEAL -> "soah.dojutsu.empty";
            };
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
            if (reason == SoahMode.BlockReason.EMPTY_SEAL) {
                player.sendSystemMessage(Component.translatable("soah.dojutsu.how_to_recharge")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }
}
