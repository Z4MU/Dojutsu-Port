package com.zamu.dojutsu.empowerment.eightgates;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.SleepChakraHandler;
import com.zamu.dojutsu.core.PlayerTickSystem;
import com.zamu.dojutsu.core.TickPhase;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EmpowermentState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.eightgates.EightGatesMode;
import com.zamu.dojutsu.domain.eightgates.EightGatesModeContext;
import com.zamu.dojutsu.domain.eightgates.EightGatesTable;
import com.zamu.dojutsu.domain.eightgates.Gate;
import com.zamu.dojutsu.domain.strain.StrainWarnings;
import com.zamu.dojutsu.empowerment.ToggleableModeService;
import java.util.Map;
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
 * Servicio de las <b>Ocho Puertas Internas</b> {@code [S11]} (F6/DJT-145): la glue del {@link EightGatesMode}
 * sobre el arnes {@link ToggleableModeService}. Quinto modo del arnes y el que mas <b>fuera</b> del modo deja:
 * la fatiga corporal baja y castiga <b>tambien con las puertas cerradas</b>, asi que la recuperacion, los avisos
 * y la resaca de 95/98 viven aqui, no en el modo.
 *
 * <p><b>Reparto</b> ({@code eight_gates/main}):
 * <ul>
 *   <li><b>El modo</b>: escalera de puertas, efectos de la puerta abierta y acumulacion de fatiga.</li>
 *   <li><b>El servicio</b>: recuperacion pasiva, avisos por banda, castigo de 95/98, los dos vetos
 *       ({@code d.eg_cd} / {@code d.eg_opencd}) y los reseteos de dormir, morir y respawnear.</li>
 * </ul>
 *
 * <p><b>Dos hallazgos del datapack que corrigen la ficha de {@code [S11]}</b> (regla 16):
 * <ol>
 *   <li><b>No hay atributos</b>. La ficha avisaba del riesgo S11-R4 ("atributos pegados"); el datapack usa
 *       {@code effect give} de duracion 1 tick refrescado cada tick. Aqui se refrescan con duracion 2 (para que
 *       no parpadeen entre ticks del pipeline) y <b>caducan solos</b>: el riesgo no existe.</li>
 *   <li><b>La 8ª puerta no mata</b>. No hay ningun {@code kill} en {@code eight_gates/}, ni siquiera en el Night
 *       Guy que la exige. Lo letal de verdad es acercarse a 100 de fatiga: la Puerta de la Muerte llega alli
 *       cuatro veces mas rapido y cerrar con la fatiga alta duele.</li>
 * </ol>
 */
public final class EightGatesService implements PlayerTickSystem {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Id del modo para {@code ModeToggledEvent}. */
    public static final String MODE_ID = "eight_gates";

    /** Ticks hasta el proximo mordisco de fatiga ({@code d.eg_stimer}); ejecucion, no guardado. */
    private static final Map<UUID, Integer> STRAIN_TIMERS = new ConcurrentHashMap<>();

    /** Ticks hasta poder recuperar fatiga ({@code d.egstimer}); corre siempre, tambien con puertas cerradas. */
    private static final Map<UUID, Integer> DECAY_TIMERS = new ConcurrentHashMap<>();

    /** Veto entre aperturas ({@code d.eg_opencd}). */
    private static final Map<UUID, Integer> OPEN_COOLDOWNS = new ConcurrentHashMap<>();

    /** Veto tras cerrar ({@code d.eg_cd}). */
    private static final Map<UUID, Integer> CLOSE_COOLDOWNS = new ConcurrentHashMap<>();

    /** Bandas de aviso ya emitidas ({@code d.egsw_*}), como en la fatiga ocular. */
    private static final Map<UUID, Set<Integer>> WARNED = new ConcurrentHashMap<>();

    private static final ToggleableModeService<EightGatesMode, EightGatesModeContext> MODES =
            new ToggleableModeService<>(MODE_ID, EightGatesMode::new, PlayerGatesContext::new);

    /** Id del clan que modula la recuperacion de fatiga ({@code clan/is_lee}). */
    private static final String LEE_CLAN = "lee";

    @Override
    public TickPhase phase() {
        return TickPhase.STRAIN;
    }

