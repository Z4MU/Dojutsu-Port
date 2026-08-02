package com.zamu.dojutsu.dojutsu.susanoo;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.SusanooState;
import com.zamu.dojutsu.domain.event.ModeToggledEvent;
import com.zamu.dojutsu.domain.susanoo.SusanooContext;
import com.zamu.dojutsu.domain.susanoo.SusanooCostTable;
import com.zamu.dojutsu.domain.susanoo.SusanooMode;
import com.zamu.dojutsu.domain.susanoo.SusanooTier;
import com.zamu.dojutsu.dojutsu.EyeStrainService;
import com.zamu.dojutsu.empowerment.ToggleableModeService;
import com.zamu.dojutsu.entity.AbilityAnchor;
import com.zamu.dojutsu.progression.LevelingService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
 * Servicio del Susanoo {@code [S24]} (DJT-120): la <b>glue</b> del {@link SusanooMode} — mantiene un modo por
 * jugador, lo tickea en {@link TickPhase#DOJUTSU}, y traduce sus hooks a Minecraft (chakra, fatiga, efectos).
 * Tambien lleva la <b>progresion de maestria</b>, que no es parte del ciclo del modo: mientras esta activo suma
 * uso y sube de nivel al alcanzar {@link SusanooCostTable#USE_PER_LEVEL}.
 *
 * <p><b>Sin tecla nueva:</b> el Susanoo es una habilidad declarada del Mangekyō, asi que se enciende desde el
 * menu ocular + tecla R como el resto ({@code SusanooAbility}). Volver a lanzarla <b>sube de etapa</b> hasta el
 * nivel dominado, y una vez en el tope lo apaga (ver {@link #toggle}); el avatar (DJT-121) crece con la etapa.
 *
 * <p><b>El estado de encendido es de ejecucion, no de guardado</b> (ver {@link SusanooState}): al desconectar se
 * olvida, como {@code check_deactivate} lo apaga al perder el ojo.
 *
 * <h2>Sobre el arnes compartido (F10/DJT-226)</h2>
 * El {@link ToggleableModeService} de F6/DJT-130 se extrajo <b>precisamente</b> del bucle que este servicio y
 * {@code TenseiganChakraModeService} repetian palabra por palabra, pero los dos se quedaron sin adoptarlo: eran
 * los dos ultimos modos fuera del arnes. Ya no. El hospedaje —modo por jugador, tick, caida, evento y sync— vive
 * en el arnes; aqui queda lo propio del Susanoo, que es lo interesante: la <b>escalera de etapas</b>, el avatar
 * y la maestria.
 *
 * <p>La escalera es la razon de que este servicio <b>no</b> use {@code MODES.toggle()} como los demas: una
 * pulsacion aqui no es "enciende o apaga", son <b>tres</b> respuestas posibles ({@link #decide}). El arnes da
 * {@code activate}/{@code deactivate} por separado justo para esto.
 */
public final class SusanooService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Id del modo para {@link ModeToggledEvent} (mismo id que la habilidad del menu). */
    public static final String MODE_ID = "susanoo";

    /** Temporizador de pulso por jugador (ticks hasta el proximo cobro); ejecucion, no guardado. */
    private static final Map<UUID, Integer> PULSE_TIMERS = new ConcurrentHashMap<>();

    /** Etapa manifestada por jugador ({@code d.susanoo_stg}, 1..nivel dominado); ejecucion, no guardado. */
    private static final Map<UUID, Integer> STAGES = new ConcurrentHashMap<>();

    /** Avatar vivo por jugador (DJT-121): el ancla que envuelve al portador mientras el modo esta activo. */
    private static final Map<UUID, AbilityAnchor> AVATARS = new ConcurrentHashMap<>();

    /** El arnes compartido: hospeda el modo, lo tickea y retira el avatar cuando cae. */
    private static final ToggleableModeService<SusanooMode, SusanooContext> MODES =
            new ToggleableModeService<>(MODE_ID, SusanooMode::new, PlayerSusanooContext::new,
                    (player, context) -> onFall(player.getUUID()));

    @Override
    public TickPhase phase() {
        return TickPhase.DOJUTSU;
    }

    @Override
    public void tick(ServerPlayer player) {
        if (MODES.tick(player)) {
            accumulateUse(player); // solo se progresa mientras el Susanoo esta manifestado
        }
    }

    /** Al caer por chakra agotado (el apagado no paso por {@code deactivate}): el avatar se va con el. */
    private static void onFall(UUID playerId) {
        despawnAvatar(playerId);
        STAGES.remove(playerId);
    }

    /**
     * Manifiesta, <b>avanza de etapa</b> o apaga el Susanoo del jugador (lo llama {@code SusanooAbility}).
     *
     * <p>Reproduce la escalera del datapack ({@code advance}): la primera pulsacion lo manifiesta en la etapa 1
     * y cada siguiente lo hace crecer una etapa, <b>con tope en el nivel dominado</b> — no se puede manifestar
     * una etapa que no se domina.
     *
     * <p><b>Divergencia consciente (regla 16):</b> alli, al llegar al tope, volver a pulsar no hacia nada y se
     * apagaba desde el menu o al perder el ojo ({@code check_deactivate}); aqui, al llegar al tope, la siguiente
     * pulsacion <b>lo apaga</b>. Con una sola tecla, una escalera sin salida dejaria al jugador sin forma de
     * bajarse.
     *
     * @return {@code true} si quedo encendido
     */
    public static boolean toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        int stage = STAGES.getOrDefault(id, 1);
        switch (decide(MODES.isActive(id), stage, levelOf(player))) {
            case ACTIVATE -> {
                if (!MODES.activate(player)) {
                    return false; // vetado por falta del chakra minimo
                }
                STAGES.put(id, 1);
                AVATARS.put(id, SusanooAvatar.spawn(player));
                return true;
            }
            case ADVANCE -> {
                advanceStage(player, stage + 1);
                return true; // crece sin re-emitir el evento de encendido: sigue activo
            }
            default -> {
                deactivate(player);
                return false;
            }
        }
    }

    /** Que hace la siguiente pulsacion sobre el Susanoo. */
    enum ToggleAction {
        /** Manifestarlo en la etapa 1. */
        ACTIVATE,
        /** Hacerlo crecer una etapa (aun no se alcanzo el nivel dominado). */
        ADVANCE,
        /** Apagarlo (ya esta en la etapa tope). */
        DEACTIVATE
    }

    /**
     * Decision pura de la escalera: apagado → manifestar; activo por debajo del nivel dominado → crecer; activo
     * en el tope → apagar. Separada del glue para poder probarla sin arrancar Minecraft.
     */
    static ToggleAction decide(boolean active, int stage, int mastered) {
        if (!active) {
            return ToggleAction.ACTIVATE;
        }
        return stage < mastered ? ToggleAction.ADVANCE : ToggleAction.DEACTIVATE;
    }

    /** Sube una etapa y lo anuncia (el avatar la lee cada tick, no hay que reinvocarlo). */
    private static void advanceStage(ServerPlayer player, int stage) {
        STAGES.put(player.getUUID(), stage);
        player.sendSystemMessage(Component.translatable("susanoo.dojutsu.stage",
                Component.literal(String.valueOf(stage)).withStyle(ChatFormatting.AQUA),
                Component.literal(SusanooCostTable.tier(stage).name()).withStyle(ChatFormatting.AQUA)));
        player.playNotifySound(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** Apaga el modo y retira el avatar (nada de anclas colgadas). */
    private static void deactivate(ServerPlayer player) {
        MODES.deactivate(player); // limpieza del modo + evento, en el arnes
        despawnAvatar(player.getUUID());
        STAGES.remove(player.getUUID());
    }

    /** Descarta el avatar de un jugador si lo tenia. */
    private static void despawnAvatar(UUID playerId) {
        AbilityAnchor avatar = AVATARS.remove(playerId);
        if (avatar != null) {
            avatar.discard();
        }
    }

    /** Indica si el jugador tiene el Susanoo manifestado ahora mismo. */
    public static boolean isActive(UUID playerId) {
        return MODES.isActive(playerId);
    }

    /** Etapa manifestada ahora mismo (1..nivel dominado); 0 si esta apagado. */
    public static int stageOf(UUID playerId) {
        return isActive(playerId) ? STAGES.getOrDefault(playerId, 1) : 0;
    }

    /** Nivel de maestria del jugador (el tope de la escalera de etapas). */
    static int levelOf(ServerPlayer player) {
        return NinjaData.get(player).susanoo().level();
    }

    /** Olvida el modo, el pulso, la etapa y el avatar de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        MODES.forget(playerId);
        PULSE_TIMERS.remove(playerId);
        STAGES.remove(playerId);
        despawnAvatar(playerId);
    }

    /**
     * Suma uso y sube de nivel al alcanzar el umbral (reiniciando el uso y dando la XP del nivel).
     *
     * <p><b>Divergencia consciente del datapack (regla 16):</b> alli el uso solo se acumula
     * {@code unless susanoo_lvl_3p}, lo que deja los niveles 4, 5 y 6 <b>inalcanzables</b> — sus
     * {@code level/set_N}, costes y efectos existen pero son codigo muerto. Se trata como defecto de S24 y aqui
     * la maestria sube hasta 6, que es lo que la propia tabla del datapack describe.
     */
    private static void accumulateUse(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        SusanooState state = data.susanoo();
        int use = state.use() + 1;
        int newLevel = SusanooCostTable.levelAfterUse(state.level(), use);
        if (newLevel == state.level()) {
            data.setSusanoo(state.withUse(use));
            return;
        }
        SusanooState leveled = state.leveledUp();
        data.setSusanoo(leveled);
        SusanooTier tier = SusanooCostTable.tier(leveled.level());
        LevelingService.addNinjaXp(data, player.getUUID(), tier.levelUpXp(), DomainEvents.bus());
        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 0.2f, 0.8f);
        player.sendSystemMessage(Component.translatable("susanoo.dojutsu.level_up",
                Component.literal(String.valueOf(leveled.level())).withStyle(ChatFormatting.AQUA),
                Component.literal(tier.name()).withStyle(ChatFormatting.AQUA)));
        LOGGER.info("[{}] El Susanoo de {} subio a nivel {} ({}).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), leveled.level(), tier.name());
    }

    /** Contexto del modo sobre un jugador real: la unica pieza que conoce a la vez el modo y Minecraft. */
    private record PlayerSusanooContext(ServerPlayer player) implements SusanooContext {

        @Override
        public int level() {
            return NinjaData.get(player).susanoo().level();
        }

        @Override
        public boolean eternal() {
            return NinjaData.get(player).eye().eternal();
        }

        @Override
        public int pulseTimer() {
            return PULSE_TIMERS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setPulseTimer(int ticks) {
            PULSE_TIMERS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public int chakra() {
            return NinjaData.get(player).chakra().chakra();
        }

        @Override
        public boolean consumeChakra(int amount) {
            return amount <= 0 || ChakraService.consume(NinjaData.get(player), amount);
        }

        @Override
        public void addStrain(int amount) {
            EyeStrainService.addStrain(NinjaData.get(player), amount);
        }

        @Override
        public void applyAura(SusanooTier tier) {
            // duracion 2 ticks (no 1) para que el efecto no parpadee entre ticks del pipeline
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2, tier.resistanceAmplifier(),
                    true, false));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 2, tier.fireResistanceAmplifier(),
                    true, false));
            if (tier.slowness()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2,
                        SusanooCostTable.SLOWNESS_AMPLIFIER, true, false));
            }
            if (player.level() instanceof ServerLevel level) {
                Vec3 pos = player.position();
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 1.5, pos.z, 6, 0.5, 1.2, 0.5, 0);
            }
        }

        @Override
        public void notifyToggled(boolean active) {
            player.sendSystemMessage(Component.translatable(
                    active ? "susanoo.dojutsu.activated" : "susanoo.dojutsu.deactivated"));
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                        SoundSource.PLAYERS, 1.0f, active ? 1.0f : 1.5f);
            }
        }
    }
}
