package com.zamu.dojutsu.story;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.StoryState;
import com.zamu.dojutsu.dojutsu.AwakeningService;
import com.zamu.dojutsu.domain.nature.NatureType;
import com.zamu.dojutsu.domain.story.SageEncounter;
import com.zamu.dojutsu.domain.story.SagePath;
import com.zamu.dojutsu.domain.story.StoryTable;
import com.zamu.dojutsu.natures.NatureService;
import com.zamu.dojutsu.network.NinjaSync;
import com.zamu.dojutsu.progression.XpApi;
import com.zamu.dojutsu.sixpaths.SixPathsService;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;

/**
 * Concesion de los caminos del Sabio {@code [S19]} (F9/DJT-200): lo que de verdad cambia al elegir.
 *
 * <h2>Auditoría de S19-R4, ejecutada</h2>
 * El riesgo pedia "auditar que desbloquea exactamente cada camino". Esto es:
 * <ul>
 *   <li><b>Yin</b> ({@code yin_path}) — chakra de los Seis Caminos, <b>despierta el ojo del sabio</b> (Rinnegan
 *       con Hagoromo, Tenseigan con Hamura) y {@value StoryTable#PATH_NINJA_XP} ninja_xp.</li>
 *   <li><b>Yang</b> ({@code yang_path}) — chakra de los Seis Caminos, naturaleza <b>Yin-Yang</b>, el <b>Modo Sabio
 *       de los Seis Caminos</b> y {@value StoryTable#PATH_NINJA_XP} ninja_xp.</li>
 *   <li><b>Ninguno</b> ({@code no_path}) — nada, y <b>vuelve a la etapa cero</b>: rechazarlo no cierra la puerta.</li>
 * </ul>
 *
 * <h2>El Modo Sabio de los Seis Caminos necesita ser sabio primero</h2>
 * {@code yang_path} solo pone {@code d.is_sage 2} <b>si ya eres sabio</b>; si no, avisa ("Must become a Sage
 * first") y el camino queda concedido a la espera. Ese matiz es el que conecta este evento con el Modo Sabio de
 * F6, que dejo {@code SageType} preparado para el tipo de Seis Caminos y su obtencion "para F9".
 *
 * <p><b>El chakra en si lo concede {@code SixPathsService}</b> (F9/DJT-210), que es el unico sitio que lo hace:
 * los <b>dos</b> caminos lo llaman, igual que {@code yin_path} y {@code yang_path} llaman los dos a
 * {@code obtain_six_paths_chakra}. Esta clase decide <b>que camino</b> se toma, no que da el chakra.
 */
public final class SagePathService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SagePathService() {
    }

    /**
     * Concede un camino.
     *
     * @param otherHalf {@code true} si es la <b>segunda</b> mitad ({@code gain_other_half}): cambia el mensaje
     * @return {@code true} si se concedio algo
     */
    public static boolean grant(ServerPlayer player, SagePath path, boolean otherHalf) {
        PlayerNinjaData data = NinjaData.get(player);
        StoryState state = data.story();
        Optional<SageEncounter> sage = state.sage().flatMap(SageEncounter::byId);
        if (sage.isEmpty()) {
            return false;
        }
        switch (path) {
            case NONE -> {
                data.setStory(state.withRefusal());
                say(player, sage.get(), "story.dojutsu.refused");
                return true;
            }
            case YIN -> {
                if (state.yin()) {
                    return false; // ya tenia esa mitad: ni se repite el ojo ni la XP
                }
                data.setStory(state.withYin());
                awakenSageEye(player, sage.get());
            }
            case YANG -> {
                if (state.yang()) {
                    return false;
                }
                data.setStory(state.withYang());
                NatureService.grant(player, NatureType.YIN_YANG);
                SixPathsService.unlockSageMode(player); // yang_path: d.is_sage 2, solo si ya es sabio
            }
        }
        // Los DOS caminos conceden el chakra de los Seis Caminos (yin_path y yang_path llaman igual a
        // obtain_six_paths_chakra). El servicio es idempotente, asi que recibir la otra mitad no lo cobra dos
        // veces. Esto es la deuda que DJT-200 dejo anotada para este ticket.
        SixPathsService.grant(player, "sage_" + sage.get().id());
        XpApi.addNinjaXp(player, StoryTable.PATH_NINJA_XP);
        say(player, sage.get(), otherHalf ? "story.dojutsu.other_half" : "story.dojutsu.chose_" + path.id());
        celebrate(player);
        NinjaSync.sendDelta(player);
        LOGGER.info("[{}] {} tomo el camino {} de {}{}.", DojutsuMod.MOD_NAME,
                player.getGameProfile().getName(), path.id(), sage.get().id(), otherHalf ? " (segunda mitad)" : "");
        return true;
    }

    /**
     * El camino Yin despierta el ojo del sabio: Rinnegan con Hagoromo, Tenseigan con Hamura
     * ({@code rinnegan/evolve/awaken_1} y {@code tenseigan/awaken}).
     */
    private static void awakenSageEye(ServerPlayer player, SageEncounter sage) {
        if (!AwakeningService.grantEye(player, sage.yinEye())) {
            return; // ya lo tenia (o el ojo aun no esta implementado): el resto del camino se concede igual
        }
        player.sendSystemMessage(Component.translatable("story.dojutsu.eye_awakened",
                Component.translatable("eye.dojutsu." + sage.yinEye()).withStyle(ChatFormatting.LIGHT_PURPLE))
                .withStyle(ChatFormatting.GOLD));
    }

    /** Linea del sabio en el chat, con su color ({@code <Hagoromo> ...}). */
    static void say(ServerPlayer player, SageEncounter sage, String key) {
        player.sendSystemMessage(Component.empty()
                .append(Component.literal("<"))
                .append(Component.translatable("story.dojutsu.sage." + sage.id())
                        .withStyle(style -> style.withColor(sage.nameColor())))
                .append(Component.literal("> "))
                .append(Component.translatable(key)));
    }

    /** Efecto de la concesion. */
    private static void celebrate(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 1f, 1.4f);
        }
    }
}
