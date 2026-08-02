package com.zamu.dojutsu.clans;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.chakra.RegenService;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ChakraState;
import com.zamu.dojutsu.data.state.ClanMembership;
import com.zamu.dojutsu.jutsu.JutsuGrantService;
import com.zamu.dojutsu.data.state.JutsuKnowledge;
import com.zamu.dojutsu.data.state.NatureProfile;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.DomainEventBus;
import com.zamu.dojutsu.domain.nature.KekkeiGenkai;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.jutsu.JutsuLearningService;
import com.zamu.dojutsu.natures.KekkeiGenkaiService;
import com.zamu.dojutsu.natures.LearnerService;
import com.zamu.dojutsu.natures.NatureService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.LevelingService;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Union a un clan ({@code clans/join_<clan>} del datapack, {@code [S04]}): aplica los bonus de la
 * {@link ClanDefinition} <b>exactamente una vez</b> y deja la pertenencia en {@code ClanMembership}.
 *
 * <p>Orden del join, fiel al datapack: naturalezas (cada una con su ninja_xp y su learner via evento) ->
 * naturaleza aleatoria si la hay (Sarutobi) -> KG (implican componentes) -> bonus de afinidad -> bonus de
 * habilidad (emiten {@code SkillLevelChangedEvent}: los desbloqueos por hito reaccionan tambien a los bonus) ->
 * chakra maximo del clan <b>relleno al maximo</b> -> ninja_xp del clan -> pertenencia.
 *
 * <p><b>Una sola vez:</b> el join se rechaza si ya hay clan ({@code if d.clan matches 0} del datapack) o si
 * {@code initialized} esta marcado. Re-join, relogin a mitad o repetir el comando <b>no re-aplican</b> nada.
 *
 * <p><b>Correccion S04-R3 (Namikaze):</b> resuelta <b>en los datos</b> — su JSON declara
 * {@code affinityBonus: lightning 25} determinista en vez del roll sobre el temporal {@code d.tmp} no
 * garantizado. El 50% de Wind de Sarutobi (azar intencionado) se conserva via
 * {@link ClanDefinition.RandomNature}, con el azar <b>inyectado</b> (testeable con semilla).
 *
 * <p>El {@code potentialDojutsu} <b>no se copia al estado</b>: el despertar (F5) lo <b>deriva</b> de la
 * pertenencia ({@code ClanRegistry.get(clan).potentialDojutsu()}) — derivado, nunca cacheado, como el rango.
 *
 * <p><b>Hooks pendientes:</b> {@code startingJutsu} (F4/DJT-061-075) y {@code startingItems} (F8) se registran
 * en el log pero no se entregan; el team/nametag es DJT-062; la velocidad de Namikaze espera al puerto
 * {@code AttributeService}. El reset ({@link #reset}) corrige S04-R2 con defaults atomicos (DJT-058).
 */
public final class ClanService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private ClanService() {
    }

    /**
     * Registra en {@code RegenService} el proveedor del factor de regen <b>declarado por el clan</b> en sus
     * datos ({@code regenFactor}: Otsutsuki/Uzumaki, {@code [S02]}). Llamar una vez en el arranque. Es el
     * enchufe que F2 dejo esperando; va como proveedor para que {@code chakra} no dependa de {@code clans}.
     */
    public static void registerRegenFactors() {
        RegenService.registerFactorProvider(data -> data.clan().clan()
                .flatMap(ClanRegistry::get)
                .flatMap(ClanDefinition::regenFactor)
                .map(Set::of)
                .orElse(Set.of()));
    }

    // --- Servicio puro (sobre el estado) ---

    /**
     * Une al jugador al clan aplicando sus bonus una sola vez.
     *
     * @param random origen del azar para {@link ClanDefinition.RandomNature} (inyectado: testeable con semilla)
     * @return {@code true} si se unio; {@code false} si ya tenia clan o sus bonus ya se aplicaron
     */
    public static boolean join(PlayerNinjaData data, UUID player, ClanDefinition def, RandomGenerator random,
            DomainEventBus bus) {
        if (data.clan().hasClan() || data.clan().initialized()) {
            return false; // "Already in a Clan." — los bonus se aplican exactamente una vez
        }
        for (NatureType nature : def.natures()) {
            NatureService.grant(data, player, nature, bus);
        }
        def.randomNature().ifPresent(roll -> {
            if (random.nextInt(roll.oneIn()) == 0) {
                NatureService.grant(data, player, roll.nature(), bus);
            }
        });
        for (KekkeiGenkai kg : def.kekkeiGenkai()) {
            KekkeiGenkaiService.grant(data, player, kg, bus);
        }
        for (Map.Entry<NatureType, Integer> bonus : def.affinityBonus().entrySet()) {
            NatureService.addAffinityLevels(data, bonus.getKey(), bonus.getValue());
        }
        for (Map.Entry<Skill, Integer> bonus : def.skillBonus().entrySet()) {
            LevelingService.addSkillLevels(data, player, bonus.getKey(), bonus.getValue(), bus);
        }
        ChakraService.setMax(data, def.maxChakra(), true); // d.max_chakra = N; d.chakra = d.max_chakra
        LevelingService.addNinjaXp(data, player, def.ninjaXp(), bus);
        data.setClan(data.clan().withClan(def.id()));
        return true;
    }

    /**
     * Reset de clan ({@code clans/reset_clan} del datapack) — <b>corrige S04-R2 con defaults atomicos</b>: en
     * vez de limpiar campo a campo (y olvidar alguno, que es justo lo que le paso al datapack), reemplaza los
     * <b>cuatro sub-estados de gameplay</b> por sus defaults: pertenencia, perfil elemental (naturalezas, KG,
     * afinidades, ADN, prestamos), progresion (habilidades, puntos, ninja_xp, unlocks) y chakra (max de vuelta
     * a 100). El <b>onboarding se conserva</b> (el ninja sigue creado; el reset total es {@code reset_ninja}).
     *
     * <p><b>Lo que el datapack olvidaba</b> (ademas de naturalezas/KG/jutsus de S04-R2): {@code d.kenjutsu},
     * {@code d.cc}, {@code d.max_chakra} y {@code d.skill_points}. Aqui el atomico lo cubre todo por
     * construccion; la ficha manda ("preferir defaults atomicos") sobre la fidelidad al reset parcial.
     *
     * <p>Idempotente y valido <b>sin clan</b> (deja igualmente el perfil limpio, como el original). Los jutsus
     * aprendidos son TODO F4 (no existen); el team/nametag lo quita DJT-062; los <b>prestamos de bijuu</b> (F7)
     * se borran con el perfil — cuando exista el sellado, su removal debera reconciliarse con esto.
     */
    public static void reset(PlayerNinjaData data) {
        data.setClan(ClanMembership.DEFAULT);
        data.setNatures(NatureProfile.DEFAULT);
        data.setProgression(ProgressionState.DEFAULT);
        data.setChakra(ChakraState.DEFAULT);
        data.setJutsu(JutsuKnowledge.DEFAULT); // 5o sub-estado de gameplay (F4/DJT-075) — el reset sigue atomico
    }

    // --- Glue con Minecraft ---

    /** Reset de clan del jugador: defaults atomicos + retirada de learners huerfanos + team fuera + sync. */
    public static void reset(ServerPlayer player) {
        reset(NinjaData.get(player));
        LearnerService.removeStaleLearners(player); // sin elementos -> sin learners ([S03-R2])
        JutsuLearningService.removeStaleJutsuItems(player); // sin conocimiento -> sin items de jutsu (DJT-075)
        ClanTeamService.sync(player); // sin clan -> fuera del team (el nametag pierde el sufijo, DJT-062)
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] Clan de {} reseteado (defaults atomicos).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName());
    }

    /**
     * Une al jugador al clan (por id del registro) y sincroniza. Devuelve {@code false} si el clan no existe,
     * si ya tiene clan o si sus bonus ya se aplicaron.
     */
    public static boolean join(ServerPlayer player, String clanId) {
        ClanDefinition def = ClanRegistry.get(clanId).orElse(null);
        if (def == null) {
            LOGGER.error("[{}] Clan sin definicion cargada: {} (no hay join).", DojutsuMod.MOD_NAME, clanId);
            return false;
        }
        if (!join(NinjaData.get(player), player.getUUID(), def, new Random(), DomainEvents.bus())) {
            return false;
        }
        ClanTeamService.sync(player); // el nametag toma el color y el sufijo del clan (DJT-062)
        NinjaSync.sendDelta(player); // chakra (max nuevo) y progresion son observables
        // Jutsus iniciales del clan (F10): el Aburame entra con el Orbe de Insectos, como en join_aburame. Los
        // ids cuyo jutsu aun no esta portado (Kaguya, Nara) se saltan con aviso y no rompen el join.
        JutsuGrantService.grantAll(player, def.startingJutsu());
        if (!def.startingItems().isEmpty()) {
            // TODO F8+: items iniciales del clan (pildoras Akimichi, sable Hatake, chakra blades Sarutobi).
            LOGGER.info("[{}] {} tiene items del clan {} pendientes de entrega: {}.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), clanId, def.startingItems());
        }
        LOGGER.info("[{}] {} se unio al clan {} (maxChakra={}, +{} ninja_xp).",
                DojutsuMod.MOD_NAME, player.getGameProfile().getName(), clanId, def.maxChakra(), def.ninjaXp());
        return true;
    }
}
