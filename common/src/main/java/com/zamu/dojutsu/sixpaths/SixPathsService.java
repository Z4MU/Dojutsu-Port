package com.zamu.dojutsu.sixpaths;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.chakra.ChakraService;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.Skill;
import com.zamu.dojutsu.data.state.SixPathsState;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.sixpaths.SixPathsTable;
import com.zamu.dojutsu.natures.NatureService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.SkillPointService;
import com.zamu.dojutsu.progression.XpApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

/**
 * El <b>unico</b> punto que concede o retira el chakra de los Seis Caminos {@code [S08/S19]} (F9/DJT-210).
 *
 * <h2>Las vias lo activan; no lo reimplementan</h2>
 * Ese es el objetivo del ticket, y resulta que es tambien como lo resuelve el datapack: hay <b>una</b>
 * {@code obtain_six_paths_chakra} y las vias la <b>llaman</b>. Aqui pasa igual —
 * {@link #grant} es el equivalente y lo invocan:
 * <ul>
 *   <li>los <b>eventos narrativos</b> (S19/DJT-200), en ambos caminos del Sabio;</li>
 *   <li>el <b>sellado del Diez Colas</b> (S09/DJT-165), pero {@link #lend prestado};</li>
 *   <li>el comando de QA, que hace de {@code cheats/give_six_paths}.</li>
 * </ul>
 * Si mañana se añade el item {@code sp_chakra} del datapack, tambien llamara aqui. Ninguna de ellas vuelve a
 * escribir los +{@value SixPathsTable#CHAKRA_BONUS} de chakra ni las siete naturalezas.
 *
 * <h2>Prestado y permanente no conceden lo mismo</h2>
 * {@link #grant} replica {@code obtain_six_paths_chakra} entera; {@link #lend} replica {@code tmp_sp_chakra}, que
 * es <b>solo</b> chakra y XP. La diferencia es del datapack, no una simplificacion: al jinchuriki del Diez Colas
 * las naturalezas se las presta {@code SealingService} por su cuenta, con la bestia como fuente.
 *
 * <p><b>La via del Rinnegan no existe</b> ({@link SixPathsTable#RINNEGAN_ROUTE_DISABLED}): sus lineas estan
 * comentadas en {@code still_check}. La ficha la contaba como una de las tres.
 */
