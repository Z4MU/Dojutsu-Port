package com.zamu.dojutsu.story;

import com.mojang.logging.LogUtils;
import com.zamu.dojutsu.DojutsuMod;
import com.zamu.dojutsu.data.NinjaData;
import com.zamu.dojutsu.data.PlayerNinjaData;
import com.zamu.dojutsu.data.state.StoryState;
import com.zamu.dojutsu.domain.story.SageEncounter;
import com.zamu.dojutsu.domain.story.SagePath;
import com.zamu.dojutsu.domain.story.StoryTable;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Encuentros con los Sabios {@code [S19]} (F9/DJT-200): la cola global, la tirada y el reparto de oportunidades.
 *
 * <h2>Una cola, no una por jugador</h2>
 * La ficha pedia "colas <b>por jugador</b>". El datapack tiene <b>una</b>: {@code queue} programa un solo
 * {@code schedule} a {@value StoryTable#QUEUE_SECONDS} s y, al dispararse, {@code queue_try} corre {@code as @a}
 * tirando los dados <b>para cada jugador conectado</b>. Portado igual, sobre el {@code ModScheduler} de F1: un
 * temporizador que reparte oportunidades, no N temporizadores compitiendo.
 *
 * <h2>Tu linaje decide quien te habla</h2>
 * Hagoromo se aparece a Uzumaki, Uchiha y Senju; Hamura solo a los Hyuga. Los conjuntos son <b>disjuntos</b>, asi
 * que no hay eleccion: {@link SageEncounter#forClan} resuelve el unico posible, o ninguno.
 *
 * <h2>La probabilidad sube si no te ha pasado</h2>
 * 5% el primer intento, 8% el segundo, 10% desde el tercero — y ahi se queda. Un sistema de piedad que el
 * datapack implementa con tres tablas de botin distintas y aqui es {@link StoryTable#chanceFor}.
 *
 * <p><b>Requisito:</b> rango <b>Kage</b> ({@value StoryTable#REQUIRED_RANK}), el mas alto de todo el datapack.
 */
public final class SageEncounterService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private SageEncounterService() {
    }

    /**
     * Programa la cola global. Llamar una vez en el arranque del servidor; se reprograma sola tras cada disparo,
     * como el {@code schedule} recursivo del datapack.
     */
    public static void scheduleQueue(MinecraftServer server) {
        com.zamu.dojutsu.core.ServerTickDispatcher.get()
                .scheduler().schedule(StoryTable.QUEUE_TICKS, () -> fireQueue(server));
    }

    /** Un disparo de la cola: tira para todos los conectados y se reprograma. */
    private static void fireQueue(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tryEncounter(player);
        }
        scheduleQueue(server); // el datapack reprograma en cada queue; aqui igual
    }

    /**
     * Tira el encuentro para un jugador: comprueba linaje, rango y etapa, y con suerte el sabio se le aparece.
     *
     * @return el sabio que se le aparecio, o vacio si no procedia o el azar no salio
     */
    public static Optional<SageEncounter> tryEncounter(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        StoryState state = data.story();
        if (state.isDone()) {
            return Optional.empty(); // ya agoto sus dos apariciones (hagoromo_2)
        }
        Optional<SageEncounter> sage = SageEncounter.forClan(data.clan().clan().orElse(null));
        if (sage.isEmpty()) {
            return Optional.empty(); // su linaje no desciende de ninguno de los dos
        }
        if (!StoryTable.rankAllows(data.progression().rank().ordinal())) {
            return Optional.empty(); // req: hace falta ser Kage
        }
        int chance = StoryTable.chanceFor(state.failedAttempts());
        int roll = player.getRandom().nextInt(100) + 1;
        if (!StoryTable.isHit(roll, chance)) {
            data.setStory(state.withFailedAttempt()); // fallar sube la probabilidad de la proxima
            return Optional.empty();
        }
        begin(player, data, sage.get());
        return sage;
    }

    /** Fuerza el encuentro, saltandose el azar (comando de QA y el disparador narrativo de F9). */
    public static Optional<SageEncounter> forceEncounter(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        Optional<SageEncounter> sage = SageEncounter.forClan(data.clan().clan().orElse(null));
        sage.ifPresent(found -> begin(player, data, found));
        return sage;
    }

    /**
     * El sabio se aparece ({@code start}). Los dos encuentros <b>no</b> hacen lo mismo:
     * <ul>
     *   <li><b>El primero</b> ({@code dialogue/first}) pregunta: abre la pantalla con los tres botones.</li>
     *   <li><b>El segundo</b> ({@code dialogue/second}) <b>no pregunta</b>: se despide y te concede la mitad que
     *       te falte. Por eso no abre pantalla ninguna — seria una eleccion que no existe.</li>
     * </ul>
     */
    private static void begin(ServerPlayer player, PlayerNinjaData data, SageEncounter sage) {
        boolean firstTime = data.story().stage() < StoryTable.STAGE_MET;
        data.setStory(data.story().withEncounter(sage.id()));
        if (firstTime) {
            DialogueController.open(player, sage);
        } else {
            DialogueController.farewell(player, sage);
            grantOtherHalf(player);
        }
        LOGGER.info("[{}] {} se ha encontrado con {}{}.", DojutsuMod.MOD_NAME,
                player.getGameProfile().getName(), sage.id(), firstTime ? " (primera vez)" : " (segunda vez)");
    }

    /** El sabio que le corresponde a este jugador, o vacio (comando de QA). */
    public static Optional<SageEncounter> sageOf(ServerPlayer player) {
        return SageEncounter.forClan(NinjaData.get(player).clan().clan().orElse(null));
    }

    /**
     * Concede la <b>otra mitad</b> del chakra del sabio ({@code gain_other_half}): la eleccion Yin/Yang no es
     * definitiva, mas adelante se puede recibir la que falte.
     *
     * @return la mitad concedida, o vacio si ya tenia las dos (o ninguna)
     */
    public static Optional<SagePath> grantOtherHalf(ServerPlayer player) {
        PlayerNinjaData data = NinjaData.get(player);
        StoryState state = data.story();
        if (state.hasBothHalves() || state.sage().isEmpty()) {
            return Optional.empty();
        }
        Optional<SagePath> missing = state.yin() ? Optional.of(SagePath.YANG)
                : state.yang() ? Optional.of(SagePath.YIN) : Optional.empty();
        missing.ifPresent(path -> SagePathService.grant(player, path, true));
        return missing;
    }
}
