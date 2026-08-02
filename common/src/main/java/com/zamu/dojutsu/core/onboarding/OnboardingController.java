package com.zamu.dojutsu.core.onboarding;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.clans.ClanDefinition;
import com.zamu.dojutsu.clans.ClanRegistry;
import com.zamu.dojutsu.clans.ClanService;
import com.zamu.dojutsu.clans.ClanTeamService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.OnboardingState;
import com.zamu.dojutsu.data.state.OnboardingState.Phase;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.natures.KekkeiGenkaiService;
import com.zamu.dojutsu.natures.NatureRoll;
import com.zamu.dojutsu.natures.NatureRollTable;
import com.zamu.dojutsu.natures.NatureService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.network.OnboardingSync;
import com.zamu.dojutsu.progression.LevelingService;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Maquina de estados de la creacion del ninja ({@code [S01]}: {@code main/join} + {@code join_main} +
 * {@code join_finish}): {@code NOT_STARTED -> SELECTING_CLAN -> SELECTING_AFFINITY -> COMPLETED}, con el paso
 * "finish" (rolls + cierre) como <b>transicion</b> a {@code COMPLETED}, no como fase persistida (decision de
 * fase: se mantienen los nombres del enum de F1).
 *
 * <p><b>El avance es SOLO del servidor</b>: el cliente (DJT-060) envia intenciones por
 * {@code OnboardingChoicePayload} y este controlador las valida contra la <b>fase actual</b> (un payload de
 * fase incorrecta se rechaza sin efecto) y contra los registros (clan/afinidad <b>por nombre</b>). La fase es
 * observable ({@code setOnboarding} marca sync): el cliente sabe que pantalla mostrar sin canal nuevo.
 *
 * <p><b>Reemplaza del datapack:</b> {@code assign_plys} y el limite de 10 jugadores (ya resuelto con UUID en
 * F1), los titulos + hotbar secuestrado (seran una Screen real en DJT-060) y el "menu item" de
 * {@code join_finish} (el menu es la tecla M desde F2: no hay item que entregar).
 *
 * <p><b>Caso Lee</b> ({@code skipsAffinity} de la {@link ClanDefinition}): salta la seleccion de afinidad
 * <b>y</b> el roll de KG (ambas ramas del datapack condicionan a {@code is_lee}).
 *
 * <p><b>Reanudacion:</b> la fase persiste; {@link #refresh} (en cada login) arranca a los nuevos
 * ({@code NOT_STARTED -> SELECTING_CLAN}) y <b>re-embarca</b> a quien perdio el clan (reset de clan con
 * {@code COMPLETED} -> {@code SELECTING_CLAN}; la pregunta abierta de DJT-058, decidida aqui). Una desconexion
 * a mitad reanuda en la fase correcta sin repetir nada: los bonus de clan quedan tras {@code initialized}.
 */
public final class OnboardingController {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Eleccion especial: el servidor elige al azar ({@code join_random} para clan; {@code random_aff} para afinidad). */
    public static final String RANDOM_CHOICE = "random";

    private OnboardingController() {
    }

    // --- Maquina de estados pura (testeable sin Minecraft) ---

    /**
     * Reconcilia la fase con el estado real. Nuevo ({@code NOT_STARTED}) -> {@code SELECTING_CLAN}; completado
     * pero <b>sin clan</b> (reset de clan) -> {@code SELECTING_CLAN} de nuevo. Las fases intermedias se
     * conservan (reanudacion tras relogin).
     *
     * @return {@code true} si la fase cambio
     */
    public static boolean refresh(PlayerNinjaData data) {
        Phase phase = data.onboarding().phase();
        boolean shouldSelectClan = phase == Phase.NOT_STARTED
                || (phase == Phase.COMPLETED && !data.clan().hasClan());
        if (!shouldSelectClan || phase == Phase.SELECTING_CLAN) {
            return false;
        }
        data.setOnboarding(data.onboarding().withPhase(Phase.SELECTING_CLAN));
        return true;
    }

    /**
     * Eleccion de clan (fase {@code SELECTING_CLAN}): valida la fase y el id, une por
     * {@code ClanService.join} (bonus una sola vez) y avanza a {@code SELECTING_AFFINITY} — o directamente al
     * finish si el clan {@code skipsAffinity} (Lee). {@link #RANDOM_CHOICE} tira uniforme entre los clanes
     * cargados ({@code clans/join_random}: uniforme 1..14, <b>Lee incluido</b> — puede saltar la afinidad).
     *
     * @return {@code true} si se aplico; {@code false} si la fase o el clan son invalidos
     */
    public static boolean chooseClan(PlayerNinjaData data, UUID player, String clanId, RandomGenerator random,
            DomainEventBus bus) {
        if (data.onboarding().phase() != Phase.SELECTING_CLAN) {
            return false; // payload de fase incorrecta: sin efecto
        }
        ClanDefinition def = RANDOM_CHOICE.equals(clanId)
                ? rollClan(random)
                : ClanRegistry.get(clanId).orElse(null);
        if (def == null || !ClanService.join(data, player, def, random, bus)) {
            return false;
        }
        if (def.skipsAffinity()) {
            finish(data, player, random, bus); // Lee: sin afinidad y sin roll de KG
        } else {
            data.setOnboarding(data.onboarding().withPhase(Phase.SELECTING_AFFINITY));
        }
        return true;
    }

    /**
     * Eleccion de afinidad (fase {@code SELECTING_AFFINITY}): una de las 5 basicas o {@link #RANDOM_CHOICE}
     * ({@code random_aff}: uniforme entre las 5). Fija la afinidad a <b>50</b> ({@code set}, no {@code add}:
     * un Uchiha que elige Fuego queda en 50, no 60), otorga la naturaleza y ejecuta el finish.
     *
     * @return {@code true} si se aplico; {@code false} si la fase o la eleccion son invalidas
     */
    public static boolean chooseAffinity(PlayerNinjaData data, UUID player, String natureId,
            RandomGenerator random, DomainEventBus bus) {
        if (data.onboarding().phase() != Phase.SELECTING_AFFINITY) {
            return false; // payload de fase incorrecta: sin efecto
        }
        NatureType nature = RANDOM_CHOICE.equals(natureId)
                ? NatureRoll.rollBasicAffinity(random)
                : parseBasic(natureId);
        if (nature == null) {
            return false; // eleccion invalida (solo las 5 basicas del datapack, por nombre)
        }
        NatureService.setAffinityLevel(data, nature, 50); // nature_select_main: set aff 50, luego give
        NatureService.grant(data, player, nature, bus);
        finish(data, player, random, bus);
        return true;
    }

    /**
     * Paso FINISH ({@code join_finish} + {@code get_random_kg}), la transicion a {@code COMPLETED}: si el clan
     * no salta el roll, tira el KG (o Yang, o nada), la 2a afinidad (solo sin premio) y el genjutsu (siempre);
     * el premio da +20 ninja_xp. El "menu item" del datapack no aplica (el menu es la tecla M desde F2).
     */
    private static void finish(PlayerNinjaData data, UUID player, RandomGenerator random, DomainEventBus bus) {
        boolean skipRolls = data.clan().clan().flatMap(ClanRegistry::get)
                .map(ClanDefinition::skipsAffinity).orElse(false);
        if (!skipRolls) {
            boolean senju = data.clan().isIn("senju"); // habilita el tramo Madera 30..40 del roll
            NatureRollTable.Outcome outcome = NatureRoll.rollKekkeiGenkai(random, senju);
            outcome.kekkeiGenkai().ifPresent(kg -> KekkeiGenkaiService.grant(data, player, kg, bus));
            outcome.nature().ifPresent(nature -> NatureService.grant(data, player, nature, bus)); // Yang (S03-R1)
            if (!outcome.claimed() && NatureRoll.rollSecondAffinity(random)) {
                NatureType extra = NatureRoll.rollBasicAffinity(random); // random_aff: set 50 + give
                NatureService.setAffinityLevel(data, extra, 50);
                NatureService.grant(data, player, extra, bus);
            }
            if (NatureRoll.rollGenjutsu(random)) {
                // gen_ulk_rng acierta (1 de 7): genjutsu +10 niveles, +10 ninja_xp y la naturaleza Yin.
                LevelingService.addSkillLevels(data, player, Skill.GENJUTSU, 10, bus);
                LevelingService.addNinjaXp(data, player, 10, bus);
                NatureService.grant(data, player, NatureType.YIN, bus);
            }
            if (outcome.claimed()) {
                LevelingService.addNinjaXp(data, player, 20, bus); // if rkg_gkg -> ninja_xp += 20
            }
        }
        data.setOnboarding(data.onboarding().withPhase(Phase.COMPLETED));
    }

    /**
     * Clan uniforme entre los cargados ({@code join_random}; con los 14 del datapack la distribucion es
     * identica al loot {@code clan_rng} 1..14). Ids <b>ordenados</b>: el indice es determinista con semilla.
     */
    private static ClanDefinition rollClan(RandomGenerator random) {
        var ids = ClanRegistry.all().keySet().stream().sorted().toList();
        return ids.isEmpty() ? null : ClanRegistry.get(ids.get(random.nextInt(ids.size()))).orElse(null);
    }

    /** Resuelve una eleccion de afinidad <b>por nombre</b> a una de las 5 basicas; {@code null} si no lo es. */
    private static NatureType parseBasic(String natureId) {
        for (NatureType nature : NatureType.basics()) {
            if (nature.name().toLowerCase(Locale.ROOT).equals(natureId)) {
                return nature;
            }
        }
        return null;
    }

    // --- Glue con Minecraft ---

    /** Reconcilia la fase al entrar el jugador (arranque de nuevos, reanudacion, re-embarque tras reset). */
    public static void refresh(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        if (refresh(data)) {
            NinjaSync.sendDelta(player);
            LOGGER.info("[{}] {} entra en la creacion del ninja (SELECTING_CLAN).",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName());
        }
        if (data.onboarding().phase() == Phase.SELECTING_CLAN) {
            // Tambien cuando la fase NO cambio (relogin a mitad): la Screen necesita la lista de clanes.
            OnboardingSync.sendOptions(player);
        }
    }

    /** Eleccion de clan del jugador (la llama el handler del payload tras validar el emisor). */
    public static boolean chooseClan(ServerPlayer player, String clanId) {
        PlayerNinjaData data = NinjaData.get(player);
        if (!chooseClan(data, player.getUUID(), clanId, new Random(), DomainEvents.bus())) {
            return false;
        }
        ClanTeamService.sync(player); // nametag del clan
        NinjaSync.sendDelta(player);
        // El clan REAL sale del estado, no del payload: con "random" el elegido lo decidio el servidor.
        String joined = data.clan().clan().orElse(clanId);
        player.sendSystemMessage(Component.translatable("onboarding.dojutsu.clan_joined",
                Component.translatable("clan.dojutsu." + joined)));
        if (data.onboarding().completed()) {
            player.sendSystemMessage(Component.translatable("onboarding.dojutsu.completed")); // caso Lee
            LOGGER.info("[{}] {} completo la creacion del ninja (clan sin afinidad).",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName());
        }
        return true;
    }

    /** Eleccion de afinidad del jugador (la llama el handler del payload tras validar el emisor). */
    public static boolean chooseAffinity(ServerPlayer player, String natureId) {
        if (!chooseAffinity(NinjaData.get(player), player.getUUID(), natureId, new Random(), DomainEvents.bus())) {
            return false;
        }
        NinjaSync.sendDelta(player);
        player.sendSystemMessage(Component.translatable("onboarding.dojutsu.completed"));
        LOGGER.info("[{}] {} completo la creacion del ninja.",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName());
        return true;
    }
}
