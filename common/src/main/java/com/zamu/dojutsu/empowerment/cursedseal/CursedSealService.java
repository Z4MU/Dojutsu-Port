package com.zamu.dojutsu.empowerment.cursedseal;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EmpowermentState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.cursedseal.CursedSealMode;
import com.zamu.dojutsu.domain.cursedseal.CursedSealModeContext;
import com.zamu.dojutsu.domain.cursedseal.CursedSealTable;
import com.zamu.dojutsu.domain.cursedseal.CursedSealType;
import com.zamu.dojutsu.empowerment.ToggleableModeService;
import com.zamu.dojutsu.progression.XpApi;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
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
 * Servicio del <b>sello maldito</b> {@code [S10]} (F6/DJT-140): la inyeccion (una apuesta), el modo activable y
 * la maduracion/degradacion. Glue del {@link CursedSealMode} sobre el arnes {@link ToggleableModeService}.
 *
 * <p><b>Establece la plantilla de "sello"</b> ({@code #plantilla}): los seis comportamientos son <b>datos</b> en
 * {@link CursedSealType} (amplificadores por nivel), no seis clases — solo las <b>habilidades propias</b> del
 * nivel 2 (vuelo, aim-assist, clon, genjutsu sonoro) necesitaran clase propia, y van en lotes posteriores.
 *
 * <p><b>Es el modo que mata.</b> A diferencia del Susanoo o el sage —que solo se apagan al quedarse sin
 * recurso—, aqui {@code onPunish} <b>consume al portador</b> ({@code cursed_seal/punish}). Y en nivel 2 cada
 * muerte asi cuenta: a las {@value CursedSealTable#DEATHS_TO_DEGRADE} el sello degrada.
 */
public final class CursedSealService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Id del modo para {@code ModeToggledEvent}. */
    public static final String MODE_ID = "cursed_seal";

    /** Timers de cobro por jugador (ejecucion, no guardado): el {@code d.cseal_timer} del datapack. */
    private static final Map<UUID, Integer> DRAIN_TIMERS = new ConcurrentHashMap<>();

    private static final ToggleableModeService<CursedSealMode, CursedSealModeContext> MODES =
            new ToggleableModeService<>(MODE_ID, CursedSealMode::new, PlayerSealContext::new,
                    (player, context) -> DRAIN_TIMERS.remove(player.getUUID()));

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        MODES.tick(player);
    }

    /**
     * <b>Inyecta</b> un sello maldito: la apuesta del datapack ({@code inject}). Con
     * {@value CursedSealTable#SUCCESS_CHANCE_PERCENT}% prende un sello al azar de los seis; si no, el intento
     * castiga con daño, veneno y hambre — <b>no mata garantizado</b>, pero con poca vida puede acabar contigo.
     *
     * <p>La invocara el <b>Injector</b> cuando exista como item (vive en {@code items}, F8, y delega aqui);
     * hoy se dispara desde el comando de QA.
     *
     * @return el sello obtenido, o vacio si la inyeccion fallo
     */
    public static Optional<CursedSealType> inject(ServerPlayer player, RandomGenerator random) {
        PlayerNinjaData data = NinjaData.get(player);
        if (data.empowerment().hasCursedSeal()) {
            player.sendSystemMessage(Component.translatable("cursed_seal.dojutsu.already")
                    .withStyle(ChatFormatting.RED));
            return Optional.empty();
        }
        Optional<CursedSealType> rolled = CursedSealTable.inject(random);
        if (rolled.isEmpty()) {
            failInjection(player);
            return Optional.empty();
        }
        succeedInjection(player, data, rolled.get());
        return rolled;
    }

    /** Azar real (no inyectado): comodidad para el item/comando. */
    public static Optional<CursedSealType> inject(ServerPlayer player) {
        return inject(player, new Random());
    }

    /** Inyeccion fallida ({@code fail}): daño instantaneo, veneno y hambre. No mata garantizado. */
    private static void failInjection(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("cursed_seal.dojutsu.failed").withStyle(ChatFormatting.RED));
        player.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 1));           // instant_damage amp 1
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));       // poison 10s amp 2
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2));       // hunger 10s amp 2
    }

    /** Inyeccion exitosa ({@code success}): el sello prende, con sus bonos y su resaca. */
    private static void succeedInjection(ServerPlayer player, PlayerNinjaData data, CursedSealType type) {
        data.setEmpowerment(data.empowerment().withCursedSeal(type.id()));
        ChakraService.setMax(data, data.chakra().maxChakra() + CursedSealTable.SUCCESS_MAX_CHAKRA, false);
        XpApi.addNinjaXp(player, CursedSealTable.SUCCESS_NINJA_XP);
        XpApi.addSkillXp(player, Skill.SENJUTSU, CursedSealTable.SUCCESS_SENJUTSU);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)); // resaca: poison 5s amp 1
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 100, 1));
        player.sendSystemMessage(Component.translatable("cursed_seal.dojutsu.injected",
                Component.translatable("cursed_seal.dojutsu.type." + type.id())).withStyle(ChatFormatting.DARK_PURPLE));
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 0.5f);
        }
        LOGGER.info("[{}] {} recibio el sello maldito {}.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), type.id());
    }

    /**
     * Madura el sello a nivel 2 ({@code level_up}): efectos mas fuertes, coste <b>menor</b> y su habilidad
     * propia. En el datapack lo dispara el consumo de pildoras ({@code use_pills}); aqui es una accion directa
     * hasta que existan los items (F8).
     *
     * @return {@code true} si maduro en esta llamada
     */
    public static boolean levelUp(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        EmpowermentState state = data.empowerment();
        if (!state.hasCursedSeal() || state.cursedSealLevel() >= CursedSealTable.MAX_LEVEL) {
            return false;
        }
        data.setEmpowerment(state.withCursedSealLevel(CursedSealTable.MAX_LEVEL));
        XpApi.addNinjaXp(player, CursedSealTable.LEVEL_UP_NINJA_XP);
        player.sendSystemMessage(Component.translatable("cursed_seal.dojutsu.matured")
                .withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    /**
     * Enciende o apaga el sello.
     *
     * @return {@code true} si quedo encendido
     */
    public static boolean toggle(ServerPlayer player) {
        if (!NinjaData.get(player).empowerment().hasCursedSeal()) {
            player.sendSystemMessage(Component.translatable("cursed_seal.dojutsu.no_seal")
                    .withStyle(ChatFormatting.RED));
            return false;
        }
        return MODES.toggle(player);
    }

    /**
     * Indica si el jugador tiene el sello <b>activo</b>. Lo consulta {@code SageService}: el datapack hace
     * <b>excluyentes</b> el sello maldito y el modo sabio ({@code sage/toggle_check}).
     */
    public static boolean isActive(UUID playerId) {
        return MODES.isActive(playerId);
    }

    /** Olvida el modo y su timer (al desconectar). */
    public static void forget(UUID playerId) {
        MODES.forget(playerId);
        DRAIN_TIMERS.remove(playerId);
    }

    /** Registra el payload C2S de alternar el sello. Llamar en la inicializacion del mod. */
    public static void register() {
        com.zamu.dojutsu.platform.PlatformServices.NETWORK.register(
                com.zamu.dojutsu.platform.NetworkService.Side.C2S,
                com.zamu.dojutsu.network.ToggleCursedSealPayload.TYPE,
                com.zamu.dojutsu.network.ToggleCursedSealPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        toggle(player);
                    }
                }));
    }

    /** Contexto del modo sobre un jugador real. */
    private record PlayerSealContext(ServerPlayer player) implements CursedSealModeContext {

        @Override
        public Optional<CursedSealType> seal() {
            return NinjaData.get(player).empowerment().cursedSeal().flatMap(CursedSealType::byId);
        }

        @Override
        public int level() {
            return Math.max(NinjaData.get(player).empowerment().cursedSealLevel(), 1);
        }

        @Override
        public boolean consumeChakra(int amount) {
            return ChakraService.consume(NinjaData.get(player), amount);
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
        public void applyAura(CursedSealType type, int level) {
            // duracion 2 ticks (no 1) para que no parpadee entre ticks del pipeline
            addIfGranted(MobEffects.DAMAGE_BOOST, type.strength(level));
            addIfGranted(MobEffects.MOVEMENT_SPEED, type.speed(level));
            addIfGranted(MobEffects.JUMP, type.jumpBoost(level));
            addIfGranted(MobEffects.DAMAGE_RESISTANCE, type.resistance(level));
            if (player.tickCount % 10 == 0 && player.level() instanceof ServerLevel serverLevel) {
                Vec3 pos = player.position();
                serverLevel.sendParticles(ParticleTypes.WITCH, pos.x, pos.y + 1, pos.z, 5, 0.5, 0.8, 0.5, 0.01);
            }
        }

        /** Aplica el efecto solo si el sello lo otorga ({@link CursedSealType#NONE} = no lo da). */
        private void addIfGranted(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
                int amplifier) {
            if (amplifier != CursedSealType.NONE) {
                player.addEffect(new MobEffectInstance(effect, 2, amplifier, true, false));
            }
        }

        @Override
        public void consumeBearer() {
            PlayerNinjaData data = NinjaData.get(player);
            EmpowermentState state = data.empowerment().withCursedSealDeath();
            boolean degrades = CursedSealTable.degradesOnDeath(state.cursedSealLevel(), state.cursedSealDeaths());
            if (degrades) {
                state = state.withCursedSealLevel(1); // el sello degrada tras 10 muertes en nivel 2
            }
            data.setEmpowerment(state);
            player.sendSystemMessage(Component.translatable("cursed_seal.dojutsu.consumed")
                    .withStyle(ChatFormatting.DARK_RED));
            if (degrades) {
                player.sendSystemMessage(Component.translatable("cursed_seal.dojutsu.degraded")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            player.kill(); // "your Cursed Seal consumed you!"
            LOGGER.info("[{}] El sello maldito consumio a {}{}.", DojutsuMod.MOD_NAME,
                    player.getGameProfile().getName(), degrades ? " (degradado a nivel 1)" : "");
        }

        @Override
        public void notifyToggled(boolean active) {
            player.sendSystemMessage(Component.translatable(
                    active ? "cursed_seal.dojutsu.on" : "cursed_seal.dojutsu.off")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(),
                        active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 0.5f, 0.6f);
            }
        }
    }
}