    @Override
    public void tick(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        UUID id = player.getUUID();
        boolean active = MODES.isActive(id);
        if (!active && data.empowerment().gateStrain() == 0 && !hasTimers(id)) {
            return; // caso comun: ni puertas abiertas ni fatiga que recuperar
        }
        countDown(OPEN_COOLDOWNS, id);
        countDown(CLOSE_COOLDOWNS, id);
        decayStrain(player, data);
        MODES.tick(player); // efectos de la puerta, acumulacion y cierre forzado a 100
        int strain = NinjaData.get(player).empowerment().gateStrain();
        punishHighStrain(player, strain); // la resaca se sufre tambien con las puertas ya cerradas
        warnStrain(player, strain, MODES.isActive(id));
    }

    /**
     * Abre la <b>siguiente</b> puerta (tecla sin agachar). La primera llamada abre la de Apertura; cada una
     * despues sube un escalon, si el taijutsu y los vetos lo permiten.
     *
     * @return {@code true} si se abrio una puerta
     */
    public static boolean openNext(ServerPlayer player) {
        if (!MODES.isActive(player.getUUID())) {
            return MODES.activate(player); // la 1ª pasa por el arnes, que publica el ModeToggledEvent
        }
        return MODES.modeOf(player).openNext(new PlayerGatesContext(player)); // subir escalon no cambia el modo
    }

    /** Cierra todas las puertas (tecla agachado), cobrando el coste de cierre. */
    public static void close(ServerPlayer player) {
        MODES.deactivate(player);
    }

    /**
     * Cierra las puertas <b>sin cobrar</b> ({@code safe_deactivate}): respawn y reset del ninja. La fatiga se
     * limpia por su cuenta en esos caminos, castigar dos veces seria injusto.
     */
    public static void closeSafely(ServerPlayer player) {
        EightGatesMode mode = MODES.modeOf(player);
        if (mode.isActive()) {
            mode.closeSafely(new PlayerGatesContext(player));
        }
    }

    /** Puerta abierta ahora mismo (1..8), o 0 si estan cerradas. */
    public static int stageOf(ServerPlayer player) {
        EightGatesMode mode = MODES.modeOf(player);
        return mode.isActive() ? mode.stage() : 0;
    }

    /** Indica si el jugador tiene alguna puerta abierta ({@code eight_gates/is_active}). */
    public static boolean isActive(UUID playerId) {
        return MODES.isActive(playerId);
    }

    /** Limpia los dos vetos ({@code d.eg_cd} y {@code d.eg_opencd}). Solo la usa el comando de QA. */
    public static void clearCooldowns(UUID playerId) {
        OPEN_COOLDOWNS.remove(playerId);
        CLOSE_COOLDOWNS.remove(playerId);
    }

    /** Olvida el modo y sus temporizadores (al desconectar). */
    public static void forget(UUID playerId) {
        MODES.forget(playerId);
        STRAIN_TIMERS.remove(playerId);
        DECAY_TIMERS.remove(playerId);
        OPEN_COOLDOWNS.remove(playerId);
        CLOSE_COOLDOWNS.remove(playerId);
        WARNED.remove(playerId);
    }

