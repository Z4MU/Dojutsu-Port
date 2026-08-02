package com.zamu.dojutsu.clans;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;

/**
 * Team y nametag por clan ({@code create_teams} de {@code main/load} + {@code clans/join_team},
 * {@code [S00 §3]}/{@code [S04]}): el nombre del jugador muestra el color y el sufijo de su clan
 * ({@code "Zamu Uchiha"} en rojo oscuro).
 *
 * <p><b>Un unico punto de entrada</b> ({@link #sync}): reconcilia el team del jugador con su clan actual. Lo
 * llaman el join, el reset y el login (cubre saves anteriores a este ticket y cualquier deriva). Los teams se
 * crean <b>perezosamente</b> y con nombre <b>namespaceado</b> ({@code dojutsu_<clan>}) — el datapack usaba
 * nombres pelados ({@code Uchiha}) que podian pisar teams del servidor, el riesgo que senala la ficha.
 *
 * <p><b>No pisa teams ajenos:</b> si el jugador ya esta en un team que no es {@code dojutsu_*} (lo puso un
 * admin), se respeta y no se toca (ni al unirse ni al resetear); queda un aviso en el log. Entre teams
 * {@code dojutsu_*} si se mueve (cambio de clan tras reset).
 *
 * <p>Propiedades fieles a {@code create_teams}: color del clan (dato {@code teamColor} de la
 * {@link ClanDefinition}), sufijo {@code " <Clan>"} (derivado del id capitalizado; los 14 coinciden) y
 * {@code seeFriendlyInvisibles false}. El team {@code MaskHidden} (ocultar nametag con mascaras) es de los
 * items de F8 — no se porta aqui.
 */
public final class ClanTeamService {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Prefijo de los teams del mod: evita colisiones con teams creados por el servidor. */
    public static final String TEAM_PREFIX = "dojutsu_";

    private ClanTeamService() {
    }

    /** Reconcilia el team del jugador con su clan actual (join, reset y login usan esto mismo). */
    public static void sync(ServerPlayer player) {
        Optional<ClanDefinition> clan = NinjaData.get(player).clan().clan().flatMap(ClanRegistry::get);
        boolean touched = update(player.getScoreboard(), player.getScoreboardName(), clan);
        if (!touched && clan.isPresent()) {
            // Solo avisa si de verdad habia un team de clan que aplicar (evita ruido en cada login de
            // jugadores sin clan que esten en teams del servidor).
            LOGGER.warn("[{}] {} esta en un team del servidor: no se aplica el team del clan {}.",
                    DojutsuMod.MOD_NAME, player.getGameProfile().getName(), clan.get().id());
        }
    }

    /**
     * Reconcilia el team de una entrada del scoreboard con el clan dado. Separado de {@link #sync} para ser
     * testeable con un {@link Scoreboard} plano, sin jugador.
     *
     * @return {@code false} solo si no se actuo por respeto a un team ajeno (no {@code dojutsu_*})
     */
    static boolean update(Scoreboard scoreboard, String entryName, Optional<ClanDefinition> clan) {
        PlayerTeam current = scoreboard.getPlayersTeam(entryName);
        if (current != null && !current.getName().startsWith(TEAM_PREFIX)) {
            return false; // team del servidor: se respeta (borde de la ficha)
        }
        if (clan.isEmpty()) {
            if (current != null) {
                scoreboard.removePlayerFromTeam(entryName, current); // reset: el sufijo se va
            }
            return true;
        }
        PlayerTeam team = ensureTeam(scoreboard, clan.get());
        if (current != team) {
            scoreboard.addPlayerToTeam(entryName, team); // mueve tambien entre teams dojutsu_*
        }
        return true;
    }

    /** Crea (perezoso) o refresca el team del clan con las propiedades de {@code create_teams}. */
    static PlayerTeam ensureTeam(Scoreboard scoreboard, ClanDefinition clan) {
        String name = TEAM_PREFIX + clan.id();
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);
        }
        clan.teamColor().ifPresent(team::setColor);
        team.setPlayerSuffix(Component.literal(" " + capitalize(clan.id())));
        team.setSeeFriendlyInvisibles(false);
        return team;
    }

    /** {@code "uchiha"} -> {@code "Uchiha"} (el sufijo de {@code create_teams}; los 14 clanes coinciden). */
    static String capitalize(String id) {
        return id.isEmpty() ? id
                : id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }
}
