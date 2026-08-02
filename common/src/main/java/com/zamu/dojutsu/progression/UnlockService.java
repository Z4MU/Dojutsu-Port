package com.zamu.dojutsu.progression;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.core.DomainEvents;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ProgressionState;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.domain.event.NinjaXpChangedEvent;
import com.zamu.dojutsu.jutsu.JutsuGrantService;
import com.zamu.dojutsu.domain.event.SkillLevelChangedEvent;
import dev.architectury.utils.GameInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Desbloqueos por hito ({@code unlocks} del datapack, {@code [S20]}), <b>reactivos e idempotentes</b>. Se
 * suscribe a {@link SkillLevelChangedEvent}: al subir una habilidad, otorga los desbloqueos de
 * {@link UnlockRegistry} cuyo nivel se cruza. Re-otorgar no duplica ({@code [S20-R4]}).
 *
 * <p>Sustituye el polling {@code unlocks} por tick (O(hitos)/tick) por una consulta O(hitos) solo cuando el
 * nivel cambia. En F2 el desbloqueo registra el id en {@code ProgressionState.unlocked}; la entrega del
 * contenido real (item de jutsu) llega en F4.
 */
public final class UnlockService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private UnlockService() {
    }

    /**
     * Pseudo-habilidad para los hitos que dependen del <b>ninja_xp global</b> y no de una habilidad
     * (F10/DJT-221). El unico del datapack es la Tecnica de Sustitucion, que se otorga con
     * {@code d.ninja_xp >= 1000} — o sea, al llegar a Genin. Es el primer jutsu que recibe un ninja y por eso
     * merece su disparador, en vez de quedarse fuera del recetario.
     */
    public static final String NINJA_XP_SKILL_ID = "ninja_xp";

    /** Suscribe el otorgamiento reactivo de desbloqueos al bus de dominio. Llamar una vez en el arranque. */
    public static void register() {
        DomainEvents.bus().subscribe(SkillLevelChangedEvent.class, UnlockService::onSkillLevelChanged);
        DomainEvents.bus().subscribe(NinjaXpChangedEvent.class, UnlockService::onNinjaXpChanged);
    }

    /**
     * Otorga (idempotente) los desbloqueos que se cruzan al subir {@code skillId} de {@code oldLevel} a
     * {@code newLevel}. Puro sobre el estado (testeable): devuelve los ids <b>nuevos</b> otorgados.
     */
    public static List<String> applyUnlocks(PlayerNinjaData data, String skillId, int oldLevel, int newLevel) {
        return grant(data, UnlockRegistry.unlocksCrossing(skillId, oldLevel, newLevel));
    }

    /**
     * Barrido de <b>puesta al dia</b> (F10/DJT-221): otorga todo hito ya alcanzado que aun no se tenga. Se
     * ejecuta al entrar el jugador.
     *
     * <p>Existe porque el modelo reactivo tiene un punto ciego que el datapack no tenia: los cuatro hitos con
     * co-requisito de chakra maximo ({@code creation_rebirth}, su version SOAH, el kunai del Dios del Trueno y
     * el acceso a Senjutsu) se pueden cumplir en <b>dos momentos distintos</b>, y {@link SkillLevelChangedEvent}
     * solo ve uno de ellos: si el chakra llega despues de la habilidad, el instante del cruce ya paso. El
     * datapack lo resolvia revisando los hitos <b>cada tick</b>; aqui basta una pasada por sesion.
     */
    public static List<String> applyReachedUnlocks(PlayerNinjaData data) {
        List<UnlockDefinition> reached = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            String skillId = skill.name().toLowerCase(Locale.ROOT);
            reached.addAll(UnlockRegistry.unlocksReached(skillId, data.progression().skill(skill).level()));
        }
        reached.addAll(UnlockRegistry.unlocksReached(NINJA_XP_SKILL_ID, data.progression().ninjaXp()));
        return grant(data, reached);
    }

    /**
     * Otorga los hitos candidatos que cumplan <b>todos</b> sus requisitos y aun no se tengan. Puro sobre el
     * estado (testeable): devuelve los ids <b>nuevos</b>.
     */
    private static List<String> grant(PlayerNinjaData data, List<UnlockDefinition> candidates) {
        List<String> granted = new ArrayList<>();
        for (UnlockDefinition def : candidates) {
            ProgressionState progression = data.progression();
            if (progression.unlocked().contains(def.unlockId())) {
                continue; // idempotente por diseno (S20-R4)
            }
            if (data.chakra().maxChakra() < def.requiredMaxChakra()) {
                continue; // el co-requisito de chakra aun no se cumple: se reintenta en el barrido al entrar
            }
            data.setProgression(progression.withUnlock(def.unlockId()));
            granted.add(def.unlockId());
        }
        return granted;
    }

    /** ninja_xp que otorga un hito ({@code main/internal/unlocks}); 0 si el id no existe. */
    static int ninjaXpOf(String unlockId) {
        for (UnlockDefinition def : UnlockRegistry.all()) {
            if (def.unlockId().equals(unlockId)) {
                return def.ninjaXp();
            }
        }
        return 0;
    }

    private static void onSkillLevelChanged(SkillLevelChangedEvent event) {
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(event.player());
        if (player == null) {
            return;
        }
        announce(player, applyUnlocks(NinjaData.get(player), event.skillId(), event.oldLevel(), event.newLevel()));
    }

    /** Hitos que dependen del ninja_xp global (la Sustitucion a 1000). Ver {@link #NINJA_XP_SKILL_ID}. */
    private static void onNinjaXpChanged(NinjaXpChangedEvent event) {
        MinecraftServer server = GameInstance.getServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(event.player());
        if (player == null) {
            return;
        }
        // El hito paga ninja_xp, lo que vuelve a emitir este evento; termina porque otorgar es idempotente.
        announce(player, applyUnlocks(NinjaData.get(player), NINJA_XP_SKILL_ID, event.oldXp(), event.newXp()));
    }

    /**
     * Puesta al dia al entrar: recupera los hitos que el modelo reactivo no pudo ver. Llamar desde
     * {@code DojutsuLifecycle.onJoin} (F10/DJT-221).
     */
    public static void onJoin(ServerPlayer player) {
        announce(player, applyReachedUnlocks(NinjaData.get(player)));
    }

    /**
     * Avisa, paga la ninja_xp y <b>entrega la tecnica</b> de cada hito otorgado.
     *
     * <p>La entrega cierra el {@code TODO F4} que llevaba abierto desde F2 y que era la razon de que
     * <b>26 de los 128 jutsus no se pudieran conseguir en partida</b>: el hito registraba su id y ahi se
     * quedaba. Ahora hace lo que hace {@code main/internal/unlocks}: sumar la XP <b>y</b> llamar al
     * {@code give/jutsu/<id>} correspondiente.
     */
    private static void announce(ServerPlayer player, List<String> granted) {
        for (String unlockId : granted) {
            player.sendSystemMessage(Component.literal("Desbloqueado: " + unlockId + "!"));
            int ninjaXp = ninjaXpOf(unlockId);
            if (ninjaXp > 0) {
                XpApi.addNinjaXp(player, ninjaXp); // unica puerta de escritura de la XP (regla 10)
            }
            int jutsus = JutsuGrantService.grantAll(player, jutsusOf(unlockId));
            LOGGER.info("[{}] {} desbloqueo {} (+{} ninja_xp, {} tecnica(s)).",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), unlockId, ninjaXp, jutsus);
        }
    }

    /** Tecnicas que entrega un hito; vacia si no entrega ninguna (capacidad o item). */
    private static List<String> jutsusOf(String unlockId) {
        for (UnlockDefinition def : UnlockRegistry.all()) {
            if (def.unlockId().equals(unlockId)) {
                return def.grantsJutsus();
            }
        }
        return List.of();
    }
}
