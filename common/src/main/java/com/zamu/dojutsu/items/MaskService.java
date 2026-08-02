package com.zamu.dojutsu.items;

import com.zamu.dojutsu.clans.ClanTeamService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.ItemFlags;
import com.zamu.dojutsu.domain.items.MaskType;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Mascaras {@code [S14]} (F8/DJT-170): ocultan tu nombre, y nada mas.
 *
 * <h2>Estado, no casco</h2>
 * El datapack la mete en el slot de la cabeza y por eso te <b>veta llevar casco</b> ({@code masks/check_eye}
 * aborta si el slot esta ocupado). Aqui la mascara es <b>estado</b> — la misma decision que ya se tomo con los
 * <b>ojos</b> en F5, y por el mismo motivo: la identidad del ninja no deberia vivir en un slot de armadura.
 * <b>Consecuencia asumida y buscada:</b> puedes llevar mascara y casco a la vez.
 *
 * <h2>El nametag y el equipo del clan compiten por el mismo sitio</h2>
 * Ocultar el nombre en Minecraft se hace con un <b>equipo</b> ({@code nametagVisibility never}), y un jugador
 * solo puede estar en uno. El datapack ya vivia con eso: al ponerte la mascara hace {@code team join MaskHidden}
 * y al quitartela llama a {@code clans/join_team} para devolverte el de tu clan. Aqui es literal —
 * {@link ClanTeamService#sync} ya existia y hace exactamente eso—, asi que el <b>sufijo del clan desaparece
 * mientras llevas mascara</b>. Es lo que tiene esconder quien eres.
 *
 * <p>{@code ClanTeamService} dejo anotado desde F3 que el equipo {@code MaskHidden} era "de los items de F8";
 * aqui llega.
 */
public final class MaskService {

    /** Nombre del equipo que oculta el nametag ({@code team join MaskHidden}). */
    public static final String HIDDEN_TEAM = ClanTeamService.TEAM_PREFIX + "mask_hidden";

    private MaskService() {
    }

    /**
     * Se pone una mascara (reemplazando la que llevase).
     *
     * @return {@code true} siempre que la mascara exista
     */
    public static boolean equip(ServerPlayer player, MaskType mask) {
        PlayerNinjaData data = NinjaData.get(player);
        data.setItems(data.items().withMask(mask.id()));
        hideName(player);
        player.sendSystemMessage(Component.translatable("mask.dojutsu.equipped",
                Component.translatable("mask.dojutsu.type." + mask.id())).withStyle(ChatFormatting.GRAY));
        player.playNotifySound(SoundEvents.ARMOR_EQUIP_TURTLE.value(), SoundSource.PLAYERS, 1f, 1f);
        return true;
    }

    /**
     * Se quita la mascara y recupera el equipo de su clan ({@code masks/unequip} + {@code clans/join_team}).
     *
     * @return la mascara que llevaba, o vacio si no llevaba ninguna
     */
    public static Optional<MaskType> unequip(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        Optional<MaskType> worn = data.items().mask().flatMap(MaskType::byId);
        if (data.items().hasMask()) {
            data.setItems(data.items().withoutMask());
        }
        ClanTeamService.sync(player); // devuelve el team del clan (o ninguno), como el datapack
        worn.ifPresent(mask -> player.sendSystemMessage(
                Component.translatable("mask.dojutsu.removed").withStyle(ChatFormatting.GRAY)));
        return worn;
    }

    /** Mascara que lleva puesta, o vacio. */
    public static Optional<MaskType> worn(ServerPlayer player) {
        return NinjaData.get(player).items().mask().flatMap(MaskType::byId);
    }

    /**
     * Reaplica el ocultado al entrar al mundo: el equipo es estado del scoreboard y el jugador podria haber
     * cambiado de partida. Idempotente; la llama el ciclo de vida en el join.
     */
    public static void refresh(ServerPlayer player) {
        if (NinjaData.get(player).items().hasMask()) {
            hideName(player);
        }
    }

    /** Mete al jugador en el equipo que oculta el nametag, creandolo si hace falta. */
    private static void hideName(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(HIDDEN_TEAM);
        if (team == null) {
            team = scoreboard.addPlayerTeam(HIDDEN_TEAM);
        }
        team.setNameTagVisibility(Team.Visibility.NEVER);
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
    }
}