public final class SixPathsService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SixPathsService() {
    }

    /** {@code true} si tiene el chakra de los Seis Caminos, sea suyo o prestado ({@code has_sp_chakra}). */
    public static boolean has(PlayerNinjaData data) {
        return data.sixPaths().chakra();
    }

    /**
     * Concede el chakra <b>en propiedad</b> ({@code obtain_six_paths_chakra}): chakra, XP, las siete
     * naturalezas con su XP, +{@value SixPathsTable#AFFINITY_BONUS} a cada afinidad y el senjutsu.
     *
     * <p>Es idempotente por el mismo motivo que en el datapack, donde todas las llamadas van precedidas de
     * {@code unless predicate has_sp_chakra}: aqui el guard esta <b>dentro</b>, para que ninguna via pueda
     * olvidarlo. Si lo tenia <b>prestado</b>, se consolida sin volver a pagar el chakra.
     *
     * @return {@code true} si se concedio algo
     */
    public static boolean grant(ServerPlayer player, String source) {
        PlayerNinjaData data = NinjaData.get(player);
        SixPathsState state = data.sixPaths();
        if (state.chakra() && !state.isLoaned()) {
            return false; // ya era suyo: unless has_sp_chakra
        }
        boolean consolidating = state.isLoaned(); // lo tenia prestado: se queda con el, sin cobrar dos veces
        data.setSixPaths(state.granted());
        if (!consolidating) {
            payChakraAndXp(player, data);
        }
        grantAllNatures(player, data);
        SkillPointService.spend(data, Skill.SENJUTSU, SixPathsTable.SENJUTSU_LEVELS);
        announce(player);
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} obtuvo el chakra de los Seis Caminos ({}{}).", DojutsuMod.MOD_NAME,
                player.getGameProfile().getName(), source, consolidating ? ", consolidando el prestamo" : "");
        return true;
    }

    /**
     * <b>Presta</b> el chakra ({@code tmp_sp_chakra}): solo chakra y XP, sin naturalezas ni afinidades. Se retira
     * con {@link #revokeFrom} cuando la fuente deja de sostenerlo.
     *
     * @param source quien lo presta (la bestia); debe coincidir con el de {@link #revokeFrom}
     * @return {@code true} si se presto
     */
    public static boolean lend(ServerPlayer player, String source) {
        PlayerNinjaData data = NinjaData.get(player);
        if (data.sixPaths().chakra()) {
            return false; // ya lo tiene (suyo o de otro prestamo): unless has_sp_chakra
        }
        data.setSixPaths(data.sixPaths().loanedBy(source));
        payChakraAndXp(player, data);
        announceLoan(player);
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} recibio el chakra de los Seis Caminos prestado por {}.", DojutsuMod.MOD_NAME,
                player.getGameProfile().getName(), source);
        return true;
    }

    /**
     * Retira lo prestado por esa fuente y <b>solo</b> eso ({@code take_sp_chakra}, desde {@code remove_tmp}): si
     * el chakra es suyo o lo presta otro, no se toca. Devuelve el chakra y la XP exactamente.
     *
     * @return {@code true} si se retiro
     */
    public static boolean revokeFrom(ServerPlayer player, String source) {
        PlayerNinjaData data = NinjaData.get(player);
        if (!data.sixPaths().isLoanedBy(source)) {
            return false; // no era suyo que prestar
        }
        data.setSixPaths(data.sixPaths().revoked());
        ChakraService.setMax(data, data.chakra().maxChakra() - SixPathsTable.CHAKRA_BONUS, false);
        XpApi.removeNinjaXp(player, SixPathsTable.NINJA_XP);
        player.sendSystemMessage(Component.translatable("sixpaths.dojutsu.lost")
                .withStyle(ChatFormatting.AQUA));
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} perdio el chakra de los Seis Caminos que le prestaba {}.", DojutsuMod.MOD_NAME,
                player.getGameProfile().getName(), source);
        return true;
    }

    /**
     * Desbloquea el <b>Modo Sabio de los Seis Caminos</b> ({@code d.is_sage 2}), que el datapack solo concede
     * <b>si ya eres sabio</b> — si no, avisa y queda a la espera. Ese matiz es del datapack, no una decision
     * nuestra: lo hace igual {@code yang_path} y el cheat.
     *
     * @return {@code true} si quedo desbloqueado; {@code false} si aun no es sabio
     */
    public static boolean unlockSageMode(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        if (!data.sage().sage()) {
            player.sendSystemMessage(Component.translatable("sixpaths.dojutsu.sage_mode_pending")
                    .withStyle(ChatFormatting.GOLD));
            return false;
        }
        if (data.sixPaths().sageMode()) {
            return false;
        }
        data.setSixPaths(data.sixPaths().withSageMode());
        player.sendSystemMessage(Component.translatable("sixpaths.dojutsu.sage_mode")
                .withStyle(ChatFormatting.GOLD));
        NinjaSync.sendDelta(player);
        return true;
    }

    /** Chakra maximo, chakra actual y ninja_xp: el nucleo comun de las dos concesiones. */
    private static void payChakraAndXp(ServerPlayer player, PlayerNinjaData data) {
        ChakraService.setMax(data, data.chakra().maxChakra() + SixPathsTable.CHAKRA_BONUS, false);
        ChakraService.restore(data, SixPathsTable.CHAKRA_BONUS);
        XpApi.addNinjaXp(player, SixPathsTable.NINJA_XP);
    }

    /**
     * Las siete naturalezas con su XP, y +{@value SixPathsTable#AFFINITY_BONUS} de afinidad a <b>todas</b>.
     *
     * <p>La XP solo se paga por las que <b>no</b> tuviera ({@code unless has_*}), pero la afinidad sube en todas
     * sin condicion — el datapack hace los {@code aff_* += 100} fuera de los guards.
     */
    private static void grantAllNatures(ServerPlayer player, PlayerNinjaData data) {
        for (NatureType nature : SixPathsTable.GRANTED_NATURES) {
            if (!NatureService.has(data, nature)) {
                NatureService.grant(player, nature);
                XpApi.addNinjaXp(player, SixPathsTable.natureXp(nature));
            }
            NatureService.addAffinityLevels(data, nature, SixPathsTable.AFFINITY_BONUS);
        }
    }

    private static void announce(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("sixpaths.dojutsu.obtained")
                .withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.translatable("sixpaths.dojutsu.all_affinities")
                .withStyle(ChatFormatting.BLUE));
        celebrate(player);
    }

    private static void announceLoan(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("sixpaths.dojutsu.obtained")
                .withStyle(ChatFormatting.AQUA));
        celebrate(player);
    }

    private static void celebrate(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundSource.PLAYERS, 0.3f, 2f);
        }
    }
}
