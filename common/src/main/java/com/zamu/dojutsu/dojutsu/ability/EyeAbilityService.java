package com.zamu.dojutsu.dojutsu.ability;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.dojutsu.EyeStrainService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.network.UseEyeAbilityPayload;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Pipeline de casteo de habilidades oculares (DJT-118): la secuencia canonica de {@code dojutsu/abilities} —
 * validar ojo activo + seleccion + chakra → consumir chakra (todo-o-nada) y fatiga → ejecutar el efecto →
 * sincronizar. Registra las habilidades piloto y el payload C2S de uso. Mismo espiritu que
 * {@code JutsuCastPipeline} (el unico camino de cast de un jutsu), aplicado a los ojos.
 *
 * <p><b>El cliente no decide gameplay</b> ({@code [ADD/07]}): la tecla solo envia la intencion; aqui se lee la
 * {@code selectedAbility} del estado real y se valida todo. La fatiga se exime con el ojo <b>eterno</b> (EMS),
 * fiel a {@code sasuke/cost} ({@code unless wearing_eternal}).
 */
public final class EyeAbilityService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Intervalo minimo entre usos por jugador (anti-spam de paquetes; el coste de chakra ya autolimita). */
    static final long MIN_INTERVAL_MS = 200L;

    private static final Map<UUID, Long> LAST_USE_MS = new ConcurrentHashMap<>();

    /** Clave de cooldown: el datapack lo lleva <b>por jugador y por habilidad</b> ({@code d.<ability>_cd}). */
    private record CooldownKey(UUID player, String abilityId) {
    }

    /** Tick de mundo en el que cada (jugador, habilidad) vuelve a estar disponible. */
    private static final Map<CooldownKey, Long> READY_AT_TICK = new ConcurrentHashMap<>();

    private EyeAbilityService() {
    }

    /** Registra las habilidades implementadas y el payload C2S de uso. Llamar en la inicializacion del mod. */
    public static void register() {
        EyeAbilityRegistry.register("amaterasu", new AmaterasuAbility());     // Mangekyo (DJT-118, plantilla)
        EyeAbilityRegistry.register("vacuum_palm", new VacuumPalmAbility());  // Byakugan: empuje dirigido (DJT-119)
        EyeAbilityRegistry.register("shinra_tensei", new ShinraTenseiAbility()); // Rinnegan: empuje AoE (DJT-119)
        EyeAbilityRegistry.register("bansho_tenin", new BanshoTeninAbility());   // Rinnegan: atraccion AoE (DJT-119)
        EyeAbilityRegistry.register("blood_eruption", new BloodEruptionAbility());       // Ketsuryugan (DJT-119 lote 2)
        EyeAbilityRegistry.register("iron_blood_fist", new IronBloodFistAbility());      // Ketsuryugan: auto-buff
        EyeAbilityRegistry.register("rising_blood_spear", new RisingBloodSpearAbility()); // Ketsuryugan: HARM AoE
        EyeAbilityRegistry.register("tsukuyomi", new TsukuyomiAbility());        // Mangekyo: genjutsu (DJT-119 lote 3)
        EyeAbilityRegistry.register("amenotejikara", new AmenotejikaraAbility()); // Rinnegan: teleport/swap
        EyeAbilityRegistry.register("sixty_four_palms", new SixtyFourPalmsAbility()); // Byakugan: golpe AoE corto
        EyeAbilityRegistry.register("exploding_human", new ExplodingHumanAbility());  // Ketsuryugan (DJT-119 lote 4)
        EyeAbilityRegistry.register("hundred_twenty_eight_palms", new HundredTwentyEightPalmsAbility()); // Byakugan
        EyeAbilityRegistry.register("chibaku_tensei", new ChibakuTenseiAbility());    // Rinnegan: gravedad vertical
        EyeAbilityRegistry.register("asura_path", new AsuraPathAbility());        // Rinnegan: misil/proyectil (DJT-119 lote 5)
        EyeAbilityRegistry.register("preta_path", new PretaPathAbility());        // Rinnegan: absorción (defensivo)
        EyeAbilityRegistry.register("truth_seeking_orbs", new TruthSeekingOrbsAbility()); // Tenseigan: HARM en el punto de mira
        EyeAbilityRegistry.register("genjutsu_stun", new GenjutsuStunAbility());  // Sharingan: stun duro (DJT-119 lote 6)
        EyeAbilityRegistry.register("copy_jutsu", new CopyJutsuAbility());        // Sharingan: copia el jutsu visto
        EyeAbilityRegistry.register("kinrin_tensei_baku", new KinrinTenseiBakuAbility()); // Tenseigan: canalizada
        EyeAbilityRegistry.register("susanoo", new SusanooAbility()); // Mangekyo: modo activable (DJT-120)
        EyeAbilityRegistry.register("tenseigan_chakra_mode", new TenseiganChakraModeAbility()); // Tenseigan: modo (fleco F5)
        EyeAbilityRegistry.register("kamui", new KamuiAbility()); // Mangekyo Obito: marca-y-vuelve (roster lote 2)
        EyeAbilityRegistry.register("kamui_others", new KamuiOthersAbility()); // Obito: manda a otros al punto marcado
        EyeAbilityRegistry.register("tengai_shinsei", new TengaiShinseiAbility()); // Mangekyo Madara: meteorito (roster lote 3)
        EyeAbilityRegistry.register("hinokagutsuchi", new HinokagutsuchiAbility()); // Mangekyo Indra: llama negra (lote 4)
        EyeAbilityRegistry.register("kotoamatsukami", new KotoamatsukamiAbility()); // Mangekyo Shisui: genjutsu supremo (lote 5)
        EyeAbilityRegistry.register("chakra_blades", new ChakraBladesAbility()); // Mangekyo Shin: cuchillas (lote 6)
        EyeAbilityRegistry.register("phase_shift", new PhaseShiftAbility());     // Mangekyo Shin: salto guiado
        EyeAbilityRegistry.register("flame_control", new FlameControlAbility()); // Mangekyo Sasuke: apaga la llama (lote 7)
        EyeAbilityRegistry.register("beams_of_light", new BeamsOfLightAbility()); // Mangekyo Shisui: rayo dirigido (2a suya)
        // Rinne Sharingan (F10/DJT-226): la subpagina de cuatro destinos del datapack se disuelve en cuatro
        // entradas del menu. Una sola clase parametrizada; el destino ES la entrada.
        for (AmenominakaAbility.Destination destination : AmenominakaAbility.Destination.values()) {
            EyeAbilityRegistry.register(destination.abilityId(), new AmenominakaAbility(destination));
        }
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, UseEyeAbilityPayload.TYPE,
                UseEyeAbilityPayload.CODEC, EyeAbilityService::onUse);
    }

    /** Olvida el estado anti-spam y los cooldowns de un jugador (al desconectar). */
    public static void forget(UUID playerId) {
        LAST_USE_MS.remove(playerId);
        READY_AT_TICK.keySet().removeIf(key -> key.player().equals(playerId));
    }

    /** Decision pura del cooldown: sigue en enfriamiento si el tick actual no ha alcanzado el de disponibilidad. */
    static boolean isOnCooldown(long readyAtTick, long nowTick) {
        return nowTick < readyAtTick;
    }

    static boolean isRateLimited(UUID playerId, long nowMs) {
        Long last = LAST_USE_MS.get(playerId);
        if (last != null && nowMs - last < MIN_INTERVAL_MS) {
            return true;
        }
        LAST_USE_MS.put(playerId, nowMs);
        return false;
    }

    /**
     * Decision pura: que habilidad castear. Requiere un ojo <b>encendido</b> (F11/DJT-230: tener el ojo no
     * basta, hay que tenerlo activo) y una habilidad seleccionada que este <b>registrada</b> (una habilidad
     * data-only del menu sin clase aun no se puede lanzar). Vacio = no procede.
     */
    static Optional<EyeAbility> resolveCast(PlayerNinjaData data, Function<String, Optional<EyeAbility>> lookup) {
        EyeState eye = data.eye();
        if (eye.poweredEye().isEmpty()) {
            return Optional.empty();
        }
        return eye.selectedAbility().flatMap(lookup);
    }

    /** Casta la habilidad seleccionada del jugador: valida y consume chakra, aplica fatiga, ejecuta y sincroniza. */
    public static void cast(ServerPlayer player) {
        if (com.zamu.dojutsu.bijuu.jinchuriki.FormService.suppressesEyes(player.getUUID())) {
            // dojutsu/main ejecuta cada ojo "unless is_in_jinchuriki_form": el ojo sigue equipado pero no
            // responde mientras dure la forma. SUPRIMIDO, no apagado — la forma del Diez Colas exige el
            // Rinnegan ACTIVO, asi que apagarlo de verdad la apagaria a ella misma (F7/DJT-166).
            player.displayClientMessage(net.minecraft.network.chat.Component
                    .translatable("jinchuriki.dojutsu.eyes_suppressed")
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
            return;
        }
        PlayerNinjaData data = NinjaData.get(player);
        Optional<EyeAbility> resolved = resolveCast(data, EyeAbilityRegistry::get);
        if (resolved.isEmpty()) {
            return; // sin ojo activo, sin seleccion, o habilidad no implementada
        }
        EyeAbility ability = resolved.get();
        String abilityId = data.eye().selectedAbility().orElseThrow(); // resolveCast ya garantizo que existe
        CooldownKey key = new CooldownKey(player.getUUID(), abilityId);
        long now = player.serverLevel().getGameTime();
        if (isOnCooldown(READY_AT_TICK.getOrDefault(key, Long.MIN_VALUE), now)) {
            return; // aun en enfriamiento: no castea (el datapack tampoco)
        }
        EyeAbilityContext context = new EyeAbilityContext(player);
        if (!ability.canApply(context)) {
            // "Nothing nearby": sin objetivo valido, se aborta ANTES de cobrar (fiel a no_cast_nby del datapack)
            player.displayClientMessage(
                    Component.translatable("ability.dojutsu.no_target").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!ChakraService.consume(data, ability.chakraCost())) {
            return; // chakra insuficiente: no castea (el datapack tampoco)
        }
        int strain = data.eye().eternal() ? 0 : ability.strainCost(); // EMS exime de fatiga (sasuke/cost)
        EyeStrainService.addStrain(data, strain);
        ability.execute(context);
        EyeAbilitySfx.play(player, abilityId); // el playsound del coste de cada tecnica (F10/DJT-227)
        if (ability.cooldownTicks() > 0) {
            READY_AT_TICK.put(key, now + ability.cooldownTicks());
        }
        NinjaSync.sendDelta(player); // empuja el chakra consumido al HUD
    }

    private static void onUse(UseEyeAbilityPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (isRateLimited(player.getUUID(), System.currentTimeMillis())) {
                LOGGER.warn("[{}] Uso de habilidad ocular ignorado por rate-limit ({}).",
                        DojutsuMod.MOD_NAME, player.getGameProfile().getName());
                return;
            }
            cast(player);
        });
    }
}