    /** Registra el payload C2S y el reset de fatiga al dormir. Llamar en la inicializacion del mod. */
    public static void register() {
        com.zamu.dojutsu.platform.PlatformServices.NETWORK.register(
                com.zamu.dojutsu.platform.NetworkService.Side.C2S,
                com.zamu.dojutsu.network.EightGatesPayload.TYPE,
                com.zamu.dojutsu.network.EightGatesPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        if (payload.close()) {
                            close(player);
                        } else {
                            openNext(player);
                        }
                    }
                }));
        SleepChakraHandler.registerWakeHandler(EightGatesService::resetStrain);
    }

    /**
     * Resetea la fatiga corporal a 0 ({@code sleep/wake_up} y {@code main/death}: las dos la ponen a cero).
     * Idempotente.
     */
    public static void resetStrain(PlayerNinjaData data) {
        EmpowermentState state = data.empowerment();
        if (state.gateStrain() != 0) {
            data.setEmpowerment(state.withGateStrain(0));
        }
    }

    /** Recuperacion pasiva: cada pulso baja la fatiga. Corre este con las puertas abiertas o cerradas. */
    private static void decayStrain(ServerPlayer player, PlayerNinjaData data) {
        EmpowermentState state = data.empowerment();
        UUID id = player.getUUID();
        int timer = DECAY_TIMERS.getOrDefault(id, 0);
        if (timer > 0) {
            DECAY_TIMERS.put(id, timer - 1);
            return;
        }
        boolean lee = data.clan().clan().map(LEE_CLAN::equals).orElse(false);
        DECAY_TIMERS.put(id, EightGatesTable.decayPulseTicks(lee));
        if (state.gateStrain() <= 0) {
            return; // sin fatiga no hay nada que recuperar (el pulso se rearma igual, como el datapack)
        }
        data.setEmpowerment(state.withGateStrain(
                state.gateStrain() - EightGatesTable.decayAmount(data.natures().hashiramaDna())));
    }

    /** Resaca de la fatiga alta ({@code strain/95_effects} y {@code 98_101_effects}), abiertas o no. */
    private static void punishHighStrain(ServerPlayer player, int strain) {
        if (strain >= EightGatesTable.PUNISH_HARD_AT) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 4, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 5, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4, true, false));
        } else if (strain >= EightGatesTable.PUNISH_AT) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 2, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30, 5, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, true, false));
        }
    }

    /**
     * Aviso {@code Body Strain: N%} una vez por banda ({@code d.egsw_*}), mismos cortes y colores que la fatiga
     * ocular ({@link StrainWarnings}). Solo avisa con las puertas abiertas; al cerrarlas se rearma todo.
     */
    private static void warnStrain(ServerPlayer player, int strain, boolean active) {
        Set<Integer> warned = WARNED.computeIfAbsent(player.getUUID(), id -> ConcurrentHashMap.newKeySet());
        if (!active) {
            warned.clear();
            return;
        }
        warned.removeIf(floor -> strain < floor);
        int floor = StrainWarnings.floorFor(strain);
        if (floor > StrainWarnings.NONE && warned.add(floor)) {
            player.sendSystemMessage(Component.empty()
                    .append(Component.translatable("eight_gates.dojutsu.strain")
                            .withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true)))
                    .append(Component.literal(": ").withStyle(s -> s.withColor(ChatFormatting.DARK_RED)))
                    .append(Component.literal(strain + "%").withStyle(s -> s.withColor(colorFor(floor)))));
            player.playNotifySound(SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 0.2f, 0.5f);
        }
    }

    /** Color por banda: 25 verde, 50 amarillo, 75 oro, 90 rojo, 99 rojo oscuro (igual que la fatiga ocular). */
    private static ChatFormatting colorFor(int floor) {
        return switch (floor) {
            case 25 -> ChatFormatting.GREEN;
            case 50 -> ChatFormatting.YELLOW;
            case 75 -> ChatFormatting.GOLD;
            case 90 -> ChatFormatting.RED;
            default -> ChatFormatting.DARK_RED;
        };
    }

    private static boolean hasTimers(UUID id) {
        return OPEN_COOLDOWNS.getOrDefault(id, 0) > 0 || CLOSE_COOLDOWNS.getOrDefault(id, 0) > 0;
    }

    private static void countDown(Map<UUID, Integer> timers, UUID id) {
        int value = timers.getOrDefault(id, 0);
        if (value > 0) {
            timers.put(id, value - 1);
        }
    }

    /** Contexto del modo sobre un jugador real. */
    private record PlayerGatesContext(ServerPlayer player) implements EightGatesModeContext {

        @Override
        public int taijutsuLevel() {
            return NinjaData.get(player).progression().skill(Skill.TAIJUTSU).level();
        }

        @Override
        public int strain() {
            return NinjaData.get(player).empowerment().gateStrain();
        }

        @Override
        public void addStrain(int amount) {
            PlayerNinjaData data = NinjaData.get(player);
            EmpowermentState state = data.empowerment();
            data.setEmpowerment(state.withGateStrain(state.gateStrain() + amount));
        }

        @Override
        public int strainTimer() {
            return STRAIN_TIMERS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setStrainTimer(int ticks) {
            STRAIN_TIMERS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public int openCooldown() {
            return OPEN_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setOpenCooldown(int ticks) {
            OPEN_COOLDOWNS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public int closeCooldown() {
            return CLOSE_COOLDOWNS.getOrDefault(player.getUUID(), 0);
        }

        @Override
        public void setCloseCooldown(int ticks) {
            CLOSE_COOLDOWNS.put(player.getUUID(), Math.max(ticks, 0));
        }

        @Override
        public void applyGateEffects(Gate gate) {
            // duracion 2 ticks (no 1) para que no parpadeen entre ticks del pipeline; caducan solos (S11-R4)
            addIfGranted(MobEffects.DAMAGE_BOOST, gate.strength());
            addIfGranted(MobEffects.MOVEMENT_SPEED, gate.speed());
            addIfGranted(MobEffects.JUMP, gate.jumpBoost());
            if (player.level() instanceof ServerLevel level) {
                auraFor(gate).ifPresent(aura -> {
                    Vec3 pos = player.position();
                    level.sendParticles(aura.options(), pos.x, pos.y + 0.5, pos.z, aura.count(), 0.3, 1, 0.3, 0);
                });
            }
        }

        private void addIfGranted(Holder<MobEffect> effect, int amplifier) {
            if (amplifier != Gate.NONE) {
                player.addEffect(new MobEffectInstance(effect, 2, amplifier, true, false));
            }
        }

        @Override
        public void notifyOpened(Gate gate) {
            player.sendSystemMessage(Component.translatable("eight_gates.dojutsu.opened",
                    Component.translatable("eight_gates.dojutsu.gate." + gate.id())
                            .withStyle(ChatFormatting.GOLD)));
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL,
                        SoundSource.PLAYERS, 0.5f, 2f);
            }
            LOGGER.debug("[{}] {} abrio la puerta {}.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), gate.id());
        }

        @Override
        public void notifyClosed(int strainAtClose) {
            // deactivate: cerrar con la fatiga alta cuesta salud y hambre; con la fatiga baja es gratis
            if (strainAtClose >= EightGatesTable.PUNISH_HARD_AT) {
                player.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 1));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 20));
                player.playNotifySound(SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1f, 0.5f);
            } else if (strainAtClose >= EightGatesTable.PUNISH_AT) {
                player.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0));
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 10));
            }
            player.sendSystemMessage(Component.translatable("eight_gates.dojutsu.closed")
                    .withStyle(ChatFormatting.RED));
            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                        SoundSource.PLAYERS, 0.5f, 2f);
            }
        }

        @Override
        public void notifyBlocked(EightGatesMode.BlockReason reason) {
            String key = switch (reason) {
                case LOW_TAIJUTSU -> "eight_gates.dojutsu.low_taijutsu";
                case CLOSE_COOLDOWN -> "eight_gates.dojutsu.cooldown";
                case OPEN_COOLDOWN -> "eight_gates.dojutsu.too_fast";
                case NO_MORE_GATES -> "eight_gates.dojutsu.no_more_gates";
            };
            if (reason == EightGatesMode.BlockReason.LOW_TAIJUTSU) {
                player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
                player.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.2f, 1f);
                return;
            }
            // el resto son avisos de ritmo: actionbar, como el datapack, para no ensuciar el chat
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        }

        /** Aura de particulas por puerta ({@code run}); las dos primeras no tienen. */
        private static java.util.Optional<Aura> auraFor(Gate gate) {
            return switch (gate) {
                case LIFE, PAIN, LIMIT -> java.util.Optional.of(new Aura(dust(0.6f, 0.2f, 0.2f), 10));
                case VIEW -> java.util.Optional.of(new Aura(dust(0.5f, 0.9f, 0.5f), 20));
                case WONDER -> java.util.Optional.of(new Aura(dust(0.6f, 0.8f, 0.9f), 30));
                case DEATH -> java.util.Optional.of(new Aura(dust(0.9f, 0.3f, 0.3f), 40));
                default -> java.util.Optional.empty();
            };
        }

        private static DustParticleOptions dust(float r, float g, float b) {
            return new DustParticleOptions(new Vector3f(r, g, b), 0.8f);
        }

        /** Polvo de color y cantidad del aura de una puerta. */
        private record Aura(DustParticleOptions options, int count) {
        }
    }
}
