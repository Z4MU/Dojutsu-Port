package com.zamu.dojutsu.dojutsu;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.EyeState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.eye.EyeRequirement;
import com.zamu.dojutsu.network.AwakenEternalPayload;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.platform.NetworkService;
import com.zamu.dojutsu.platform.PlatformServices;
import com.zamu.dojutsu.progression.XpApi;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

/**
 * Despertar de la <b>forma eterna</b> (EMS: Eternal Mangekyo Sharingan) {@code [S05]} (fleco F5): la accion manual
 * que vuelve <b>eterno</b> el ojo activo si cumple los requisitos, con los bonos del datapack. Reemplaza las
 * <b>herramientas quirurgicas</b> del datapack ({@code items/surgical_tools/implant_ems} → {@code eternal/awaken}
 * → {@code try_give}) por una accion del menu ocular, server-side y validada.
 *
 * <p><b>Requisitos (verificados contra el datapack):</b> el ojo activo debe ser <b>eternalizable</b>
 * ({@code EyeDefinition.eternalRequirements} no vacio: el Mangekyo), no estarlo ya, cumplir su <b>clan</b>
 * ({@code otsutsuki_uchiha}) y <b>uso</b> ({@code use_level >= 864000}, el {@code ems_req}), y tener
 * <b>Medical Ninjutsu >= 150</b> ({@code d.medical}, la puerta de las herramientas). <b>Bonos al despertar</b>
 * ({@code try_give}): {@code +60 max_chakra}, {@code +150 ninja_xp}, {@code +78000 susanoo_use}.
 *
 * <p>La decision {@link #evaluate} es <b>pura</b> (testeable sin Minecraft); {@link #tryAwaken} es el glue.
 */
public final class EternalAwakeningService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Medical Ninjutsu minimo para el implante ({@code d.medical matches 150..}). */
    static final int MEDICAL_REQUIREMENT = 150;

    private static final int MAX_CHAKRA_BONUS = 60;   // try_give: max_chakra += 60
    private static final int NINJA_XP_BONUS = 150;    // try_give: ninja_xp += 150
    private static final int SUSANOO_USE_BONUS = 78000; // try_give: susanoo_use += 78000

    /**
     * Resultado de intentar despertar el EMS: exito o el motivo del rechazo.
     *
     * <p>Es <b>publico desde F10</b> porque el menu ocular lo necesita. La verificacion en partida encontro dos
     * cosas a la vez: que el boton de despertar el EMS salia con <b>cualquier</b> ojo —incluidos el Rinnegan y el
     * Rinne Sharingan, que no tienen forma eterna— y que <b>no habia manera de saber que faltaba</b> para
     * conseguirlo. Las dos se arreglan mandando este resultado con la vista del menu: el boton solo aparece donde
     * procede, y cuando no se puede todavia dice por que.
     */
    public enum AwakenOutcome {
        AWAKENED,
        NO_ETERNAL_EYE,   // sin ojo activo eternalizable ("must have your Mangekyo active")
        ALREADY_ETERNAL,
        NEEDS_MEDICAL,    // d.medical < 150
        WRONG_CLAN,       // no otsutsuki_uchiha
        NEEDS_DEVELOPMENT // uso por debajo del umbral (ems_req)
    }

    private EternalAwakeningService() {
    }

    /**
     * Estado del EMS para el jugador, sin intentar nada: lo que el menu necesita para decidir si dibuja el boton
     * y con que texto (F10).
     */
    public static AwakenOutcome statusFor(PlayerNinjaData data) {
        EyeState state = data.eye();
        EyeDefinition activeDef = state.activeEye().flatMap(EyeRegistry::get).orElse(null);
        return evaluate(activeDef, state, data.clan().clan(),
                data.progression().skill(Skill.MEDICAL).level());
    }

    /**
     * Decision pura: que ocurre al intentar despertar el EMS. El orden de las puertas sigue al datapack
     * (herramientas → medicina → clan → ojo desarrollado).
     *
     * @param activeDef    definicion del ojo activo (o {@code null} si no hay)
     * @param state        estado ocular (para {@code eternal} y el uso de la familia)
     * @param clanId       clan del jugador
     * @param medicalLevel nivel de Medical Ninjutsu
     */
    static AwakenOutcome evaluate(EyeDefinition activeDef, EyeState state, Optional<String> clanId, int medicalLevel) {
        if (activeDef == null || !activeDef.isEternalizable()) {
            return AwakenOutcome.NO_ETERNAL_EYE;
        }
        if (state.eternal()) {
            return AwakenOutcome.ALREADY_ETERNAL;
        }
        if (medicalLevel < MEDICAL_REQUIREMENT) {
            return AwakenOutcome.NEEDS_MEDICAL;
        }
        if (!clanMet(activeDef, clanId)) {
            return AwakenOutcome.WRONG_CLAN;
        }
        if (!useMet(activeDef, state)) {
            return AwakenOutcome.NEEDS_DEVELOPMENT;
        }
        return AwakenOutcome.AWAKENED;
    }

    /** Cumple todos los requisitos de clan de {@code eternalRequirements} (compuesto {@code otsutsuki_uchiha}). */
    private static boolean clanMet(EyeDefinition def, Optional<String> clanId) {
        return def.eternalRequirements().stream()
                .filter(req -> req.type() == EyeRequirement.Type.CLAN)
                .allMatch(req -> clanId.map(id -> EvolutionService.clanMatches(id, req.value())).orElse(false));
    }

    /** Cumple los requisitos de uso de {@code eternalRequirements} (uso de la familia del ojo activo). */
    private static boolean useMet(EyeDefinition def, EyeState state) {
        int use = state.useLevel(def.family());
        return def.eternalRequirements().stream()
                .filter(req -> req.type() == EyeRequirement.Type.USE_LEVEL)
                .allMatch(req -> use >= req.min() && use <= req.max());
    }

    /** Intenta despertar el EMS del jugador (lo llama el handler de {@link AwakenEternalPayload}). */
    public static void tryAwaken(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        EyeState state = data.eye();
        EyeDefinition activeDef = state.activeEye().flatMap(EyeRegistry::get).orElse(null);
        int medical = data.progression().skill(Skill.MEDICAL).level();
        AwakenOutcome outcome = evaluate(activeDef, state, data.clan().clan(), medical);
        if (outcome != AwakenOutcome.AWAKENED) {
            player.sendSystemMessage(messageFor(outcome));
            return;
        }
        grant(player, data, state);
    }

    /** Aplica los bonos del EMS y marca el ojo eterno. */
    private static void grant(ServerPlayer player, PlayerNinjaData data, EyeState state) {
        data.setEye(state.withEternal(true));
        ChakraService.setMax(data, data.chakra().maxChakra() + MAX_CHAKRA_BONUS, false);
        XpApi.addNinjaXp(player, NINJA_XP_BONUS); // auto-nivela y avisa del rango
        data.setSusanoo(data.susanoo().plusUse(SUSANOO_USE_BONUS));
        player.sendSystemMessage(Component.translatable("eternal.dojutsu.awakened").withStyle(ChatFormatting.GOLD));
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
        NinjaSync.sendDelta(player); // el max_chakra nuevo al HUD
        LOGGER.info("[{}] {} desperto el Eternal Mangekyo Sharingan.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName());
    }

    private static Component messageFor(AwakenOutcome outcome) {
        String key = switch (outcome) {
            case NO_ETERNAL_EYE -> "eternal.dojutsu.no_eye";
            case ALREADY_ETERNAL -> "eternal.dojutsu.already";
            case NEEDS_MEDICAL -> "eternal.dojutsu.needs_medical";
            case WRONG_CLAN -> "eternal.dojutsu.wrong_clan";
            case NEEDS_DEVELOPMENT -> "eternal.dojutsu.needs_development";
            default -> "eternal.dojutsu.no_eye";
        };
        return Component.translatable(key).withStyle(ChatFormatting.DARK_RED);
    }

    /** Registra el payload C2S de despertar el EMS. Llamar en la inicializacion del mod. */
    public static void register() {
        PlatformServices.NETWORK.register(NetworkService.Side.C2S, AwakenEternalPayload.TYPE,
                AwakenEternalPayload.CODEC, EternalAwakeningService::onAwaken);
    }

    private static void onAwaken(AwakenEternalPayload payload, NetworkService.Context context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                tryAwaken(player);
            }
        });
    }
}
